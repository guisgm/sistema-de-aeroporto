import pg from 'pg';

export default async function cleanup(config) {
  const schema = config.metadata.testSchema;
  if (!/^e2e_[a-f0-9]{32}$/.test(schema)) throw new Error('Schema de teste invalido.');
  const pool = new pg.Pool({
    connectionString:
      process.env.AEROHUB_TEST_DATABASE_URL ||
      'postgresql://aeroporto_teste@127.0.0.1:55439/sistema_aeroporto',
    connectionTimeoutMillis: 5000,
  });
  try {
    await pool.query(`DROP SCHEMA IF EXISTS ${schema} CASCADE`);
  } finally {
    await pool.end();
  }
}
