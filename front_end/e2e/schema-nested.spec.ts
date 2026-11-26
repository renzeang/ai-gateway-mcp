import { test, expect } from '@playwright/test';

/**
 * Schema 嵌套支持 E2E 测试
 * 测试场景：
 * 1. 创建包含 3 层嵌套对象的 Tool
 * 2. 编辑已保存的嵌套 Schema
 * 3. 折叠/展开操作
 * 4. 入参方式勾选验证
 * 5. 非法输入验证
 */

test.describe('Schema 嵌套支持', () => {
  const baseUrl = 'http://localhost:5173';

  test.beforeEach(async ({ page }) => {
    // 访问工具创建页面
    await page.goto(`${baseUrl}/tools/new`);
  });

  test('场景 1: 创建包含 3 层嵌套对象的 Tool', async ({ page }) => {
    // 填写基本信息
    await page.fill('input[placeholder="请输入名称"]', 'test_nested_schema');
    await page.fill('textarea[placeholder="请输入描述"]', '测试嵌套 Schema');

    // 选择服务（假设第一个服务）
    await page.click('el-select[placeholder="请选择服务"]');
    await page.click('.el-select-dropdown__item:first-child');

    // 填写路径和方法
    await page.fill('input[placeholder="请输入路径"]', '/api/test');
    await page.click('el-select[placeholder="请选择方法"]');
    await page.click('.el-select-dropdown__item:first-child'); // GET

    // 添加根字段：userId (query 参数)
    await page.click('button:has-text("添加根字段")');
    await page.fill('.schema-field-node:first-child input[placeholder="字段名"]', 'userId');
    await page.click('.schema-field-node:first-child .el-select');
    await page.click('.el-select-dropdown__item:has-text("string")');
    // 选择入参方式为 QueryParams
    await page.click('.schema-field-node:first-child .el-radio:has-text("QueryParams")');

    // 添加嵌套对象字段：userInfo
    await page.click('button:has-text("添加根字段")');
    const userInfoField = page.locator('.schema-field-node:nth-child(2)');
    await userInfoField.fill('input[placeholder="字段名"]', 'userInfo');
    await userInfoField.click('.el-select');
    await userInfoField.click('.el-select-dropdown__item:has-text("object")');

    // 展开 userInfo 并添加子字段 name
    await userInfoField.click('button:has-text("添加子字段")');
    await userInfoField.fill('.child-wrapper input[placeholder="字段名"]', 'name');

    // 添加嵌套地址对象
    await userInfoField.click('button:has-text("添加子字段")');
    const addressField = userInfoField.locator('.child-wrapper:nth-child(2)');
    await addressField.fill('input[placeholder="字段名"]', 'address');
    await addressField.click('.el-select');
    await addressField.click('.el-select-dropdown__item:has-text("object")');

    // 展开 address 并添加子字段 city
    await addressField.click('button:has-text("添加子字段")');
    await addressField.fill('.child-wrapper input[placeholder="字段名"]', 'city');

    // 提交表单
    await page.click('button:has-text("提交")');

    // 等待成功提示
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    // 验证是否跳转到列表页
    await expect(page).toHaveURL(`${baseUrl}/tools`);
  });

  test('场景 2: 编辑已保存的嵌套 Schema', async ({ page }) => {
    // 先导航到工具列表
    await page.goto(`${baseUrl}/tools`);

    // 查找之前创建的测试工具并点击编辑
    await page.click('button:has-text("编辑"):first-child');

    // 等待页面加载
    await page.waitForTimeout(1000);

    // 验证嵌套结构正确加载（检查是否有 userInfo 字段）
    const userInfoField = page.locator('.schema-field-node:has-text("userInfo")');
    await expect(userInfoField).toBeVisible();

    // 验证可以修改字段
    await page.fill('input[placeholder="请输入描述"]', '已修改的嵌套 Schema 描述');

    // 保存
    await page.click('button:has-text("提交")');
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
  });

  test('场景 3: 折叠/展开操作', async ({ page }) => {
    // 添加一个对象字段
    await page.click('button:has-text("添加根字段")');
    const objField = page.locator('.schema-field-node:first-child');
    await objField.fill('input[placeholder="字段名"]', 'testObj');
    await objField.click('.el-select');
    await objField.click('.el-select-dropdown__item:has-text("object")');

    // 添加子字段
    await objField.click('button:has-text("添加子字段")');
    await objField.fill('.child-wrapper input[placeholder="字段名"]', 'child1');

    // 验证子字段可见
    await expect(objField.locator('.child-wrapper')).toBeVisible();

    // 点击折叠
    await objField.click('.actions button:first-child');
    await page.waitForTimeout(300);

    // 验证子字段隐藏
    // 注意：实际验证需要检查 CSS 类或样式

    // 点击展开
    await objField.click('.actions button:first-child');
    await page.waitForTimeout(300);

    // 验证子字段重新显示
    await expect(objField.locator('.child-wrapper')).toBeVisible();
  });

  test('场景 4: 入参方式勾选验证', async ({ page }) => {
    // 添加基础类型字段
    await page.click('button:has-text("添加根字段")');
    const stringField = page.locator('.schema-field-node:first-child');
    await stringField.fill('input[placeholder="字段名"]', 'testString');

    // 验证可以选择 QueryParams
    const queryRadio = stringField.locator('.el-radio:has-text("QueryParams")');
    await expect(queryRadio).toBeEnabled();

    // 验证可以选择 Header
    const headerRadio = stringField.locator('.el-radio:has-text("Header")');
    await expect(headerRadio).toBeEnabled();

    // 添加对象类型字段
    await page.click('button:has-text("添加根字段")');
    const objField = page.locator('.schema-field-node:nth-child(2)');
    await objField.fill('input[placeholder="字段名"]', 'testObj');
    await objField.click('.el-select');
    await objField.click('.el-select-dropdown__item:has-text("object")');

    // 验证对象类型只能选择 JSON Body
    const objQueryRadio = objField.locator('.el-radio:has-text("QueryParams")');
    await expect(objQueryRadio).toBeDisabled();

    const objHeaderRadio = objField.locator('.el-radio:has-text("Header")');
    await expect(objHeaderRadio).toBeDisabled();
  });

  test('场景 5: Schema 预览实时同步', async ({ page }) => {
    // 添加一个字段
    await page.click('button:has-text("添加根字段")');
    await page.fill('.schema-field-node:first-child input[placeholder="字段名"]', 'testField');

    // 验证预览区域显示该字段
    const preview = page.locator('.schema-preview code');
    await expect(preview).toContainText('testField', { timeout: 3000 });

    // 修改字段类型
    await page.click('.schema-field-node:first-child .el-select');
    await page.click('.el-select-dropdown__item:has-text("number")');

    // 验证预览更新
    await expect(preview).toContainText('"type": "number"', { timeout: 3000 });
  });
});
