package org.veronicadavichi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veronicadavichi.config.mcp.ApiKeyContext;
import org.veronicadavichi.config.mcp.McpSession;
import org.veronicadavichi.config.mcp.McpSessionManager;
import org.veronicadavichi.config.mcp.SseConnectionManager;
import org.veronicadavichi.dto.ApiKeyCreateResult;
import org.veronicadavichi.dto.CreateApiKeyRequest;
import org.veronicadavichi.entity.McpApiKey;
import org.veronicadavichi.exception.McpApiKeyValidationException;
import org.veronicadavichi.mapper.McpApiKeyMapper;
import org.veronicadavichi.service.McpApiKeyService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class McpApiKeyServiceImpl implements McpApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(McpApiKeyServiceImpl.class);
    private static final String KEY_PREFIX = "mkp-";
    private static final int RANDOM_BYTES = 32;
    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpApiKeyMapper apiKeyMapper;
    private final McpSessionManager sessionManager;
    private final SseConnectionManager connectionManager;
    private final org.veronicadavichi.mapper.McpServerMapper serverMapper;

    public McpApiKeyServiceImpl(McpApiKeyMapper apiKeyMapper,
                                McpSessionManager sessionManager,
                                SseConnectionManager connectionManager,
                                org.veronicadavichi.mapper.McpServerMapper serverMapper) {
        this.apiKeyMapper = apiKeyMapper;
        this.sessionManager = sessionManager;
        this.connectionManager = connectionManager;
        this.serverMapper = serverMapper;
    }

    @Override
    @Transactional
    public ApiKeyCreateResult create(CreateApiKeyRequest request) {
        String plaintextKey = generateKey();
        String hash = sha256(plaintextKey);
        String keyPrefix = plaintextKey.substring(0, 8);

        McpApiKey apiKey = new McpApiKey();
        apiKey.setAlias(request.getAlias());
        apiKey.setKeyHash(hash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setEnabled(true);
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setCreatedAt(LocalDateTime.now());

        // server_scope: null = 全部, JSON array = 指定 server
        if (request.getServerIds() != null && !request.getServerIds().isEmpty()) {
            try {
                apiKey.setServerScope(MAPPER.writeValueAsString(request.getServerIds()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize server_scope", e);
            }
        }

        apiKeyMapper.insert(apiKey);
        log.info("API Key created: id={}, alias={}, prefix={}", apiKey.getId(), request.getAlias(), keyPrefix);

        return toCreateResult(apiKey, plaintextKey);
    }

    @Override
    public ApiKeyContext validate(String plaintextKey) {
        if (plaintextKey == null || !plaintextKey.startsWith(KEY_PREFIX)) {
            throw new McpApiKeyValidationException("Invalid API key", "invalid_format");
        }

        String hash = sha256(plaintextKey);
        McpApiKey apiKey = apiKeyMapper.selectOneByQuery(
                com.mybatisflex.core.query.QueryWrapper.create().eq("key_hash", hash));

        if (apiKey == null) {
            throw new McpApiKeyValidationException("Invalid API key", "not_found");
        }
        if (!Boolean.TRUE.equals(apiKey.getEnabled())) {
            throw new McpApiKeyValidationException("API key has been deactivated", "deactivated");
        }
        if (apiKey.getExpiresAt() != null && LocalDateTime.now().isAfter(apiKey.getExpiresAt())) {
            throw new McpApiKeyValidationException("API key has expired", "expired");
        }

        List<Long> serverIds = parseServerScope(apiKey.getServerScope());

        // Update last_used_at asynchronously to avoid blocking
        updateLastUsed(apiKey.getId());

        return new ApiKeyContext(apiKey.getId(), apiKey.getAlias(), serverIds);
    }

    @Override
    public McpApiKey getById(Long id) {
        return apiKeyMapper.selectOneById(id);
    }

    @Override
    public List<McpApiKey> list(String alias, Boolean enabled, int page, int size) {
        com.mybatisflex.core.query.QueryWrapper qw = com.mybatisflex.core.query.QueryWrapper.create()
                .orderBy("created_at", false);
        if (alias != null && !alias.isEmpty()) {
            qw.like("alias", alias);
        }
        if (enabled != null) {
            qw.eq("enabled", enabled);
        }
        qw.limit((page - 1) * size, size);
        List<McpApiKey> list = apiKeyMapper.selectListByQuery(qw);

        // Resolve server names for display
        for (McpApiKey key : list) {
            List<Long> ids = parseServerScope(key.getServerScope());
            if (ids != null && !ids.isEmpty()) {
                List<org.veronicadavichi.entity.McpServer> servers = serverMapper.selectListByIds(ids);
                key.setServerNames(servers.stream().map(org.veronicadavichi.entity.McpServer::getName).toList());
            } else {
                key.setServerNames(null); // all servers
            }
        }
        return list;
    }

    @Override
    public long countList(String alias, Boolean enabled) {
        com.mybatisflex.core.query.QueryWrapper qw = com.mybatisflex.core.query.QueryWrapper.create();
        if (alias != null && !alias.isEmpty()) {
            qw.like("alias", alias);
        }
        if (enabled != null) {
            qw.eq("enabled", enabled);
        }
        return apiKeyMapper.selectCountByQuery(qw);
    }

    @Override
    @Transactional
    public McpApiKey toggle(Long id, boolean enabled) {
        McpApiKey apiKey = apiKeyMapper.selectOneById(id);
        if (apiKey == null) {
            throw new IllegalArgumentException("API Key not found: " + id);
        }

        apiKey.setEnabled(enabled);
        apiKeyMapper.update(apiKey);

        if (!enabled) {
            // Deactivating: remove associated sessions
            removeSessionsByKey(id);
            log.info("API Key deactivated: id={}, alias={}", id, apiKey.getAlias());
        } else {
            log.info("API Key activated: id={}, alias={}", id, apiKey.getAlias());
        }

        return apiKey;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        McpApiKey apiKey = apiKeyMapper.selectOneById(id);
        if (apiKey == null) {
            throw new IllegalArgumentException("API Key not found: " + id);
        }

        // Remove associated sessions before deleting
        removeSessionsByKey(id);
        apiKeyMapper.deleteById(id);
        log.info("API Key deleted: id={}, alias={}", id, apiKey.getAlias());
    }

    @Override
    public void updateLastUsed(Long keyId) {
        McpApiKey apiKey = new McpApiKey();
        apiKey.setId(keyId);
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyMapper.update(apiKey);
    }

    /**
     * 生成 "mkp-" + 32-char base62 random string
     */
    private String generateKey() {
        byte[] bytes = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String random = base62Encode(bytes);
        // Truncate to 32 chars
        return KEY_PREFIX + random.substring(0, 32);
    }

    /**
     * Base62 encode bytes
     */
    private String base62Encode(byte[] bytes) {
        // Use simple modular arithmetic for base62
        StringBuilder sb = new StringBuilder();
        // Convert bytes to a big positive number string in base62
        java.math.BigInteger num = new java.math.BigInteger(1, bytes);
        while (num.signum() != 0) {
            java.math.BigInteger[] divmod = num.divideAndRemainder(java.math.BigInteger.valueOf(62));
            sb.append(BASE62[divmod[1].intValue()]);
            num = divmod[0];
        }
        return sb.reverse().toString();
    }

    /**
     * SHA-256 hash → lowercase hex
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 遍历 session，匹配 keyId 的 session 断开 SSE 连接
     */
    private void removeSessionsByKey(Long keyId) {
        sessionManager.removeSessionsByKey(keyId, connectionManager);
    }

    private List<Long> parseServerScope(String serverScopeJson) {
        if (serverScopeJson == null || serverScopeJson.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(serverScopeJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse server_scope: {}", serverScopeJson, e);
            return null;
        }
    }

    private ApiKeyCreateResult toCreateResult(McpApiKey apiKey, String plaintextKey) {
        ApiKeyCreateResult result = new ApiKeyCreateResult();
        result.setId(apiKey.getId());
        result.setAlias(apiKey.getAlias());
        result.setKey(plaintextKey);
        result.setKeyPrefix(apiKey.getKeyPrefix());
        result.setServerScope(parseServerScope(apiKey.getServerScope()));
        result.setExpiresAt(apiKey.getExpiresAt());
        result.setEnabled(apiKey.getEnabled());
        result.setCreatedAt(apiKey.getCreatedAt());
        return result;
    }
}
