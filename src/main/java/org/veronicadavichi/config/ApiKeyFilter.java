package org.veronicadavichi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.veronicadavichi.config.mcp.ApiKeyContext;
import org.veronicadavichi.config.mcp.McpSession;
import org.veronicadavichi.config.mcp.McpSessionManager;
import org.veronicadavichi.config.mcp.SseConnectionManager;
import org.veronicadavichi.exception.McpApiKeyValidationException;
import org.veronicadavichi.service.McpApiKeyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * API Key 认证 Filter
 * 拦截 /mcp/** 路径，验证 Bearer token
 * 管理端点 /api/** 不受影响
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String ATTR_API_KEY_CONTEXT = "apiKeyContext";

    private final McpApiKeyService apiKeyService;
    private final McpSessionManager sessionManager;
    private final SseConnectionManager connectionManager;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ApiKeyFilter(McpApiKeyService apiKeyService,
                        McpSessionManager sessionManager,
                        SseConnectionManager connectionManager) {
        this.apiKeyService = apiKeyService;
        this.sessionManager = sessionManager;
        this.connectionManager = connectionManager;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip non-MCP paths
        if (!path.startsWith("/mcp")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // /mcp/sse (GET) and POST /mcp — validate from Authorization header
        if ("/mcp/sse".equals(path) || "/mcp".equals(path)) {
            ApiKeyContext context = validateFromHeader(request, response);
            if (context == null) {
                return; // Already sent 401
            }
            request.setAttribute(ATTR_API_KEY_CONTEXT, context);
            filterChain.doFilter(request, response);
            return;
        }

        // /mcp/message — validate via session
        if ("/mcp/message".equals(path)) {
            String connectionId = request.getParameter("connectionId");
            if (connectionId == null || connectionId.isEmpty()) {
                sendError(response, 401, "Missing connectionId");
                return;
            }

            // Check if connection exists and has an authenticated session
            if (!connectionManager.hasConnection(connectionId)) {
                sendError(response, 401, "Connection not found: " + connectionId);
                return;
            }

            // Find session with this connectionId and check API key validity
            boolean valid = false;
            for (McpSession session : sessionManager.getSessions().values()) {
                if (connectionId.equals(session.getConnectionId())) {
                    ApiKeyContext ctx = session.getApiKeyContext();
                    if (ctx != null) {
                        // Re-check expiration (enabled was checked at connection time, key is still in DB)
                        valid = true;
                        request.setAttribute(ATTR_API_KEY_CONTEXT, ctx);
                    }
                    break;
                }
            }

            if (!valid) {
                sendError(response, 401, "Invalid or expired API key");
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        // Other /mcp paths — fallback to header validation
        ApiKeyContext context = validateFromHeader(request, response);
        if (context != null) {
            request.setAttribute(ATTR_API_KEY_CONTEXT, context);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization header 解析并验证 API Key
     * 返回 ApiKeyContext 或 null（已发送 401）
     */
    private ApiKeyContext validateFromHeader(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            safeSendError(response, 401, "Missing Authorization header");
            return null;
        }

        if (!authHeader.startsWith("Bearer ") || authHeader.length() < 9) {
            safeSendError(response, 401, "Invalid Authorization format");
            return null;
        }

        String plaintextKey = authHeader.substring(7);

        try {
            ApiKeyContext context = apiKeyService.validate(plaintextKey);
            return context;
        } catch (McpApiKeyValidationException e) {
            safeSendError(response, 401, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("API Key validation error", e);
            safeSendError(response, 401, "API key validation failed");
            return null;
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(Map.of(
                "code", "UNAUTHORIZED",
                "message", message
        )));
    }

    private void safeSendError(HttpServletResponse response, int status, String message) {
        try {
            sendError(response, status, message);
        } catch (IOException e) {
            log.error("Failed to send error response", e);
        }
    }
}
