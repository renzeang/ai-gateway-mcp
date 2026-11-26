package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * MCP Server 配置实体
 */
@Data
@Table("mcp_server")
public class McpServer {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("base_url")
    @NotBlank(message = "Base URL 不能为空")
    @Pattern(regexp = "^https?://[\\w.:\\-_/]+$", message = "Base URL 格式不正确，必须以 http:// 或 https:// 开头")
    private String baseUrl;

    @Column("description")
    private String description;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
