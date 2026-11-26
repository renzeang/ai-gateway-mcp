package org.veronicadavichi.config.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 传输层处理器
 * 提供 MCP over SSE 端点：/mcp/sse
 */
@RestController
@RequestMapping("/mcp")
public class SseHandler {

    private static final Logger log = LoggerFactory.getLogger(SseHandler.class);

    private final McpProtocolHandler protocolHandler;
    private final SseConnectionManager connectionManager;
    private final McpSessionManager sessionManager;

    public SseHandler(McpProtocolHandler protocolHandler,
                      SseConnectionManager connectionManager,
                      McpSessionManager sessionManager) {
        this.protocolHandler = protocolHandler;
        this.connectionManager = connectionManager;
        this.sessionManager = sessionManager;
    }

    /**
     * SSE 连接端点
     * GET /mcp/sse
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestParam(value = "connectionId", required = false) String connectionId,
            HttpServletRequest httpRequest) {

        // 生成或使用提供的 connection ID
        if (connectionId == null || connectionId.isEmpty()) {
            connectionId = java.util.UUID.randomUUID().toString();
        }

        final String finalConnectionId = connectionId;

        // 创建或获取会话，并将会话与 SSE 连接绑定
        McpSession session = sessionManager.getOrCreateSession(null);
        session.setConnectionId(connectionId);

        // 从 Filter 设置的 request attribute 中获取 ApiKeyContext，绑定到 session
        ApiKeyContext apiKeyContext = (ApiKeyContext) httpRequest.getAttribute("apiKeyContext");
        session.setApiKeyContext(apiKeyContext);

        log.info("SSE connection request: connectionId={}, sessionId={}", connectionId, session.getSessionId());

        // 创建 SSE Emitter（超时时间设为 0 表示永不过期）
        SseEmitter emitter = new SseEmitter(0L);

        // 注册连接
        connectionManager.addConnection(connectionId, emitter);

        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE connection completed: connectionId={}", finalConnectionId);
            connectionManager.removeConnection(finalConnectionId);
            // 解绑会话
            session.setConnectionId(null);
        });

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout: connectionId={}", finalConnectionId);
            connectionManager.removeConnection(finalConnectionId);
            // 解绑会话
            session.setConnectionId(null);
        });

        // 设置错误回调
        emitter.onError(throwable -> {
            log.error("SSE connection error: connectionId={}", finalConnectionId, throwable);
            connectionManager.removeConnection(finalConnectionId);
            // 解绑会话
            session.setConnectionId(null);
        });

        try {
            // 发送连接成功消息
            emitter.send(Map.of(
                "type", "connected",
                "connectionId", connectionId
            ));
            log.info("SSE connection established: connectionId={}", connectionId);
        } catch (IOException e) {
            log.error("Failed to send connection confirmation: connectionId={}", connectionId, e);
            connectionManager.removeConnection(connectionId);
        }

        return emitter;
    }

    /**
     * 接收客户端消息端点
     * POST /mcp/message
     */
    @PostMapping("/message")
    public String handleMessage(
            @RequestParam("connectionId") String connectionId,
            @RequestBody String message) {

        log.info("SSE message received: connectionId={}", connectionId);

        // 检查连接是否存在
        SseEmitter emitter = connectionManager.getConnection(connectionId);
        if (emitter == null) {
            log.warn("SSE connection not found: connectionId={}", connectionId);
            return "{\"status\":\"error\",\"message\":\"Connection not found: " + connectionId + "\"}";
        }

        try {
            // 从 session 获取 ApiKeyContext
            ApiKeyContext apiKeyContext = null;
            for (McpSession sess : sessionManager.getSessions().values()) {
                if (connectionId.equals(sess.getConnectionId())) {
                    apiKeyContext = sess.getApiKeyContext();
                    break;
                }
            }

            // 使用协议处理器处理消息
            String response = protocolHandler.handleRequest(message, apiKeyContext);

            if (response != null) {
                // 异步发送响应
                sendResponse(emitter, connectionId, response);
            }

            return "{\"status\":\"ok\"}";
        } catch (IOException e) {
            // SSE 连接已关闭，清理连接
            log.warn("SSE connection closed unexpectedly: connectionId={}", connectionId);
            connectionManager.removeConnection(connectionId);
            return "{\"status\":\"error\",\"message\":\"Connection closed\"}";
        } catch (Exception e) {
            log.error("Error processing message: connectionId={}", connectionId, e);
            try {
                sendResponse(emitter, connectionId, buildErrorResponse(null, e.getMessage()));
            } catch (IOException ex) {
                // 发送失败，清理连接
                log.warn("Failed to send error response, removing connection: connectionId={}", connectionId);
                connectionManager.removeConnection(connectionId);
            }
            return "{\"status\":\"error\",\"message\":\"Internal error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 异步发送响应到 SSE 客户端
     */
    private void sendResponse(SseEmitter emitter, String connectionId, String response) throws IOException {
        emitter.send(Map.of(
            "type", "message",
            "connectionId", connectionId,
            "data", response
        ));
    }

    /**
     * 构建错误响应（带 id）
     */
    private String buildErrorResponse(Object id, String message) {
        return String.format(
            "{\"jsonrpc\":\"2.0\",\"id\":%s,\"error\":{\"code\":-32000,\"message\":\"%s\"}}",
            id != null ? id : "null",
            message.replace("\"", "\\\"")
        );
    }

    /**
     * 构建错误响应（不带 id，用于非 JSON-RPC 错误）
     */
    private String buildErrorResponse(String message) {
        return String.format(
            "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32000,\"message\":\"%s\"}}",
            message.replace("\"", "\\\"")
        );
    }

    /**
     * Streamable HTTP 端点
     * POST /mcp
     * 支持 MCP over HTTP 直接请求（非 SSE）
     *
     * 响应头：
     * - Mcp-Session-Id: 在 initialize 响应中返回，或绑定现有会话
     */
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> handleHttpRequest(
            @RequestBody String body,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Mcp-Session-Id", required = false) String sessionIdHeader,
            HttpServletRequest httpRequest) {

        log.info("MCP HTTP request: {}", body);

        // 获取或创建会话
        McpSession session = sessionManager.getOrCreateSession(sessionIdHeader);

        // 从 Filter 设置的 request attribute 中获取 ApiKeyContext
        ApiKeyContext apiKeyContext = (ApiKeyContext) httpRequest.getAttribute("apiKeyContext");

        try {
            // 使用协议处理器处理请求
            String response = protocolHandler.handleRequest(body, apiKeyContext);

            if (response == null) {
                // notification 请求，返回 202 Accepted
                return ResponseEntity.accepted().build();
            }

            // 构建响应，添加会话 ID
            ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();

            // 如果是 initialize 响应，设置会话 ID 并标记会话已初始化
            if (body.contains("\"initialize\"")) {
                session.setInitialized(true);
                bodyBuilder.header("Mcp-Session-Id", session.getSessionId());
                log.info("Initialize completed: sessionId={}", session.getSessionId());
            } else if (session.isInitialized()) {
                // 已有会话，添加会话 ID 到响应头
                bodyBuilder.header("Mcp-Session-Id", session.getSessionId());
            }

            return bodyBuilder.body(response);

        } catch (Exception e) {
            log.error("Error processing MCP request: sessionId={}", session.getSessionId(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("Internal error: " + e.getMessage()));
        }
    }
}
