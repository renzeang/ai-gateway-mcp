package org.veronicadavichi.config.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * stdio 传输层处理器
 * 从 System.in 读取 JSON-RPC 请求，向 System.out 写入响应
 * 实现 CommandLineRunner 接口，应用启动后自动运行
 */
@Component
public class StdioHandler implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StdioHandler.class);

    private final McpProtocolHandler protocolHandler;

    /**
     * 标记是否启用 stdio 模式
     * 通过系统属性 stdio.enabled 控制
     */
    private final boolean stdioEnabled;

    public StdioHandler(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        this.stdioEnabled = Boolean.parseBoolean(
            System.getProperty("stdio.enabled", "false")
        );

        // stdio 模式下禁用 web 服务
        if (stdioEnabled) {
            System.setProperty("spring.main.web-application-type", "none");
        }
    }

    @Override
    public void run(String... args) throws Exception {
        if (!stdioEnabled) {
            log.info("stdio mode disabled. Set -Dstdio.enabled=true to enable.");
            return;
        }

        log.info("stdio mode started. Waiting for JSON-RPC requests on stdin...");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 空行跳过
                if (line.trim().isEmpty()) {
                    continue;
                }

                log.debug("Received: {}", line);

                try {
                    // 处理 JSON-RPC 请求
                    String response = protocolHandler.handleRequest(line, null);

                    if (response != null) {
                        // 写入响应到 stdout
                        System.out.println(response);
                        System.out.flush();
                        log.debug("Sent: {}", response);
                    }
                } catch (Exception e) {
                    log.error("Error processing request: {}", line, e);
                    // 发送错误响应
                    String errorResponse = protocolHandler.handleRequest(
                        "{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"error\"}", null
                    );
                    if (errorResponse != null) {
                        System.out.println(errorResponse);
                        System.out.flush();
                    }
                }
            }
        } catch (IOException e) {
            log.error("stdin read error", e);
        }

        log.info("stdio mode ended.");
    }
}
