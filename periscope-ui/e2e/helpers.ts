import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, type Page } from '@playwright/test';

const CURRENT_DIR = path.dirname(fileURLToPath(import.meta.url));
const DPMA_SAMPLE_PATH = path.resolve(
  CURRENT_DIR,
  '../../docs/modernization/dataset-teste/dpma-sample.csv',
);

export async function loginAsAdmin(page: Page) {
  await page.goto('./login');
  await page.getByTestId('login-username').fill('admin');
  await page.getByTestId('login-password').fill('123456');
  await page.getByTestId('login-submit').click();
  await expect(page).toHaveURL(/\/projects\/?$/);
  await expect(page.getByRole('heading', { name: 'Projetos' })).toBeVisible();
}

/**
 * Ensures there is a project populated with patents from dpma-sample.csv
 * so E2E suites run deterministically even on a fresh database.
 */
export async function openProjectWithPatents(page: Page) {
  await loginAsAdmin(page);

  const table = page.getByTestId('projects-table');
  const revisao = page.getByRole('link', { name: /Projeto Revisao/i });

  if ((await table.count()) > 0 && (await revisao.count()) > 0) {
    await revisao.first().click();
  } else if ((await table.count()) > 0 && (await table.locator('tbody a').count()) > 0) {
    await table.locator('tbody a').first().click();
  } else {
    await page.getByTestId('project-create').click();
    await page.getByTestId('project-title').fill('Projeto Revisao E2E');
    await page.getByTestId('project-description').fill('Seed E2E');
    await page.getByTestId('project-save').click();
    await expect(table.getByRole('link', { name: 'Projeto Revisao E2E' })).toBeVisible();
    await table.getByRole('link', { name: 'Projeto Revisao E2E' }).first().click();
  }

  await expect(page).toHaveURL(/\/projects\/[^/]+\/patents/);

  if ((await page.getByTestId('patents-table').count()) === 0) {
    await page.getByRole('link', { name: 'Importar' }).click();
    await expect(page).toHaveURL(/\/projects\/[^/]+\/import/);
    await page.getByTestId('import-type').selectOption('DPMA');
    await page.getByTestId('import-file').setInputFiles(DPMA_SAMPLE_PATH);
    await page.getByTestId('import-submit').click();
    await expect(page.getByTestId('success-alert')).toBeVisible({ timeout: 20_000 });
    await page.getByRole('link', { name: 'Ver patentes' }).click();
    await expect(page).toHaveURL(/\/projects\/[^/]+\/patents/);
  }

  await expect(page.getByTestId('patents-table')).toBeVisible({ timeout: 20_000 });
}
