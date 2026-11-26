package org.veronicadavichi.exception;

/**
 * API Key 验证失败异常
 */
public class McpApiKeyValidationException extends RuntimeException {

    private final String reason;

    public McpApiKeyValidationException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
