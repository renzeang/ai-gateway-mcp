package org.veronicadavichi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建 API Key 请求
 */
@Data
public class CreateApiKeyRequest {
    private String alias;
    private List<Long> serverIds; // null = 全部
    private LocalDateTime expiresAt; // null = 永不过期
}
