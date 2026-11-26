import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should navigate to Servers page', async ({ page }) => {
    await page.getByTestId('nav-servers').click();
    await expect(page).toHaveURL('/#/servers');
    await expect(page.getByText('Servers 管理')).toBeVisible();
  });

  test('should navigate to Tools page', async ({ page }) => {
    await page.getByTestId('nav-tools').click();
    await expect(page).toHaveURL('/#/tools');
    await expect(page.getByText('Tools 管理')).toBeVisible();
  });

  test('should navigate to Resources page', async ({ page }) => {
    await page.getByTestId('nav-resources').click();
    await expect(page).toHaveURL('/#/resources');
    await expect(page.getByText('Resources 管理')).toBeVisible();
  });

  test('should navigate to Prompts page', async ({ page }) => {
    await page.getByTestId('nav-prompts').click();
    await expect(page).toHaveURL('/#/prompts');
    await expect(page.getByText('Prompts 管理')).toBeVisible();
  });

  test('should navigate to Audit Logs page', async ({ page }) => {
    await page.getByTestId('nav-audit-logs').click();
    await expect(page).toHaveURL('/#/audit-logs');
    await expect(page.getByText('审计日志')).toBeVisible();
  });

  test('should navigate to API Keys page', async ({ page }) => {
    await page.getByTestId('nav-api-keys').click();
    await expect(page).toHaveURL('/#/api-keys');
    await expect(page.getByText('API Keys 管理')).toBeVisible();
  });

  test('should keep current route after refresh', async ({ page }) => {
    await page.getByTestId('nav-tools').click();
    await expect(page).toHaveURL('/#/tools');
    await page.reload();
    await expect(page).toHaveURL('/#/tools');
  });
});
