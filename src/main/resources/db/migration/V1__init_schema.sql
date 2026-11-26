-- V1__init_schema.sql
-- MCP Gateway Management 初始表结构

-- 1. MCP Server 配置表
CREATE TABLE mcp_server (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(100) NOT NULL COMMENT '服务名称（唯一）',
    base_url VARCHAR(500) NOT NULL COMMENT '基础 URL',
    description TEXT COMMENT '描述',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Server 配置表';

-- 2. MCP Tools 登记表
CREATE TABLE mcp_tools (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    server_id BIGINT NOT NULL COMMENT '关联 mcp_server.id',
    name VARCHAR(200) NOT NULL COMMENT '工具名称（全局唯一）',
    description TEXT COMMENT '描述',
    path VARCHAR(500) NOT NULL COMMENT '后端接口路径',
    method VARCHAR(20) NOT NULL COMMENT 'HTTP 方法（GET/POST/PUT/DELETE/PATCH）',
    input_schema JSON NOT NULL COMMENT '原始 inputSchema（含 x-header 标记）',
    header_mappings JSON COMMENT 'Header 映射配置',
    timeout_ms INT DEFAULT 30000 COMMENT '超时时间（毫秒）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name),
    INDEX idx_server (server_id),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_tools_server FOREIGN KEY (server_id) REFERENCES mcp_server(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Tools 登记表';

-- 3. MCP Resources 登记表
CREATE TABLE mcp_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    server_id BIGINT NOT NULL COMMENT '关联 mcp_server.id',
    uri VARCHAR(500) NOT NULL COMMENT 'Resource URI（唯一）',
    name VARCHAR(200) NOT NULL COMMENT '资源名称',
    description TEXT COMMENT '描述',
    path VARCHAR(500) NOT NULL COMMENT '后端接口路径',
    method VARCHAR(20) NOT NULL COMMENT 'HTTP 方法',
    template JSON COMMENT 'URI template 参数定义',
    mime_type VARCHAR(100) COMMENT 'MIME 类型',
    header_mappings JSON COMMENT 'Header 映射配置',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_uri (uri),
    INDEX idx_server (server_id),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_resources_server FOREIGN KEY (server_id) REFERENCES mcp_server(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Resources 登记表';

-- 4. MCP Prompts 登记表
CREATE TABLE mcp_prompts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    server_id BIGINT NOT NULL COMMENT '关联 mcp_server.id',
    name VARCHAR(200) NOT NULL COMMENT '提示词名称（同一 server 下唯一）',
    description TEXT COMMENT '描述',
    template TEXT NOT NULL COMMENT '提示词模板',
    arguments JSON COMMENT '参数定义',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_server_name (server_id, name),
    INDEX idx_server (server_id),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_prompts_server FOREIGN KEY (server_id) REFERENCES mcp_server(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Prompts 登记表';

-- 5. MCP 审计日志表
CREATE TABLE mcp_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_token VARCHAR(500) NOT NULL COMMENT '用户 token（脱敏存储）',
    request_type VARCHAR(50) NOT NULL COMMENT '请求类型（tools/call, resources/read）',
    target_name VARCHAR(200) NOT NULL COMMENT '目标名称（Tool 名或 Resource URI）',
    server_id BIGINT COMMENT '关联的后端 server',
    request_body JSON COMMENT '请求体',
    response_status INT COMMENT '响应状态码',
    response_body JSON COMMENT '响应体',
    is_error TINYINT(1) DEFAULT 0 COMMENT '是否错误',
    error_code VARCHAR(100) COMMENT '错误代码',
    error_message VARCHAR(500) COMMENT '错误消息',
    latency_ms INT COMMENT '耗时（毫秒）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_token (user_token(100)),
    INDEX idx_target (target_name),
    INDEX idx_created (created_at),
    INDEX idx_is_error (is_error)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 审计日志表';

-- 6. MCP 限流配置表（预留，暂不启用）
CREATE TABLE mcp_rate_limit_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    target_name VARCHAR(200) COMMENT '目标名称（NULL=全局）',
    target_type VARCHAR(50) COMMENT '目标类型（tools/resources）',
    max_requests_per_minute INT COMMENT '每分钟最大请求数',
    max_requests_per_hour INT COMMENT '每小时最大请求数',
    enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用（默认关闭）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_target (target_name, target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 限流配置表';

-- 7. MCP 熔断配置表（预留，暂不启用）
CREATE TABLE mcp_circuit_breaker_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    server_id BIGINT NOT NULL COMMENT '关联 mcp_server.id',
    failure_threshold INT DEFAULT 5 COMMENT '失败次数阈值',
    success_threshold INT DEFAULT 3 COMMENT '恢复成功次数阈值',
    timeout_seconds INT DEFAULT 30 COMMENT '熔断超时（秒）',
    enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用（默认关闭）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_server (server_id),
    CONSTRAINT fk_cb_server FOREIGN KEY (server_id) REFERENCES mcp_server(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 熔断配置表';
