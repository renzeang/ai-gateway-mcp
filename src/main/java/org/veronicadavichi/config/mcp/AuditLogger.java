package org.veronicadavichi.config.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.veronicadavichi.entity.McpAuditLog;
import org.veronicadavichi.mapper.McpAuditLogMapper;

import java.time.LocalDateTime;

/**
 * MCP 审计日志记录器
 * 异步写入 mcp_audit_log 表
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpAuditLogMapper auditLogMapper;

    public AuditLogger(McpAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录 MCP 请求审计日志（含 API Key 别名）
     *
     * @param userToken    用户 token（脱敏）
     * @param requestType  请求类型（tools/call, resources/read, prompts/get）
     * @param targetName   目标名称（Tool 名、Resource URI 或 Prompt 名）
     * @param serverId     后端 server ID
     * @param requestBody  请求体
     * @param responseBody 响应体
     * @param statusCode   响应状态码
     * @param latencyMs    耗时（毫秒）
     * @param isError      是否错误
     * @param errorCode    错误代码
     * @param errorMessage 错误消息
     * @param apiKeyAlias  API Key 别名
     */
    @Async("auditLogExecutor")
    public void log(String userToken, String requestType, String targetName,
                    Long serverId, JsonNode requestBody, String responseBody,
                    Integer statusCode, Long latencyMs, boolean isError,
                    String errorCode, String errorMessage, String apiKeyAlias) {

        log.info(">>> AuditLogger.log called: type={}, target={}, userToken={}, serverId={}, isError={}",
                requestType, targetName, userToken, serverId, isError);
        log.info(">>> AuditLogger: requestBody={}, responseBody={}",
                requestBody != null ? requestBody.toString() : "null",
                responseBody != null ? responseBody : "null");

        try {
            McpAuditLog auditLog = new McpAuditLog();
            auditLog.setUserToken(maskToken(userToken));
            auditLog.setRequestType(requestType);
            auditLog.setTargetName(targetName);
            auditLog.setServerId(serverId);
            auditLog.setRequestBody(requestBody != null ? MAPPER.writeValueAsString(requestBody) : null);
            auditLog.setResponseBody(responseBody != null ? responseBody : null);
            auditLog.setResponseStatus(statusCode);
            auditLog.setIsError(isError);
            auditLog.setErrorCode(errorCode);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setLatencyMs(latencyMs != null ? latencyMs.intValue() : null);
            auditLog.setApiKeyAlias(apiKeyAlias);
            auditLog.setCreatedAt(LocalDateTime.now());

            log.info(">>> AuditLogger: before insert, auditLog={}", auditLog);
            auditLogMapper.insert(auditLog);
            log.info(">>> Audit log inserted successfully: id={}, type={}, target={}",
                    auditLog.getId(), requestType, targetName);

        } catch (Exception e) {
            log.error("Failed to insert audit log: target={}, userToken={}", targetName, userToken, e);
        }
    }

    /**
     * 兼容版日志记录（无 API Key 别名）
     */
    @Async("auditLogExecutor")
    public void log(String userToken, String requestType, String targetName,
                    Long serverId, JsonNode requestBody, String responseBody,
                    Integer statusCode, Long latencyMs, boolean isError,
                    String errorCode, String errorMessage) {

        log(userToken, requestType, targetName, serverId, requestBody, responseBody,
                statusCode, latencyMs, isError, errorCode, errorMessage, null);
    }

    /**
     * 简化版日志记录（无 serverId，无 API Key）
     */
    @Async("auditLogExecutor")
    public void log(String userToken, String requestType, String targetName,
                    JsonNode requestBody, String responseBody, Long latencyMs) {

        log(userToken, requestType, targetName, null, requestBody, responseBody,
                200, latencyMs, false, null, null, null);
    }

    /**
     * 脱敏 user token
     * 保留前 8 位和后 4 位，中间用 *** 替换
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "***" + token.substring(token.length() - 4);
    }
}
