-- V2__add_input_schema_and_base_url.sql
-- 为 MCP Resources 添加 input_schema 字段
-- 为 MCP Tools 和 MCP Resources 添加 base_url 字段（可选，用于覆盖 server 级别配置）

-- 1. 为 mcp_resources 添加 input_schema 字段
ALTER TABLE mcp_resources
ADD COLUMN input_schema JSON COMMENT '输入参数 schema（用于解析 x-header 映射）'
AFTER template;

-- 2. 为 mcp_tools 添加 base_url 字段（可选）
ALTER TABLE mcp_tools
ADD COLUMN base_url VARCHAR(500) COMMENT '后端服务基础 URL（可选，为空则使用 server 级别配置）'
AFTER path;

-- 3. 为 mcp_resources 添加 base_url 字段（可选）
ALTER TABLE mcp_resources
ADD COLUMN base_url VARCHAR(500) COMMENT '后端服务基础 URL（可选，为空则使用 server 级别配置）'
AFTER path;
