package org.veronicadavichi.config.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 会话数据类
 * 表示一个 MCP 客户端会话，包含会话状态和活动信息
 */
public class McpSession {

    /**
     * 会话 ID（UUID v4）
     */
    private final String sessionId;

    /**
     * 关联的 SSE 连接 ID（可选）
     */
    private String connectionId;

    /**
     * 会话创建时间
     */
    private final Instant createdAt;

    /**
     * 最后活动时间
     */
    private Instant lastActivityAt;

    /**
     * 会话状态（initialize 后的能力协商结果）
     */
    private final Map<String, Object> state;

    /**
     * 是否已完成 initialize 握手
     */
    private boolean initialized;

    /**
     * 关联的 API Key 认证上下文
     */
    private ApiKeyContext apiKeyContext;

    /**
     * 创建新会话
     *
     * @param sessionId 会话 ID
     */
    public McpSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.state = new ConcurrentHashMap<>();
        this.initialized = false;
    }

    /**
     * 获取会话 ID
     *
     * @return sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取关联的 SSE 连接 ID
     *
     * @return connectionId
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * 设置关联的 SSE 连接 ID
     *
     * @param connectionId 连接 ID
     */
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * 获取会话创建时间
     *
     * @return createdAt
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取最后活动时间
     *
     * @return lastActivityAt
     */
    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * 更新最后活动时间（调用此方法表示会话有活动）
     */
    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * 检查会话是否已过期
     *
     * @param timeout 超时时间
     * @return 如果超过 timeout 时间无活动则返回 true
     */
    public boolean isExpired(Duration timeout) {
        Instant expiryTime = lastActivityAt.plus(timeout);
        return Instant.now().isAfter(expiryTime);
    }

    /**
     * 获取会话状态
     *
     * @return state
     */
    public Map<String, Object> getState() {
        return state;
    }

    /**
     * 设置会话状态值
     *
     * @param key   键
     * @param value 值
     */
    public void setState(String key, Object value) {
        this.state.put(key, value);
    }

    /**
     * 获取会话状态值
     *
     * @param key 键
     * @return 值
     */
    public Object getStateValue(String key) {
        return state.get(key);
    }

    /**
     * 检查是否已完成 initialize 握手
     *
     * @return initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 设置 initialize 状态
     *
     * @param initialized 是否已初始化
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public ApiKeyContext getApiKeyContext() {
        return apiKeyContext;
    }

    public void setApiKeyContext(ApiKeyContext apiKeyContext) {
        this.apiKeyContext = apiKeyContext;
    }
}
