package org.veronicadavichi.config.mcp;

/**
 * MCP 方法未找到异常
 */
public class McpMethodNotFoundException extends RuntimeException {

    public McpMethodNotFoundException(String message) {
        super(message);
    }

    public McpMethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
