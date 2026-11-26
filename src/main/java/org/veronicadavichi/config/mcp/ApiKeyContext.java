package org.veronicadavichi.config.mcp;

import java.util.List;

/**
 * API Key 认证上下文
 * 携带已验证的 Key 信息，在 Filter → Handler → ProtocolHandler 间传递
 */
public class ApiKeyContext {

    private final Long keyId;
    private final String alias;
    private final List<Long> serverIds; // null = 全部 Server

    public ApiKeyContext(Long keyId, String alias, List<Long> serverIds) {
        this.keyId = keyId;
        this.alias = alias;
        this.serverIds = serverIds;
    }

    public Long getKeyId() { return keyId; }

    public String getAlias() { return alias; }

    public List<Long> getServerIds() { return serverIds; }

    /**
     * 是否对指定 serverId 有访问权限
     */
    public boolean hasAccessToServer(Long serverId) {
        if (serverIds == null) {
            return true; // null = 全部
        }
        return serverIds.contains(serverId);
    }
}
