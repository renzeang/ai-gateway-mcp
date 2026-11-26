package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Table("mcp_api_key")
public class McpApiKey {

    @Id
    private Long id;

    @Column("alias")
    private String alias;

    @Column("key_hash")
    private String keyHash;

    @Column("key_prefix")
    private String keyPrefix;

    @Column("enabled")
    private Boolean enabled;

    @Column("server_scope")
    private String serverScope;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("last_used_at")
    private LocalDateTime lastUsedAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    private transient List<String> serverNames;
}
