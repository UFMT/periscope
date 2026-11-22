import { expect, test } from '@playwright/test';
import { openProjectWithPatents } from './helpers';

test.describe('harmonization', () => {
  test('busca sugestão ACME GMBH, lista entidades e cria/edita/remove regra', async ({ page }) => {
    await openProjectWithPatents(page);

    const url = page.url();
    const match = url.match(/\/projects\/([^/]+)\/patents/);
    expect(match).toBeTruthy();
    await page.goto(`./projects/${match![1]}/harmonization`);

    await expect(page.getByRole('heading', { name: 'Harmonização' })).toBeVisible();
    await expect(page.getByTestId('entities-table')).toBeVisible();

    await page.getByTestId('harmonization-query').fill('ACME GMBH');
    await expect(page.getByTestId('suggestions-list')).toBeVisible();
    await expect(page.getByTestId('suggestions-list')).toContainText('ACME GMBH');

    const ruleName = `E2E RULE ${Date.now()}`;
    await page.getByTestId('rule-name').fill(ruleName);
    await page.getByTestId('rule-submit').click();

    await expect(page.getByTestId('rules-table')).toContainText(ruleName);
    const ruleRow = page.getByTestId('rules-table').locator('tr', { hasText: ruleName });
    page.once('dialog', (dialog) => dialog.accept());
    await ruleRow.getByRole('button', { name: 'Excluir' }).click();
    await expect(page.getByTestId('rules-table').locator('tr', { hasText: ruleName })).toHaveCount(0);
  });
});
