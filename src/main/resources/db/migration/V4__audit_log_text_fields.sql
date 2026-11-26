-- V4__audit_log_text_fields.sql
-- 将 mcp_audit_log 的 JSON 字段改为 TEXT 类型，避免 JSON 验证失败

ALTER TABLE mcp_audit_log
MODIFY COLUMN request_body TEXT COMMENT '请求体',
MODIFY COLUMN response_body TEXT COMMENT '响应体';
