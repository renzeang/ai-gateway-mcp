package org.veronicadavichi.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 后端 API 调用器
 * 使用 Java 17 HttpClient 调用后端服务
 */
@Component
public class BackendCaller {

    private static final Logger log = LoggerFactory.getLogger(BackendCaller.class);

    private final HttpClient httpClient;

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public BackendCaller() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 调用后端 API
     *
     * @param baseUrl   后端服务基础 URL
     * @param path      API 路径
     * @param method    HTTP 方法（GET, POST, PUT, DELETE 等）
     * @param args      请求参数（用于构建 query string 或 request body）
     * @param headers   请求头映射
     * @return HTTP 响应体
     */
    public String call(String baseUrl, String path, String method,
                       Map<String, Object> args, Map<String, String> headers) {

        String url = buildUrl(baseUrl, path, args, method);

        log.info("Calling backend: {} {} with {} args, {} headers",
                method, url, args != null ? args.size() : 0, headers != null ? headers.size() : 0);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

        // 添加自定义 headers
        if (headers != null) {
            headers.forEach((key, value) -> {
                // Cookie header 需要特殊处理，使用 setHeader 而不是 header
                if ("Cookie".equalsIgnoreCase(key)) {
                    requestBuilder.setHeader("Cookie", value);
                    log.debug("Setting Cookie header: {}", value);
                } else {
                    requestBuilder.header(key, value);
                }
            });
        }

        // 设置请求方法和 body（如果需要）
        if ("GET".equalsIgnoreCase(method)) {
            requestBuilder.GET();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            requestBuilder.DELETE();
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) ||
                   "PATCH".equalsIgnoreCase(method)) {
            String body = buildBody(args);
            if (body != null && !body.isEmpty()) {
                requestBuilder.header("Content-Type", "application/json");
                if ("POST".equalsIgnoreCase(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
                }
            } else {
                if ("POST".equalsIgnoreCase(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                } else if ("PUT".equalsIgnoreCase(method)) {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            }
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest request = requestBuilder.build();

        try {
            log.debug("Sending request: method={}, url={}, headers={}", method, url,
                    request.headers().map().keySet());

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            log.info("Backend response: status={}, bodyLength={}",
                    statusCode, responseBody.length());

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                // 返回友好的错误消息，不暴露原始堆栈
                String errorMessage = String.format("Backend returned %d: %s", statusCode,
                        truncate(responseBody, 200));
                log.error("Backend call failed: url={}, status={}, body={}", url, statusCode, errorMessage);
                throw new BackendCallException(errorMessage);
            }

        } catch (IOException e) {
            log.error("IO error calling backend: {} - {}", url, e.getMessage());
            throw new BackendCallException("Network error: unable to connect to backend service", e);
        } catch (InterruptedException e) {
            log.warn("Interrupted calling backend: {}", url);
            Thread.currentThread().interrupt();
            throw new BackendCallException("Request interrupted", e);
        }
    }

    /**
     * 截断字符串到指定长度（用于日志记录）
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 构建 URL（包含 query string）
     */
    private String buildUrl(String baseUrl, String path, Map<String, Object> args, String method) {
        // 对于 GET 请求，将参数添加到 query string
        if ("GET".equalsIgnoreCase(method) && args != null && !args.isEmpty()) {
            StringBuilder url = new StringBuilder(baseUrl);

            // 确保 baseUrl 以/结尾，path 不以/开头
            if (!url.toString().endsWith("/")) {
                url.append("/");
            }
            if (path.startsWith("/")) {
                url.append(path.substring(1));
            } else {
                url.append(path);
            }

            // 添加 query string
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                url.append(encode(entry.getKey()))
                   .append("=")
                   .append(encode(String.valueOf(entry.getValue())));
                first = false;
            }
            return url.toString();
        } else {
            // 非 GET 请求或无参数，直接拼接路径
            if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
                return baseUrl + "/" + path;
            } else if (baseUrl.endsWith("/") && path.startsWith("/")) {
                return baseUrl + path.substring(1);
            } else {
                return baseUrl + path;
            }
        }
    }

    /**
     * ObjectMapper for JSON serialization
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 构建请求体（JSON 格式）
     * 使用 Jackson 库构建正确的 JSON，避免手动拼接导致的转义问题
     */
    private String buildBody(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(args);
        } catch (Exception e) {
            log.error("Failed to serialize args to JSON", e);
            return null;
        }
    }

    /**
     * URL 编码（使用 Java 标准库 URLEncoder）
     * 支持中文、特殊字符的完整编码
     */
    private String encode(String value) {
        if (value == null) {
            return "";
        }
        // 使用 URLEncoder.encode 进行标准编码，然后将 '+' 替换为 '%20'
        // 因为 URLEncoder 会将空格编码为 '+'，但某些服务器不识别 '+' 作为空格
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}

/**
 * 后端调用异常
 */
class BackendCallException extends RuntimeException {
    public BackendCallException(String message) {
        super(message);
    }

    public BackendCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
