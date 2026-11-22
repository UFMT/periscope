import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('reports', () => {
  test('abre relatórios, alterna entre múltiplos relatórios e i18n PT/EN', async ({ page }) => {
    await openProjectWithPatents(page);

    await page.getByRole('main').getByRole('link', { name: 'Relatórios' }).click();
    await expect(page).toHaveURL(/\/reports\/?$/);
    await expect(page.getByRole('heading', { name: 'Relatórios' })).toBeVisible();

    await expect(page.getByTestId('report-chart')).toBeVisible();
    await expect(page.getByTestId('report-table')).toBeVisible();
    await expect(page.getByTestId('report-table').locator('tbody tr').first()).toBeVisible();

    const select = page.getByTestId('report-select');
    await select.selectOption('main-inventor');
    await expect(page.getByTestId('report-table').locator('tbody tr').first()).toBeVisible();

    await select.selectOption('main-ipc');
    await page.getByTestId('filter-ipc-level').selectOption('section');
    await expect(page.getByTestId('report-table')).toBeVisible();

    await select.selectOption('application-date');
    await expect(page.getByTestId('report-chart')).toBeVisible();

    await select.selectOption('docs-national-priority');
    await expect(page.getByTestId('report-table')).toBeVisible();

    // Verifica troca de idioma PT <-> EN
    await page.getByTestId('lang-en').click();
    await expect(page.getByRole('heading', { name: 'Reports' })).toBeVisible();
    await page.getByTestId('lang-pt').click();
    await expect(page.getByRole('heading', { name: 'Relatórios' })).toBeVisible();
  });
});
