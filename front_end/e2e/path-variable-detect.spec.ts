import { test, expect } from '@playwright/test';

test.describe('Path Variable Auto-Detect', () => {
  test.describe('Resource Page', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/#/resources/create');
    });

    test('should auto-generate path variable when entering /users/{id}', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('URI').fill('/test/users/{id}');
      await page.getByLabel('名称').fill('Test Resource');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with variable
      await page.getByLabel('路径').fill('/api/users/{id}');

      // Wait for debounce
      await page.waitForTimeout(600);

      // Check if Path parameter was auto-generated
      const pathTag = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTag).toBeVisible();

      // Verify the field name is 'id'
      await expect(page.locator('input[placeholder="字段名"]').first()).toHaveValue('id');
    });

    test('should auto-generate multiple path variables', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('URI').fill('/test/orgs/{orgId}/users/{userId}');
      await page.getByLabel('名称').fill('Test Resource 2');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with multiple variables
      await page.getByLabel('路径').fill('/api/orgs/{orgId}/users/{userId}');

      // Wait for debounce
      await page.waitForTimeout(600);

      // Check if two Path parameters were auto-generated
      const pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(2);

      // Verify field names
      const fieldNames = page.locator('input[placeholder="字段名"]');
      await expect(fieldNames.nth(0)).toHaveValue('orgId');
      await expect(fieldNames.nth(1)).toHaveValue('userId');
    });

    test('should remove path variable when removed from path', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('URI').fill('/test/users/{id}');
      await page.getByLabel('名称').fill('Test Resource 3');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with variable
      await page.getByLabel('路径').fill('/api/users/{id}');
      await page.waitForTimeout(600);

      // Verify path variable exists
      let pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(1);

      // Remove the variable from path
      await page.getByLabel('路径').fill('/api/users');
      await page.waitForTimeout(600);

      // Verify path variable was removed
      pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(0);
    });
  });

  test.describe('Tool Page', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/#/tools/create');
    });

    test('should auto-generate path variable when entering /users/{id}', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('名称').fill('Test Tool');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with variable
      await page.getByLabel('路径').fill('/api/users/{id}');

      // Wait for debounce
      await page.waitForTimeout(600);

      // Check if Path parameter was auto-generated
      const pathTag = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTag).toBeVisible();

      // Verify the field name is 'id'
      await expect(page.locator('input[placeholder="字段名"]').first()).toHaveValue('id');
    });

    test('should auto-generate multiple path variables', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('名称').fill('Test Tool 2');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with multiple variables
      await page.getByLabel('路径').fill('/api/orgs/{orgId}/users/{userId}');

      // Wait for debounce
      await page.waitForTimeout(600);

      // Check if two Path parameters were auto-generated
      const pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(2);
    });

    test('should remove path variable when removed from path', async ({ page }) => {
      // Fill in required fields first
      await page.getByLabel('名称').fill('Test Tool 3');
      await page.getByLabel('关联服务').click();
      await page.getByRole('option').first().click();

      // Enter path with variable
      await page.getByLabel('路径').fill('/api/users/{id}');
      await page.waitForTimeout(600);

      // Verify path variable exists
      let pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(1);

      // Remove the variable from path
      await page.getByLabel('路径').fill('/api/users');
      await page.waitForTimeout(600);

      // Verify path variable was removed
      pathTags = page.locator('.param-type-tag', { hasText: 'Path' });
      await expect(pathTags).toHaveCount(0);
    });
  });
});
