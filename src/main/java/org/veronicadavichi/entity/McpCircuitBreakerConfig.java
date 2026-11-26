package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 熔断配置实体
 */
@Data
@Table("mcp_circuit_breaker_config")
public class McpCircuitBreakerConfig {

    @Id
    private Long id;

    @Column("server_id")
    private Long serverId;

    @Column("failure_threshold")
    private Integer failureThreshold;

    @Column("success_threshold")
    private Integer successThreshold;

    @Column("timeout_seconds")
    private Integer timeoutSeconds;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
