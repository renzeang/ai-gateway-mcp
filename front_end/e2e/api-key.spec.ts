import { test, expect } from '@playwright/test';

test.describe('API Keys Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/api-keys');
  });

  test('should load API Keys list page', async ({ page }) => {
    await expect(page.getByText('API Keys 管理')).toBeVisible();
    await expect(page.getByTestId('btn-create-api-key')).toBeVisible();
  });

  test('should show search form', async ({ page }) => {
    await expect(page.getByPlaceholder('请输入别名')).toBeVisible();
    await expect(page.getByText('启用状态')).toBeVisible();
  });

  test('should navigate to create page', async ({ page }) => {
    await page.getByTestId('btn-create-api-key').first().click();
    await expect(page).toHaveURL('/#/api-keys/create');
    await expect(page.getByText('创建 API Key')).toBeVisible();
  });

  test('create page should show form fields', async ({ page }) => {
    await page.goto('/#/api-keys/create');
    await expect(page.getByTestId('input-alias')).toBeVisible();
    await expect(page.getByTestId('checkbox-all-servers')).toBeVisible();
    await expect(page.getByTestId('checkbox-no-expire')).toBeVisible();
    await expect(page.getByTestId('btn-back-api-key')).toBeVisible();
  });

  test('create page should have back button', async ({ page }) => {
    await page.goto('/#/api-keys/create');
    await page.getByTestId('btn-back-api-key').click();
    await expect(page).toHaveURL('/#/api-keys');
  });

  test('should show table columns', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 5000 });
    const headers = page.locator('table th');
    await expect(headers).toContainText('别名');
    await expect(headers).toContainText('Key');
    await expect(headers).toContainText('作用域');
    await expect(headers).toContainText('启用状态');
    await expect(headers).toContainText('过期时间');
    await expect(headers).toContainText('最后使用');
    await expect(headers).toContainText('创建时间');
  });
});
