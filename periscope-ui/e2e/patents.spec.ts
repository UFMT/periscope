import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('patents', () => {
  test('abre projeto com patentes, verifica tabela, detalhe e cadastro manual', async ({ page }) => {
    await openProjectWithPatents(page);

    const table = page.getByTestId('patents-table');
    await expect(table.locator('tbody tr').first()).toBeVisible();

    const firstLink = table.locator('tbody a').first();
    const title = (await firstLink.textContent())?.trim() || '';
    await firstLink.click();

    await expect(page).toHaveURL(/\/patents\/[^/]+$/);
    await expect(page.getByTestId('patent-detail-title')).toBeVisible();
    if (title && title !== '(sem título)') {
      await expect(page.locator('#title')).toHaveValue(title);
    }

    await page.getByRole('link', { name: 'Voltar à lista' }).click();
    await expect(page).toHaveURL(/\/patents\/?$/);

    const manualTitle = `PATENTE MANUAL E2E ${Date.now()}`;
    const manualPub = `BR${Date.now()}`;
    await page.getByTestId('patent-create').click();
    await expect(page.getByTestId('patent-create-form')).toBeVisible();
    await page.getByTestId('new-patent-title').fill(manualTitle);
    await page.getByTestId('new-patent-pubnum').fill(manualPub);
    await page.getByTestId('patent-create-save').click();

    await expect(page.getByTestId('patents-table')).toContainText(manualTitle);
  });
});
