-- V3__drop_base_url_columns.sql
-- 删除 mcp_tools 和 mcp_resources 表中未使用的 base_url 字段
-- base_url 统一使用 server 级别配置

-- 删除 mcp_tools 表的 base_url 字段
ALTER TABLE mcp_tools DROP COLUMN base_url;

-- 删除 mcp_resources 表的 base_url 字段
ALTER TABLE mcp_resources DROP COLUMN base_url;

-- 确保 mcp_server 表的 base_url 字段不为空（添加 NOT NULL 约束）
ALTER TABLE mcp_server MODIFY COLUMN base_url VARCHAR(500) NOT NULL COMMENT '基础 URL';
