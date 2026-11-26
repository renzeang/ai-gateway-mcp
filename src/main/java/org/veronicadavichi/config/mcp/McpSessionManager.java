package org.veronicadavichi.config.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP 会话管理器
 * 负责创建、管理和清理 MCP 会话
 *
 * 会话特性：
 * - 会话 ID：UUID v4
 * - 存储：内存 ConcurrentHashMap（重启后失效，符合 MCP 规范）
 * - 过期：30 分钟无活动自动清理
 */
@Component
public class McpSessionManager {

    private static final Logger log = LoggerFactory.getLogger(McpSessionManager.class);

    /**
     * 会话存储：sessionId -> McpSession
     */
    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

    /**
     * 会话超时时间（30 分钟）
     */
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);

    /**
     * 清理任务执行间隔（5 分钟）
     */
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);

    /**
     * 定时清理执行器
     */
    private final ScheduledExecutorService cleanupExecutor;

    public McpSessionManager() {
        // 启动定时清理任务
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mcp-session-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                CLEANUP_INTERVAL.toMinutes(),
                CLEANUP_INTERVAL.toMinutes(),
                TimeUnit.MINUTES
        );

        log.info("McpSessionManager initialized with {} minute timeout", SESSION_TIMEOUT.toMinutes());
    }

    /**
     * 创建新会话
     *
     * @return 新创建的会话
     */
    public McpSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        McpSession session = new McpSession(sessionId);
        sessions.put(sessionId, session);
        log.info("Session created: sessionId={}, totalSessions={}", sessionId, sessions.size());
        return session;
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话，如果不存在则返回 null
     */
    public McpSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        McpSession session = sessions.get(sessionId);
        if (session != null) {
            // 更新活动时间
            session.touch();
        }
        return session;
    }

    /**
     * 获取或创建会话
     * 如果 sessionId 为空或对应的会话不存在，则创建新会话
     *
     * @param sessionId 会话 ID（可为 null）
     * @return 现有会话或新创建的会话
     */
    public McpSession getOrCreateSession(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            McpSession existing = getSession(sessionId);
            if (existing != null) {
                log.debug("Session reused: sessionId={}", sessionId);
                return existing;
            }
        }
        McpSession newSession = createSession();
        log.debug("Session created: sessionId={}", newSession.getSessionId());
        return newSession;
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话 ID
     * @return 被移除的会话，如果不存在则返回 null
     */
    public McpSession removeSession(String sessionId) {
        McpSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("Session removed: sessionId={}, remainingSessions={}", sessionId, sessions.size());
        } else {
            log.warn("Attempted to remove non-existent session: sessionId={}", sessionId);
        }
        return removed;
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 如果会话存在则返回 true
     */
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * 获取当前活跃会话数
     *
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 获取所有活跃会话（供外部遍历使用）
     */
    public Map<String, McpSession> getSessions() {
        return sessions;
    }

    /**
     * 移除关联到指定 API Key 的所有会话及其 SSE 连接
     *
     * @param keyId API Key 记录 ID
     * @return 被移除的会话数
     */
    public int removeSessionsByKey(Long keyId, SseConnectionManager connectionManager) {
        int removed = 0;
        List<String> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<String, McpSession> entry : sessions.entrySet()) {
            McpSession session = entry.getValue();
            if (session.getApiKeyContext() != null
                    && keyId.equals(session.getApiKeyContext().getKeyId())) {
                String connId = session.getConnectionId();
                if (connId != null) {
                    connectionManager.removeConnection(connId);
                }
                toRemove.add(entry.getKey());
                removed++;
            }
        }
        for (String sessionId : toRemove) {
            sessions.remove(sessionId);
        }
        if (removed > 0) {
            log.info("Removed {} sessions for API Key id={}", removed, keyId);
        }
        return removed;
    }

    /**
     * 清理所有过期的会话
     */
    private void cleanupExpiredSessions() {
        final int[] cleaned = {0};
        sessions.entrySet().removeIf(entry -> {
            McpSession session = entry.getValue();
            if (session.isExpired(SESSION_TIMEOUT)) {
                log.info("Session expired: sessionId={}, lastActivity={}",
                        session.getSessionId(), session.getLastActivityAt());
                cleaned[0]++;
                return true;
            }
            return false;
        });

        if (cleaned[0] > 0) {
            log.info("Cleanup completed: removed {} expired sessions, remaining {} sessions",
                    cleaned[0], sessions.size());
        } else {
            log.debug("Cleanup completed: no expired sessions, total {} sessions", sessions.size());
        }
    }

    /**
     * 关闭会话管理器
     * 释放定时任务执行器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("McpSessionManager shutdown, cleared {} sessions", sessions.size());
        sessions.clear();
    }
}
