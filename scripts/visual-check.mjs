import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  const page = await browser.newPage();
  await page.goto(process.env.APP_URL || 'http://127.0.0.1:5173');
  await page.getByRole('button', { name: 'Administrador', exact: true }).click();
  await page.locator('.flight-board tbody tr').first().waitFor();
  await page.evaluate(() => document.fonts.ready);
  mkdirSync('artifacts', { recursive: true });
  for (const width of [1440, 1280, 768, 390, 320]) {
    await page.setViewportSize({ width, height: width > 780 ? 1000 : 844 });
    await page.screenshot({ path: `artifacts/viewport-${width}.png`, fullPage: true });
    const report = await page.evaluate(() => ({
      viewport: innerWidth,
      width: document.documentElement.scrollWidth,
      overflow: [...document.querySelectorAll('body *')]
        .filter((el) => {
          const rect = el.getBoundingClientRect();
          return (
            rect.width > 0 &&
            rect.right > innerWidth + 1 &&
            getComputedStyle(el).position !== 'fixed' &&
            !el.closest('.table-scroll,.sidebar,.seat-map')
          );
        })
        .map((el) => ({
          tag: el.tagName,
          class: el.className,
          right: Math.round(el.getBoundingClientRect().right),
        }))
        .slice(0, 20),
      imageHeight: document.querySelector('.airport-image')?.getBoundingClientRect().height,
    }));
    console.log(JSON.stringify(report));
  }
  for (const width of [390, 320]) {
    await page.setViewportSize({ width, height: 844 });
    for (const label of [
      'Voos',
      'Passageiros',
      'Companhias aéreas',
      'Aeronaves',
      'Terminais e portões',
      'Relatórios',
      'Histórico',
      'Alertas operacionais',
    ]) {
      await page.getByRole('button', { name: 'Abrir menu' }).click();
      await page
        .locator('.sidebar nav')
        .getByRole('button', { name: new RegExp(`^${label}(?: \\d+)?$`) })
        .click();
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
      console.log(JSON.stringify({ view: label, width, overflow }));
      if (overflow) throw new Error(`Layout overflow: ${label} at ${width}px`);
    }
  }
} finally {
  await browser.close();
}
