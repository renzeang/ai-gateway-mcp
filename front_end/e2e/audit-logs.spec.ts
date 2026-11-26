import { test, expect } from '@playwright/test';

test.describe('Audit Logs', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/audit-logs');
  });

  test('should load audit log list page', async ({ page }) => {
    await expect(page.getByText('审计日志')).toBeVisible();
  });

  test('should show search form', async ({ page }) => {
    await expect(page.getByPlaceholder('请输入目标名称')).toBeVisible();
    await expect(page.getByText('请求类型')).toBeVisible();
    await expect(page.getByText('是否错误')).toBeVisible();
  });

  test('should show table headers', async ({ page }) => {
    await page.waitForSelector('table', { timeout: 5000 });
    await expect(page.getByText('类型')).toBeVisible();
    await expect(page.getByText('目标名称')).toBeVisible();
    await expect(page.getByText('User Token')).toBeVisible();
  });
});
