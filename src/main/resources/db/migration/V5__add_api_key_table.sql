-- V5__add_api_key_table.sql
-- 创建 MCP API Key 表 + 审计日志追加 api_key_alias 字段

CREATE TABLE mcp_api_key (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    alias           VARCHAR(100) NOT NULL COMMENT '用户别名',
    key_hash        CHAR(64) NOT NULL COMMENT 'SHA-256(明文 key)',
    key_prefix      VARCHAR(20) NOT NULL COMMENT '显示前缀 (如 mkp-a3xK)',
    enabled         TINYINT(1) DEFAULT 1 COMMENT '是否启用 (0=停用)',
    server_scope    JSON COMMENT '作用域 server ID 数组, NULL=全部',
    expires_at      TIMESTAMP NULL COMMENT '过期时间, NULL=永不过期',
    last_used_at    TIMESTAMP NULL COMMENT '最后使用时间',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_key_hash (key_hash),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP API Key 表';

ALTER TABLE mcp_audit_log ADD COLUMN api_key_alias VARCHAR(100) COMMENT '发起请求的 API Key 别名';
