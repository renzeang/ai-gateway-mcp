package org.veronicadavichi.service;

import org.veronicadavichi.config.mcp.ApiKeyContext;
import org.veronicadavichi.dto.ApiKeyCreateResult;
import org.veronicadavichi.dto.CreateApiKeyRequest;
import org.veronicadavichi.entity.McpApiKey;

import java.util.List;

/**
 * MCP API Key 服务接口
 */
public interface McpApiKeyService {

    /**
     * 创建 API Key — 生成随机 key、hash、存 DB、返回含明文的创建结果
     */
    ApiKeyCreateResult create(CreateApiKeyRequest request);

    /**
     * 验证明文 Key — hash 查 DB → 检查 enabled/expired → 返回 ApiKeyContext
     * 失败抛出 McpApiKeyValidationException
     */
    ApiKeyContext validate(String plaintextKey);

    /**
     * 查询详情（不含 key_hash）
     */
    McpApiKey getById(Long id);

    /**
     * 分页查询列表
     */
    List<McpApiKey> list(String alias, Boolean enabled, int page, int size);

    long countList(String alias, Boolean enabled);

    /**
     * 启用/停用 — 停用时清理关联 session
     */
    McpApiKey toggle(Long id, boolean enabled);

    /**
     * 物理删除 — 清理关联 session
     */
    void delete(Long id);

    /**
     * 更新最后使用时间
     */
    void updateLastUsed(Long keyId);
}
