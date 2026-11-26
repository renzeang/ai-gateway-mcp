import { test, expect } from '@playwright/test';

test.describe('Tools Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/tools');
  });

  test('should load tool list page', async ({ page }) => {
    await expect(page.getByText('Tools 管理')).toBeVisible();
    await expect(page.getByText('创建 Tool')).toBeVisible();
  });

  test('should show import and export buttons', async ({ page }) => {
    await expect(page.getByText('导入')).toBeVisible();
    await expect(page.getByText('导出')).toBeVisible();
  });

  test('should navigate to create page', async ({ page }) => {
    await page.getByText('创建 Tool').click();
    await expect(page).toHaveURL('/#/tools/create');
    await expect(page.getByText('创建 Tool')).toBeVisible();
  });

  test('should navigate to import page', async ({ page }) => {
    await page.getByText('导入').click();
    await expect(page).toHaveURL('/#/tools/import');
    await expect(page.getByText('导入 Tools')).toBeVisible();
  });
});
