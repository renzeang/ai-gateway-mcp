package org.veronicadavichi.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 审计日志实体
 */
@Data
@Table("mcp_audit_log")
public class McpAuditLog {

    @Id
    private Long id;

    @Column("user_token")
    private String userToken;

    @Column("request_type")
    private String requestType;

    @Column("target_name")
    private String targetName;

    @Column("server_id")
    private Long serverId;

    @Column("request_body")
    private String requestBody;  // TEXT 类型，存储 JSON 或纯文本

    @Column("response_status")
    private Integer responseStatus;

    @Column("response_body")
    private String responseBody;  // TEXT 类型，存储 JSON 或纯文本

    @Column("is_error")
    private Boolean isError;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @Column("latency_ms")
    private Integer latencyMs;

    @Column("api_key_alias")
    private String apiKeyAlias;

    @Column("created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserToken() { return userToken; }
    public void setUserToken(String userToken) { this.userToken = userToken; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public Boolean getIsError() { return isError; }
    public void setIsError(Boolean isError) { this.isError = isError; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getApiKeyAlias() { return apiKeyAlias; }
    public void setApiKeyAlias(String apiKeyAlias) { this.apiKeyAlias = apiKeyAlias; }
}
