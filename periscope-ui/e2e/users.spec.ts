import { expect, test } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('users', () => {
  test('cria, edita e exclui usuário pela SPA', async ({ page }) => {
    const username = `e2euser_${Date.now()}`;

    await loginAsAdmin(page);
    await page.getByRole('link', { name: 'Usuários' }).click();
    await expect(page).toHaveURL(/\/users\/?$/);
    await expect(page.getByTestId('users-table')).toBeVisible();

    await page.getByTestId('user-create').click();
    await expect(page.getByTestId('user-form')).toBeVisible();
    await page.getByTestId('user-username').fill(username);
    await page.getByTestId('user-firstname').fill('E2E');
    await page.getByTestId('user-lastname').fill('Tester');
    await page.getByTestId('user-email').fill(`${username}@example.com`);
    await page.getByTestId('user-password').fill('Secret123!');
    await page.getByTestId('user-save').click();

    const row = page.getByTestId(`user-row-${username}`);
    await expect(row).toBeVisible();

    await row.getByRole('button', { name: 'Editar' }).click();
    await page.getByTestId('user-firstname').fill('E2EUpdated');
    await page.getByTestId('user-save').click();
    await expect(row).toContainText('E2EUpdated');

    page.once('dialog', (dialog) => dialog.accept());
    await row.getByRole('button', { name: 'Excluir' }).click();
    await expect(page.getByTestId(`user-row-${username}`)).toHaveCount(0);
  });
});
