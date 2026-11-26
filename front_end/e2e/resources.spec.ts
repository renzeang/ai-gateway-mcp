import { test, expect } from '@playwright/test';

test.describe('Resources Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/resources');
  });

  test('should load resource list page', async ({ page }) => {
    await expect(page.getByText('Resources 管理')).toBeVisible();
    await expect(page.getByText('创建 Resource')).toBeVisible();
  });

  test('should show import and export buttons', async ({ page }) => {
    await expect(page.getByText('导入')).toBeVisible();
    await expect(page.getByText('导出')).toBeVisible();
  });

  test('should navigate to create page', async ({ page }) => {
    await page.getByText('创建 Resource').click();
    await expect(page).toHaveURL('/#/resources/create');
    await expect(page.getByText('创建 Resource')).toBeVisible();
  });
});
