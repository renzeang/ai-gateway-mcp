-- AI Gateway MCP 数据库初始化脚本
-- 字符集配置
CREATE DATABASE IF NOT EXISTS ai_gateway_mcp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_gateway_mcp;

-- MCP Servers 表
CREATE TABLE IF NOT EXISTS `mcp_server` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `name` VARCHAR(255) NOT NULL COMMENT '服务名称',
    `base_url` VARCHAR(512) NOT NULL COMMENT '基础 URL',
    `description` VARCHAR(1024) COMMENT '描述',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 服务配置表';

-- MCP Tools 表
CREATE TABLE IF NOT EXISTS `mcp_tools` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `server_id` BIGINT NOT NULL COMMENT '关联服务 ID',
    `name` VARCHAR(255) NOT NULL COMMENT '工具名称',
    `description` VARCHAR(1024) COMMENT '描述',
    `path` VARCHAR(512) NOT NULL COMMENT '请求路径',
    `base_url` VARCHAR(512) COMMENT '基础 URL',
    `method` VARCHAR(20) NOT NULL COMMENT 'HTTP 方法',
    `input_schema` TEXT COMMENT '输入参数 Schema',
    `header_mappings` TEXT COMMENT '请求头映射',
    `timeout_ms` INT DEFAULT 30000 COMMENT '超时时间 (毫秒)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_name` (`name`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 工具登记表';

-- MCP Resources 表
CREATE TABLE IF NOT EXISTS `mcp_resources` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `server_id` BIGINT NOT NULL COMMENT '关联服务 ID',
    `uri` VARCHAR(512) NOT NULL COMMENT '资源 URI',
    `name` VARCHAR(255) NOT NULL COMMENT '资源名称',
    `description` VARCHAR(1024) COMMENT '描述',
    `path` VARCHAR(512) NOT NULL COMMENT '请求路径',
    `base_url` VARCHAR(512) COMMENT '基础 URL',
    `method` VARCHAR(20) NOT NULL COMMENT 'HTTP 方法',
    `template` TEXT COMMENT '模板',
    `input_schema` TEXT COMMENT '输入参数 Schema',
    `mime_type` VARCHAR(100) COMMENT 'MIME 类型',
    `header_mappings` TEXT COMMENT '请求头映射',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_uri` (`uri`),
    KEY `idx_name` (`name`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 资源登记表';

-- MCP Prompts 表
CREATE TABLE IF NOT EXISTS `mcp_prompts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `server_id` BIGINT NOT NULL COMMENT '关联服务 ID',
    `name` VARCHAR(255) NOT NULL COMMENT '提示词名称',
    `description` VARCHAR(1024) COMMENT '描述',
    `template` TEXT COMMENT '模板',
    `arguments` TEXT COMMENT '参数定义',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_name` (`name`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 提示词登记表';

-- MCP 审计日志表
CREATE TABLE IF NOT EXISTS `mcp_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `session_id` VARCHAR(100) COMMENT '会话 ID',
    `method` VARCHAR(50) COMMENT '请求方法',
    `params` TEXT COMMENT '请求参数',
    `result` TEXT COMMENT '响应结果',
    `duration_ms` BIGINT COMMENT '耗时 (毫秒)',
    `status` VARCHAR(20) COMMENT '状态',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_method` (`method`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 审计日志表';

-- MCP 限流配置表
CREATE TABLE IF NOT EXISTS `mcp_rate_limit_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `server_id` BIGINT NOT NULL COMMENT '关联服务 ID',
    `method_name` VARCHAR(255) NOT NULL COMMENT '方法名称',
    `max_requests` INT DEFAULT 100 COMMENT '最大请求数',
    `window_seconds` INT DEFAULT 60 COMMENT '时间窗口 (秒)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_method_name` (`method_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 限流配置表';

-- MCP 熔断配置表
CREATE TABLE IF NOT EXISTS `mcp_circuit_breaker_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `server_id` BIGINT NOT NULL COMMENT '关联服务 ID',
    `method_name` VARCHAR(255) NOT NULL COMMENT '方法名称',
    `failure_threshold` INT DEFAULT 5 COMMENT '失败阈值',
    `success_threshold` INT DEFAULT 3 COMMENT '成功阈值',
    `timeout_seconds` INT DEFAULT 30 COMMENT '超时时间 (秒)',
    `window_seconds` INT DEFAULT 60 COMMENT '时间窗口 (秒)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_method_name` (`method_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 熔断配置表';
