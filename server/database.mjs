import pg from 'pg';
import { readFile } from 'node:fs/promises';
import { AsyncLocalStorage } from 'node:async_hooks';
import { randomBytes, scryptSync } from 'node:crypto';

export const today = () =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
export const localTime = (day, hour) => new Date(`${day}T${hour}:00-03:00`).toISOString();
export function passwordHash(password, salt = randomBytes(16).toString('hex')) {
  return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`;
}
const camelFields = Object.fromEntries(
  [
    'terminalId',
    'airlineId',
    'maintenanceDate',
    'aircraftId',
    'originCode',
    'destinationCode',
    'gateId',
    'documentType',
    'birthDate',
    'passengerId',
    'flightId',
    'checkedAt',
    'createdAt',
    'userId',
    'entityId',
    'userName',
  ].map((name) => [name.toLowerCase(), name]),
);
function parameters(sql) {
  let index = 0;
  return sql
    .split(/('(?:''|[^'])*')/g)
    .map((part, i) => (i % 2 ? part : part.replace(/\?/g, () => `$${++index}`)))
    .join('');
}
async function localConnection() {
  if (process.env.DATABASE_URL) return { connectionString: process.env.DATABASE_URL };
  if (process.env.PGHOST || process.env.PGUSER || process.env.PGDATABASE) return {};
  let content;
  try {
    content = await readFile(new URL('../config/application.properties', import.meta.url), 'utf8');
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    throw new Error(
      'Configure config/application.properties, DATABASE_URL ou as variaveis PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD.',
    );
  }
  const properties = Object.fromEntries(
    content
      .split(/\r?\n/)
      .filter((line) => line.trim() && !/^[\s]*[#!]/.test(line))
      .map((line) => {
        const i = line.indexOf('=');
        return [line.slice(0, i).trim(), line.slice(i + 1).trim()];
      }),
  );
  if (
    !properties['db.url']?.startsWith('jdbc:postgresql://') ||
    !properties['db.usuario'] ||
    !properties['db.senha']
  )
    throw new Error('Preencha db.url, db.usuario e db.senha em config/application.properties.');
  const url = new URL(properties['db.url'].slice(5));
  url.username = properties['db.usuario'];
  url.password = properties['db.senha'];
  return { connectionString: url.toString() };
}
export async function openDatabase({
  schema = process.env.AEROHUB_DB_SCHEMA || 'aerohub',
  seed = process.env.AEROHUB_DEMO === 'true',
  ...connection
} = {}) {
  if (!/^[a-z][a-z0-9_]{0,62}$/.test(schema) || ['public', 'aeroporto'].includes(schema))
    throw new Error('Use um schema exclusivo para a web (AEROHUB_DB_SCHEMA).');
  const configured = Object.keys(connection).length ? connection : await localConnection();
  const pool = new pg.Pool({
    connectionTimeoutMillis: 5000,
    ...configured,
    options: `-c search_path=${schema} -c timezone=America/Sao_Paulo`,
  });
  pool.on('error', (error) => console.error('Conexao PostgreSQL interrompida:', error.code));
  const context = new AsyncLocalStorage();
  const db = {
    schema,
    pool,
    context,
    async execute(sql, values = []) {
      return (context.getStore() || pool).query(parameters(sql), values);
    },
    async all(sql, values = []) {
      const result = await db.execute(sql, values);
      return result.rows.map((row) =>
        Object.fromEntries(
          Object.entries(row).map(([key, value]) => [
            camelFields[key] || key,
            key === 'count' ? Number(value) : value,
          ]),
        ),
      );
    },
    async one(sql, values = []) {
      return (await db.all(sql, values))[0];
    },
    async close() {
      await pool.end();
    },
  };
  try {
    const ddl = await readFile(new URL('../sql/web.sql', import.meta.url), 'utf8');
    await transaction(db, async () => {
      await db.execute(`CREATE SCHEMA IF NOT EXISTS ${schema}`);
      await db.execute(ddl);
      if (seed && !(await db.one('SELECT id FROM users LIMIT 1'))) {
        const { seed: populate } = await import('./demo.mjs');
        await populate(db);
      }
    });
    return db;
  } catch (error) {
    await pool.end();
    throw error;
  }
}
export async function transaction(db, action) {
  if (db.context.getStore()) return action();
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    // Writers lock before reading availability/version, including initialization.
    await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', [`aerohub:${db.schema}`]);
    const result = await db.context.run(client, action);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch {
      /* preserve original failure */
    }
    throw error;
  } finally {
    client.release();
  }
}
