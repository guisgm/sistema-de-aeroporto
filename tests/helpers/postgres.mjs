import { randomUUID } from 'node:crypto';
import { openDatabase } from '../../server/database.mjs';

export async function testDatabase() {
  const schema = `test_${randomUUID().replaceAll('-', '')}`;
  const db = await openDatabase({
    connectionString:
      process.env.AEROHUB_TEST_DATABASE_URL ||
      'postgresql://aeroporto_teste@127.0.0.1:55439/sistema_aeroporto',
    schema,
    seed: true,
  });
  const close = db.close;
  db.close = async () => {
    try {
      await db.execute(`DROP SCHEMA ${schema} CASCADE`);
    } finally {
      await close();
    }
  };
  return db;
}
