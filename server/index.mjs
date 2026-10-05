import express from 'express';
import { resolve } from 'node:path';
import { openDatabase } from './database.mjs';
import { createApp } from './app.mjs';

const db = openDatabase(process.env.DB_PATH || 'data/aerohub.sqlite');
const app = createApp(db);
if (process.argv.includes('--dev')) {
  const { createServer } = await import('vite');
  const vite = await createServer({ server: { middlewareMode: true }, appType: 'spa' });
  app.use(vite.middlewares);
} else {
  app.use(express.static(resolve('dist')));
  app.get('/{*path}', (req, res) => res.sendFile(resolve('dist/index.html')));
}
const port = Number(process.env.PORT || 5173);
const server = app.listen(port, process.env.HOST || '127.0.0.1', () =>
  console.log(`AeroHub disponivel em http://localhost:${port}`),
);
server.on('error', (error) => {
  console.error(
    error.code === 'EADDRINUSE' ? `Porta ${port} ocupada. Defina PORT com outra porta.` : error,
  );
  db.close();
  process.exit(1);
});
for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, () =>
    server.close(() => {
      db.close();
      process.exit(0);
    }),
  );
