package org.veronicadavichi.config.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE 连接管理器
 * 管理所有活跃的 SSE 连接
 */
@Component
public class SseConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionManager.class);

    /**
     * 存储所有活跃连接：connectionId -> SseEmitter
     */
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     * 当前连接数计数器
     */
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    /**
     * 添加连接
     */
    public void addConnection(String connectionId, SseEmitter emitter) {
        connections.put(connectionId, emitter);
        int count = connectionCount.incrementAndGet();
        log.info("Connection added: connectionId={}, totalConnections={}", connectionId, count);
    }

    /**
     * 移除连接
     */
    public void removeConnection(String connectionId) {
        SseEmitter removed = connections.remove(connectionId);
        if (removed != null) {
            int count = connectionCount.decrementAndGet();
            log.info("Connection removed: connectionId={}, totalConnections={}", connectionId, count);
        } else {
            log.warn("Attempted to remove non-existent connection: connectionId={}", connectionId);
        }
    }

    /**
     * 获取连接
     */
    public SseEmitter getConnection(String connectionId) {
        return connections.get(connectionId);
    }

    /**
     * 获取当前连接数
     */
    public int getCurrentConnectionCount() {
        return connectionCount.get();
    }

    /**
     * 检查连接是否存在
     */
    public boolean hasConnection(String connectionId) {
        return connections.containsKey(connectionId);
    }

    /**
     * 广播消息到所有连接（用于服务器主动推送）
     */
    public void broadcast(String message) {
        int sent = 0;
        int failed = 0;

        for (Map.Entry<String, SseEmitter> entry : connections.entrySet()) {
            try {
                entry.getValue().send(Map.of(
                    "type", "broadcast",
                    "connectionId", entry.getKey(),
                    "data", message
                ));
                sent++;
            } catch (Exception e) {
                log.error("Failed to broadcast to connection: {}", entry.getKey(), e);
                failed++;
            }
        }

        if (failed > 0) {
            log.info("Broadcast completed: sent={}, failed={}", sent, failed);
        } else {
            log.debug("Broadcast completed: sent={} connections", sent);
        }
    }

    /**
     * 发送消息到指定连接
     */
    public void sendToConnection(String connectionId, String message) {
        SseEmitter emitter = connections.get(connectionId);
        if (emitter != null) {
            try {
                emitter.send(Map.of(
                    "type", "message",
                    "connectionId", connectionId,
                    "data", message
                ));
                log.debug("Message sent to connection: {}", connectionId);
            } catch (Exception e) {
                log.error("Failed to send message to connection: {}", connectionId, e);
                removeConnection(connectionId);
            }
        } else {
            log.warn("Cannot send to non-existent connection: {}", connectionId);
        }
    }
}
