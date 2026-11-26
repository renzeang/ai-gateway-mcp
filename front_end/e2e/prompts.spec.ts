import { test, expect } from '@playwright/test';

test.describe('Prompts Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/prompts');
  });

  test('should load prompt list page', async ({ page }) => {
    await expect(page.getByText('Prompts 管理')).toBeVisible();
    await expect(page.getByText('创建 Prompt')).toBeVisible();
  });

  test('should show import and export buttons', async ({ page }) => {
    await expect(page.getByText('导入')).toBeVisible();
    await expect(page.getByText('导出')).toBeVisible();
  });

  test('should navigate to create page', async ({ page }) => {
    await page.getByText('创建 Prompt').click();
    await expect(page).toHaveURL('/#/prompts/create');
    await expect(page.getByText('创建 Prompt')).toBeVisible();
  });
});
