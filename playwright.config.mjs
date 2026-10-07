import { defineConfig } from '@playwright/test';
import { randomUUID } from 'node:crypto';
const testSchema = `e2e_${randomUUID().replaceAll('-', '')}`;
export default defineConfig({
  metadata: { testSchema },
  globalTeardown: './tests/e2e/cleanup.mjs',
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  timeout: 40000,
  use: {
    baseURL: 'http://127.0.0.1:5199',
    browserName: 'chromium',
    channel: 'chrome',
    headless: true,
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'node server/index.mjs',
    url: 'http://127.0.0.1:5199',
    reuseExistingServer: false,
    env: {
      PORT: '5199',
      DATABASE_URL:
        process.env.AEROHUB_TEST_DATABASE_URL ||
        'postgresql://aeroporto_teste@127.0.0.1:55439/sistema_aeroporto',
      AEROHUB_DB_SCHEMA: testSchema,
      AEROHUB_DEMO: 'true',
    },
    timeout: 30000,
  },
});
