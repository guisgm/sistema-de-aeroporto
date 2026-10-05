import { test, expect } from '@playwright/test';

async function login(page, role = 'Administrador') {
  await page.goto('/');
  await page.getByRole('button', { name: role, exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Visão geral da operação' })).toBeVisible();
  await expect(page.locator('.flight-board tbody tr').first()).toBeVisible();
}
test('desktop dashboard, all navigation views, global search and exports work', async ({
  page,
}) => {
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.setViewportSize({ width: 1440, height: 1000 });
  await login(page);
  await page.screenshot({ path: 'artifacts/dashboard-desktop.png', fullPage: true });
  expect(
    await page
      .locator('.airport-image img')
      .evaluate((img) => img.complete && img.naturalWidth > 0),
  ).toBeTruthy();
  for (const [label, title] of [
    ['Voos', 'Gestão de voos'],
    ['Passageiros', 'Passageiros e reservas'],
    ['Companhias aéreas', 'Companhias aéreas'],
    ['Aeronaves', 'Gestão de aeronaves'],
    ['Terminais e portões', 'Terminais e portões'],
    ['Relatórios', 'Relatórios'],
    ['Histórico', 'Histórico de alterações'],
    ['Alertas operacionais', 'Alertas operacionais'],
  ]) {
    await page
      .locator('.sidebar nav')
      .getByRole('button', { name: new RegExp(`^${label}(?: \\d+)?$`) })
      .click();
    await expect(page.locator('h1')).toHaveText(title);
  }
  await page.getByRole('textbox', { name: 'Busca global' }).fill('LA 1200');
  await page.locator('.search-results button').first().click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.getByRole('button', { name: 'Fechar', exact: true }).click();
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Relatórios', exact: true })
    .click();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar CSV' }).first().click();
  expect((await download).suggestedFilename()).toContain('flights');
  expect(errors).toEqual([]);
});
test('complete passenger registration, reservation, check-in and boarding card', async ({
  page,
}) => {
  await login(page);
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Passageiros', exact: true })
    .click();
  await page.getByRole('button', { name: 'Novo passageiro' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Nome completo').fill('Passageiro Teste Completo');
  await dialog.getByLabel('Tipo de documento').selectOption('passport');
  await dialog.getByLabel('Documento', { exact: true }).fill('BR999991');
  await dialog.getByLabel('Data de nascimento').fill('1995-04-10');
  await dialog.getByLabel('E-mail', { exact: true }).fill('teste-completo@example.com');
  await dialog.getByLabel('Telefone com DDD').fill('11988887777');
  await dialog.getByRole('button', { name: 'Salvar registro' }).click();
  await expect(dialog).not.toBeVisible();
  await page.getByPlaceholder('Buscar nome, documento ou e-mail').fill('Passageiro Teste Completo');
  await page.getByRole('button', { name: 'Reservar para Passageiro Teste Completo' }).click();
  await page.getByLabel('Voo de partida').selectOption({ index: 1 });
  await page.getByRole('button', { name: 'Assento 10A', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar reserva' }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await page.getByRole('button', { name: /1 reservas/ }).click();
  await page.getByRole('button', { name: 'Check-in', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Cartão de embarque' })).toBeVisible();
  await expect(page.locator('.boarding-pass')).toContainText('Passageiro Teste Completo');
  await expect(page.locator('.pass-tear')).toContainText('10A');
  await page.screenshot({ path: 'artifacts/boarding-pass.png', fullPage: true });
});
test('flight creation, editing, critical confirmation and history', async ({ page }) => {
  await login(page);
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Terminais e portões', exact: true })
    .click();
  await page.getByRole('button', { name: 'Novo portão' }).click();
  await page.getByLabel('Código do portão').fill('A88');
  await page.getByRole('button', { name: 'Salvar registro' }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Aeronaves', exact: true })
    .click();
  await page.getByRole('button', { name: 'Nova aeronave' }).click();
  await page.getByLabel('Matrícula', { exact: true }).fill('PR-E2E');
  await page.getByLabel('Modelo', { exact: true }).fill('Airbus A320neo');
  await page.getByRole('button', { name: 'Salvar registro' }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: /^Voos(?: \d+)?$/ })
    .click();
  await page.getByRole('button', { name: 'Novo voo' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Número do voo').fill('LA 9988');
  await dialog
    .getByLabel('Aeronave', { exact: true })
    .selectOption({ label: 'PR-E2E · Airbus A320neo' });
  await dialog.getByLabel('Cidade de destino').fill('Recife');
  await dialog.getByLabel('IATA do destino').fill('REC');
  const day = new Date(Date.now() + 86400000).toLocaleDateString('en-CA', {
    timeZone: 'America/Sao_Paulo',
  });
  await dialog.getByLabel('Horário previsto (Brasília)').fill(`${day}T19:00`);
  await dialog.getByLabel('Portão', { exact: true }).selectOption({ label: 'A88 · Terminal 1' });
  await dialog.getByRole('button', { name: 'Salvar registro' }).click();
  await expect(dialog).not.toBeVisible();
  await page.getByLabel('Data operacional').fill(day);
  await page.getByPlaceholder('Buscar voo ou destino').fill('LA 9988');
  await page.getByRole('button', { name: 'LA 9988', exact: true }).click();
  await page.getByRole('button', { name: 'Editar voo' }).click();
  await page.getByLabel('Status', { exact: true }).selectOption('cancelled');
  await page.getByLabel('Observações / Motivo').fill('Cancelamento de teste operacional');
  await page.getByRole('button', { name: 'Salvar registro' }).click();
  await expect(page.getByText('Confirmar ação operacional')).toBeVisible();
  await page.getByRole('button', { name: 'Confirmar alteração' }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await expect(page.locator('tbody')).toContainText('Cancelado');
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Histórico', exact: true })
    .click();
  await page.getByPlaceholder('Buscar ação, usuário ou registro').fill('LA 9988');
  await expect(page.locator('tbody')).toContainText('Cancelamento de teste operacional');
});
test('attendant permissions and mobile layouts have no page overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, 'Atendente');
  await expect(page.getByRole('button', { name: 'Novo voo', exact: true })).not.toBeVisible();
  await page.screenshot({ path: 'artifacts/dashboard-mobile.png', fullPage: true });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
  ).toBeTruthy();
  await page.getByRole('button', { name: 'Abrir menu' }).click();
  await page
    .locator('.sidebar nav')
    .getByRole('button', { name: 'Passageiros', exact: true })
    .click();
  await expect(page.locator('h1')).toHaveText('Passageiros e reservas');
  await page.getByRole('button', { name: 'Novo passageiro' }).click();
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
  ).toBeTruthy();
  await page.screenshot({ path: 'artifacts/form-mobile.png', fullPage: true });
});
