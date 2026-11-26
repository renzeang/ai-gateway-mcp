package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 限流配置实体
 */
@Data
@Table("mcp_rate_limit_config")
public class McpRateLimitConfig {

    @Id
    private Long id;

    @Column("target_name")
    private String targetName;

    @Column("target_type")
    private String targetType;

    @Column("max_requests_per_minute")
    private Integer maxRequestsPerMinute;

    @Column("max_requests_per_hour")
    private Integer maxRequestsPerHour;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
