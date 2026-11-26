package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP Resources 登记实体
 */
@Data
@Table("mcp_resources")
public class McpResource {

    @Id
    private Long id;

    @Column("server_id")
    private Long serverId;

    @Column("uri")
    private String uri;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("path")
    private String path;

    @Column("method")
    private String method;

    @Column("template")
    private String template;

    @Column("input_schema")
    private String inputSchema;

    @Column("mime_type")
    private String mimeType;

    @Column("header_mappings")
    private String headerMappings;

    /**
     * 关联的 Server 名称（通过 SQL JOIN 获取，非数据库字段）
     */
    private transient String serverName;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
