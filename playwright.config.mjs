import { defineConfig } from '@playwright/test';
export default defineConfig({
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
    env: { PORT: '5199', DB_PATH: `artifacts/e2e-${process.pid}.sqlite` },
    timeout: 30000,
  },
});
