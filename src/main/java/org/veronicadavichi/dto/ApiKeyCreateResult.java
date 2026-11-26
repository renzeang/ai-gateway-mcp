package org.veronicadavichi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 创建结果 — 包含明文 key（仅创建时返回一次）
 */
@Data
public class ApiKeyCreateResult {
    private Long id;
    private String alias;
    private String key; // 明文，仅创建时返回
    private String keyPrefix;
    private List<Long> serverScope;
    private LocalDateTime expiresAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
