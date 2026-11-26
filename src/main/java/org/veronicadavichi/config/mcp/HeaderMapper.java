package org.veronicadavichi.config.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Header 映射转换器
 * 从 input_schema 中解析 x-param-type=header 标记，将 arguments 映射为 HTTP 请求头
 * 支持新的 x-param-type 扩展字段（向后兼容旧的 x-header）
 */
@Component
public class HeaderMapper {

    private static final Logger log = LoggerFactory.getLogger(HeaderMapper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String X_PARAM_TYPE = "x-param-type";
    private static final String X_HEADER_NAME = "x-header-name";
    private static final String X_HEADER = "x-header"; // 旧版兼容

    /**
     * 从 input_schema 解析 header 映射规则
     * 支持两种格式：
     * 1. 新版：x-param-type: "header" + x-header-name: "X-Custom-Header"
     * 2. 旧版：x-header: "X-Custom-Header"
     *
     * @param inputSchemaJson input_schema JSON 字符串
     * @return header 映射规则：argumentName -> headerName
     */
    public Map<String, String> parseHeaderMappings(String inputSchemaJson) {
        Map<String, String> headerMappings = new HashMap<>();

        if (inputSchemaJson == null || inputSchemaJson.isEmpty()) {
            return headerMappings;
        }

        try {
            JsonNode schema = MAPPER.readTree(inputSchemaJson);
            parseHeaderMappingsFromProperties(schema.get("properties"), headerMappings);

        } catch (Exception e) {
            log.warn("Failed to parse header mappings from schema: {}", e.getMessage());
        }

        return headerMappings;
    }

    /**
     * 递归解析 properties 中的 header 映射
     */
    private void parseHeaderMappingsFromProperties(JsonNode properties, Map<String, String> headerMappings) {
        if (properties == null || !properties.isObject()) {
            return;
        }

        properties.fields().forEachRemaining(entry -> {
            String argName = entry.getKey();
            JsonNode propertyNode = entry.getValue();

            // 新版：检查 x-param-type = "header"
            if (propertyNode.has(X_PARAM_TYPE) && "header".equals(propertyNode.get(X_PARAM_TYPE).asText())) {
                String headerName = propertyNode.has(X_HEADER_NAME)
                    ? propertyNode.get(X_HEADER_NAME).asText()
                    : argName.toUpperCase().replace("_", "-");
                headerMappings.put(argName, headerName);
                log.debug("Header mapping (new): {} -> {}", argName, headerName);
            }
            // 旧版兼容：检查 x-header
            else if (propertyNode.has(X_HEADER)) {
                String headerName = propertyNode.get(X_HEADER).asText();
                if (headerName != null && !headerName.isEmpty()) {
                    headerMappings.put(argName, headerName);
                    log.debug("Header mapping (legacy): {} -> {}", argName, headerName);
                }
            }

            // 递归处理嵌套对象的 properties
            if (propertyNode.has("properties")) {
                parseHeaderMappingsFromProperties(propertyNode.get("properties"), headerMappings);
            }
        });
    }

    /**
     * 将 arguments 转换为 HTTP headers
     *
     * @param arguments      客户端传入的参数
     * @param headerMappings header 映射规则
     * @return HTTP headers
     */
    public Map<String, String> buildHeaders(Map<String, Object> arguments,
                                            Map<String, String> headerMappings) {
        Map<String, String> headers = new HashMap<>();

        if (arguments == null || headerMappings == null || headerMappings.isEmpty()) {
            return headers;
        }

        for (Map.Entry<String, String> mapping : headerMappings.entrySet()) {
            String argName = mapping.getKey();
            String headerName = mapping.getValue();

            if (arguments.containsKey(argName)) {
                Object value = arguments.get(argName);
                if (value != null) {
                    headers.put(headerName, String.valueOf(value));
                    log.debug("Header set: {}={}", headerName, value);
                }
            }
        }

        return headers;
    }

    /**
     * 完整流程：从 input_schema 和 arguments 构建最终 headers
     *
     * @param inputSchemaJson input_schema JSON 字符串
     * @param arguments       客户端参数
     * @return HTTP headers
     */
    public Map<String, String> transformHeaders(String inputSchemaJson,
                                                 Map<String, Object> arguments) {
        Map<String, String> mappings = parseHeaderMappings(inputSchemaJson);
        Map<String, String> headers = buildHeaders(arguments, mappings);

        log.info("Transformed {} arguments to {} headers using {} mappings",
                arguments != null ? arguments.size() : 0,
                headers.size(),
                mappings.size());

        return headers;
    }
}
