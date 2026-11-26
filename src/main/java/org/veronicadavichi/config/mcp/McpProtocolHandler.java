package org.veronicadavichi.config.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.veronicadavichi.entity.McpPrompt;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.entity.McpServer;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.mapper.McpPromptMapper;
import org.veronicadavichi.mapper.McpResourceMapper;
import org.veronicadavichi.mapper.McpServerMapper;
import org.veronicadavichi.mapper.McpToolMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MCP 协议处理器 - 手写 JSON-RPC 2.0 实现
 */
@Component
public class McpProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(McpProtocolHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpToolMapper toolMapper;
    private final McpResourceMapper resourceMapper;
    private final McpPromptMapper promptMapper;
    private final BackendCaller backendCaller;
    private final HeaderMapper headerMapper;
    private final McpServerMapper serverMapper;
    private final AuditLogger auditLogger;

    public McpProtocolHandler(McpToolMapper toolMapper,
                              McpResourceMapper resourceMapper,
                              McpPromptMapper promptMapper,
                              BackendCaller backendCaller,
                              HeaderMapper headerMapper,
                              McpServerMapper serverMapper,
                              AuditLogger auditLogger) {
        this.toolMapper = toolMapper;
        this.resourceMapper = resourceMapper;
        this.promptMapper = promptMapper;
        this.backendCaller = backendCaller;
        this.headerMapper = headerMapper;
        this.serverMapper = serverMapper;
        this.auditLogger = auditLogger;
    }

    /**
     * 处理 JSON-RPC 请求
     */
    public String handleRequest(String jsonRequest, ApiKeyContext apiKeyContext) {
        JsonNode id = null;
        try {
            JsonNode request = MAPPER.readTree(jsonRequest);

            // 验证 JSON-RPC 2.0 基本格式
            if (!request.has("jsonrpc") || !"2.0".equals(request.get("jsonrpc").asText())) {
                return buildError(null, -32600, "Invalid Request: missing or invalid jsonrpc version");
            }

            String method = request.has("method") ? request.get("method").asText() : null;
            JsonNode params = request.has("params") ? request.get("params") : null;
            id = request.has("id") ? request.get("id") : null;

            if (method == null) {
                return buildError(id, -32600, "Invalid Request: missing method");
            }

            log.info("MCP request: method={}, id={}", method, id);

            // 路由到对应处理方法
            String result;
            switch (method) {
                case "initialize":
                    result = handleInitialize(params, id, apiKeyContext);
                    break;
                case "initialized":
                    result = handleInitialized(params, id, apiKeyContext);
                    break;
                case "notifications/initialized":
                    // 通知类型，不需要回复
                    log.info("MCP notifications/initialized received");
                    return null;
                case "ping":
                    result = handlePing(params, id, apiKeyContext);
                    break;
                case "tools/list":
                    result = handleToolsList(params, id, apiKeyContext);
                    break;
                case "tools/call":
                    result = handleToolsCall(params, id, apiKeyContext);
                    break;
                case "resources/list":
                    result = handleResourcesList(params, id, apiKeyContext);
                    break;
                case "resources/read":
                    result = handleResourcesRead(params, id, apiKeyContext);
                    break;
                case "prompts/list":
                    result = handlePromptsList(params, id, apiKeyContext);
                    break;
                case "prompts/get":
                    result = handlePromptsGet(params, id, apiKeyContext);
                    break;
                default:
                    return buildError(id, -32601, "Method not found: " + method);
            }

            return result;

        } catch (JsonProcessingException e) {
            log.error("JSON parse error", e);
            return buildError(null, -32700, "Parse error: " + e.getMessage());
        } catch (McpMethodNotFoundException e) {
            log.warn("MCP method/resource/prompt not found: {}", e.getMessage());
            // id 已经在方法开头解析过了，这里直接使用
            // 根据错误消息判断错误码
            int errorCode = -32000;
            if (e.getMessage().contains("Tool")) {
                errorCode = -32001;
            } else if (e.getMessage().contains("Resource")) {
                errorCode = -32002;
            } else if (e.getMessage().contains("Prompt")) {
                errorCode = -32003;
            }
            return buildError(id, errorCode, e.getMessage());
        } catch (Exception e) {
            log.error("MCP handler error", e);
            return buildError(id, -32000, "Server error: " + e.getMessage());
        }
    }

    private String handleInitialize(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.info("MCP initialize: params={}", params);

        ObjectNode result = MAPPER.createObjectNode();
        result.put("protocolVersion", "2024-11-05");

        ObjectNode serverInfo = MAPPER.createObjectNode();
        serverInfo.put("name", "ai-gateway-mcp");
        serverInfo.put("version", "1.0.0");
        result.set("serverInfo", serverInfo);

        ObjectNode capabilities = MAPPER.createObjectNode();
        ObjectNode toolsCap = MAPPER.createObjectNode();
        toolsCap.put("listChanged", true);
        capabilities.set("tools", toolsCap);

        ObjectNode resourcesCap = MAPPER.createObjectNode();
        resourcesCap.put("subscribe", false);
        resourcesCap.put("listChanged", true);
        capabilities.set("resources", resourcesCap);

        ObjectNode promptsCap = MAPPER.createObjectNode();
        promptsCap.put("listChanged", true);
        capabilities.set("prompts", promptsCap);

        result.set("capabilities", capabilities);

        return buildResponse(id, result);
    }

    private String handleInitialized(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.info("MCP initialized - session ready");
        // notification, no response
        return null;
    }

    private String handlePing(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.debug("MCP ping");
        return buildResponse(id, MAPPER.createObjectNode());
    }

    private String handleToolsList(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.info("MCP tools/list");

        List<McpTool> tools = toolMapper.selectAll();

        // Filter by API Key server scope
        if (ctx != null && ctx.getServerIds() != null) {
            tools = tools.stream()
                    .filter(t -> ctx.hasAccessToServer(t.getServerId()))
                    .toList();
        }

        List<ObjectNode> toolList = new ArrayList<>();

        for (McpTool tool : tools) {
            if (tool.getEnabled() == null || !tool.getEnabled()) {
                continue;
            }

            ObjectNode toolDef = MAPPER.createObjectNode();
            toolDef.put("name", tool.getName());
            if (tool.getDescription() != null) {
                toolDef.put("description", tool.getDescription());
            }

            try {
                ObjectNode inputSchema;
                if (tool.getInputSchema() != null) {
                    inputSchema = (ObjectNode) MAPPER.readTree(tool.getInputSchema());
                } else {
                    inputSchema = MAPPER.createObjectNode();
                    inputSchema.put("type", "object");
                    inputSchema.set("properties", MAPPER.createObjectNode());
                }

                // 动态注入 user_token 参数
                injectUserTokenParam(inputSchema);
                toolDef.set("inputSchema", inputSchema);
            } catch (Exception e) {
                log.warn("Failed to parse inputSchema for tool {}: {}", tool.getName(), e.getMessage());
                ObjectNode emptySchema = MAPPER.createObjectNode();
                emptySchema.put("type", "object");
                emptySchema.set("properties", MAPPER.createObjectNode());
                // 动态注入 user_token 参数
                injectUserTokenParam(emptySchema);
                toolDef.set("inputSchema", emptySchema);
            }

            toolList.add(toolDef);
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.set("tools", MAPPER.valueToTree(toolList));
        return buildResponse(id, result);
    }

    /**
     * 向 input_schema 中注入 user_token 参数
     */
    private void injectUserTokenParam(ObjectNode inputSchema) {
        ObjectNode properties = (ObjectNode) inputSchema.get("properties");
        if (properties == null) {
            properties = MAPPER.createObjectNode();
            inputSchema.set("properties", properties);
        }

        // 添加 user_token 字段定义
        ObjectNode userTokenParam = MAPPER.createObjectNode();
        userTokenParam.put("type", "string");
        userTokenParam.put("description", "统一平台用户认证 token");
        properties.set("user_token", userTokenParam);

        // 添加到 required 数组
        ArrayNode required = (ArrayNode) inputSchema.get("required");
        if (required == null) {
            required = MAPPER.createArrayNode();
            inputSchema.set("required", required);
        }
        // 检查是否已存在，避免重复添加
        boolean exists = false;
        for (JsonNode node : required) {
            if ("user_token".equals(node.asText())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            required.add("user_token");
        }
    }

    private String handleToolsCall(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        if (params == null) {
            return buildError(id, -32602, "Invalid params: missing params object");
        }
        if (!params.has("name")) {
            return buildError(id, -32602, "Invalid params: missing 'name'");
        }
        if (!params.get("name").isTextual()) {
            return buildError(id, -32602, "Invalid params: 'name' must be a string");
        }

        String name = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : MAPPER.createObjectNode();
        if (!arguments.isObject()) {
            return buildError(id, -32602, "Invalid params: 'arguments' must be an object");
        }

        log.info("MCP tools/call: name={}, arguments={}", name, arguments);

        // 提取 user_token 用于审计日志
        String userToken = extractUserTokenForAudit(arguments);

        List<McpTool> tools = toolMapper.selectListByQuery(
                com.mybatisflex.core.query.QueryWrapper.create().eq("name", name)
        );

        if (tools == null || tools.isEmpty()) {
            audit(userToken, "tools/call", name, null,
                    arguments, null, null, null, true, "NOT_FOUND", "Tool not found: " + name, ctx);
            throw new McpMethodNotFoundException("Tool not found: " + name);
        }

        McpTool tool = tools.get(0);
        if (tool.getEnabled() == null || !tool.getEnabled()) {
            audit(userToken, "tools/call", name, tool.getServerId(),
                    arguments, null, null, null, true, "FORBIDDEN", "Tool disabled: " + name, ctx);
            throw new McpMethodNotFoundException("Tool disabled: " + name);
        }

        // Deep check: tool's server must be within key's scope
        if (ctx != null && !ctx.hasAccessToServer(tool.getServerId())) {
            audit(userToken, "tools/call", name, tool.getServerId(),
                    arguments, null, null, null, true, "FORBIDDEN", "Access denied: key does not cover this tool", ctx);
            return buildError(id, -32003, "Access denied: key does not cover this tool");
        }

        // 解析 input_schema 并路由参数
        Map<String, Object> allArgs = convertToJsonMap(arguments);

        // 1. 从 schema 中提取 query params 和 header params
        ParamRouting routing = routeParams(tool.getInputSchema(), allArgs);

        // 2. 获取 header 映射（包含 x-param-type=header 和旧版 x-header）
        Map<String, String> headers = headerMapper.transformHeaders(tool.getInputSchema(), routing.headerParams);

        // 3. 提取 user_token 并添加到 Cookie
        String userTokenValue = extractUserToken(allArgs);
        if (userTokenValue != null && !userTokenValue.isEmpty()) {
            String cookieValue = "token=" + userTokenValue;
            String existingCookie = headers.get("Cookie");
            if (existingCookie != null && !existingCookie.isEmpty()) {
                headers.put("Cookie", existingCookie + "; " + cookieValue);
            } else {
                headers.put("Cookie", cookieValue);
            }
            log.debug("Added Cookie header: {}", headers.get("Cookie"));
        }

        // 4. 调用后端 API（BackendCaller 会根据 method 自动处理 query params 和 body）
        String baseUrl = getBaseUrl(tool);
        String resultText;
        long startTime = System.currentTimeMillis();
        try {
            // 对于 GET 请求，将 query params 合并到 args 中用于构建 URL
            Map<String, Object> urlArgs = new HashMap<>(routing.bodyParams);
            if ("GET".equalsIgnoreCase(tool.getMethod())) {
                urlArgs.putAll(routing.queryParams);
            }
            String responseBody = backendCaller.call(baseUrl, tool.getPath(), tool.getMethod(), urlArgs, headers);
            resultText = responseBody;
            long latencyMs = System.currentTimeMillis() - startTime;
            audit(userToken, "tools/call", name, tool.getServerId(),
                    arguments, responseBody, 200, latencyMs, false, null, null, ctx);
        } catch (BackendCallException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Backend call failed for tool {}: {}", name, e.getMessage());
            resultText = "Error: " + e.getMessage();
            audit(userToken, "tools/call", name, tool.getServerId(),
                    arguments, resultText, 500, latencyMs, true, "BACKEND_ERROR", e.getMessage(), ctx);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Unexpected error for tool {}: {}", name, e.getMessage());
            resultText = "Error: " + e.getMessage();
            audit(userToken, "tools/call", name, tool.getServerId(),
                    arguments, resultText, 500, latencyMs, true, "INTERNAL_ERROR", e.getMessage(), ctx);
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.set("content", MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode()
                        .put("type", "text")
                        .put("text", resultText)));
        return buildResponse(id, result);
    }

    /**
     * 从参数中提取 user_token（不删除，仅用于审计日志）
     * @return user_token 值，如果不存在返回 null
     */
    private String extractUserTokenForAudit(JsonNode arguments) {
        if (arguments == null || !arguments.has("user_token")) {
            return null;
        }
        return arguments.get("user_token").asText();
    }

    /**
     * 从参数中提取 user_token，并从 argsMap 中移除（避免被添加到 URL 参数）
     * @return user_token 值，如果不存在返回 null
     */
    private String extractUserToken(Map<String, Object> argsMap) {
        return (String) argsMap.remove("user_token");
    }

    /**
     * 参数路由：根据 schema 中的 x-param-type 将参数分离到不同位置
     * @param schemaJson input_schema JSON 字符串
     * @param allArgs 所有参数
     * @return 路由后的参数分组
     */
    private ParamRouting routeParams(String schemaJson, Map<String, Object> allArgs) {
        ParamRouting routing = new ParamRouting();

        if (schemaJson == null || schemaJson.isEmpty() || allArgs == null) {
            routing.bodyParams = allArgs != null ? allArgs : new HashMap<>();
            return routing;
        }

        try {
            JsonNode schema = MAPPER.readTree(schemaJson);
            JsonNode properties = schema.get("properties");

            if (properties == null || !properties.isObject()) {
                routing.bodyParams = allArgs;
                return routing;
            }

            routeParamsRecursive(properties, allArgs, routing);

        } catch (Exception e) {
            log.warn("Failed to route params: {}", e.getMessage());
            routing.bodyParams = allArgs;
        }

        return routing;
    }

    /**
     * 递归路由参数
     */
    private void routeParamsRecursive(JsonNode properties, Map<String, Object> allArgs,
                                       ParamRouting routing) {
        if (properties == null || !properties.isObject()) {
            return;
        }

        properties.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldSchema = entry.getValue();
            JsonNode paramTypeNode = fieldSchema.get("x-param-type");

            if (!allArgs.containsKey(fieldName)) {
                return;
            }

            Object value = allArgs.get(fieldName);

            // 根据 x-param-type 路由
            if (paramTypeNode != null) {
                String paramType = paramTypeNode.asText();
                if ("query".equals(paramType)) {
                    routing.queryParams.put(fieldName, value);
                    log.debug("Route to query: {}={}", fieldName, value);
                } else if ("header".equals(paramType)) {
                    routing.headerParams.put(fieldName, value);
                    log.debug("Route to header: {}={}", fieldName, value);
                } else {
                    // json 或未知类型，放入 body
                    routing.bodyParams.put(fieldName, value);
                }
            } else {
                // 没有 x-param-type，默认放入 body
                routing.bodyParams.put(fieldName, value);
            }

            // 递归处理嵌套对象
            if (fieldSchema.has("properties") && value instanceof Map) {
                routeParamsRecursive(fieldSchema.get("properties"),
                        (Map<String, Object>) value, routing);
            }
        });
    }

    /**
     * 参数路由结果
     */
    private static class ParamRouting {
        Map<String, Object> queryParams = new HashMap<>();
        Map<String, Object> headerParams = new HashMap<>();
        Map<String, Object> bodyParams = new HashMap<>();
    }

    private String handleResourcesList(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.info("MCP resources/list");

        List<McpResource> resources = resourceMapper.selectAll();

        // Filter by API Key server scope
        if (ctx != null && ctx.getServerIds() != null) {
            resources = resources.stream()
                    .filter(r -> ctx.hasAccessToServer(r.getServerId()))
                    .toList();
        }

        List<ObjectNode> resourceList = new ArrayList<>();

        for (McpResource resource : resources) {
            if (resource.getEnabled() == null || !resource.getEnabled()) {
                continue;
            }

            ObjectNode resourceDef = MAPPER.createObjectNode();
            resourceDef.put("uri", resource.getUri());
            if (resource.getName() != null) {
                resourceDef.put("name", resource.getName());
            }
            if (resource.getDescription() != null) {
                resourceDef.put("description", resource.getDescription());
            }
            if (resource.getMimeType() != null) {
                resourceDef.put("mimeType", resource.getMimeType());
            }

            resourceList.add(resourceDef);
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.set("resources", MAPPER.valueToTree(resourceList));
        return buildResponse(id, result);
    }

    private String handleResourcesRead(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        if (params == null) {
            return buildError(id, -32602, "Invalid params: missing params object");
        }
        if (!params.has("uri")) {
            return buildError(id, -32602, "Invalid params: missing 'uri'");
        }
        if (!params.get("uri").isTextual()) {
            return buildError(id, -32602, "Invalid params: 'uri' must be a string");
        }

        String uri = params.get("uri").asText();
        log.info("MCP resources/read: uri={}", uri);

        // 提取 user_token 用于审计日志
        String userToken = extractUserTokenForAudit(params);

        List<McpResource> resources = resourceMapper.selectListByQuery(
                com.mybatisflex.core.query.QueryWrapper.create().eq("uri", uri)
        );

        if (resources == null || resources.isEmpty()) {
            audit(userToken, "resources/read", uri, null, params, null, null, null, true, "NOT_FOUND", "Resource not found: " + uri, ctx);
            throw new McpMethodNotFoundException("Resource not found: " + uri);
        }

        McpResource resource = resources.get(0);
        if (resource.getEnabled() == null || !resource.getEnabled()) {
            audit(userToken, "resources/read", uri, resource.getServerId(), params, null, null, null, true, "FORBIDDEN", "Resource disabled: " + uri, ctx);
            throw new McpMethodNotFoundException("Resource disabled: " + uri);
        }

        // Deep check: resource's server must be within key's scope
        if (ctx != null && !ctx.hasAccessToServer(resource.getServerId())) {
            audit(userToken, "resources/read", uri, resource.getServerId(),
                    params, null, null, null, true, "FORBIDDEN", "Access denied: key does not cover this resource", ctx);
            return buildError(id, -32003, "Access denied: key does not cover this resource");
        }

        // 处理 URI template 参数替换
        String path = resource.getPath();
        if (resource.getTemplate() != null && !resource.getTemplate().isEmpty()) {
            path = applyTemplate(path, params);
        }

        // 调用后端 API 读取资源
        String baseUrl = getBaseUrl(resource);
        String content;
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> argsMap = convertToJsonMap(params);
            Map<String, String> headers = headerMapper.transformHeaders(resource.getInputSchema(), argsMap);

            // 提取 user_token 并添加到 Cookie
            String userTokenValue = extractUserToken(argsMap);
            if (userTokenValue != null && !userTokenValue.isEmpty()) {
                String cookieValue = "token=" + userTokenValue;
                String existingCookie = headers.get("Cookie");
                if (existingCookie != null && !existingCookie.isEmpty()) {
                    headers.put("Cookie", existingCookie + "; " + cookieValue);
                } else {
                    headers.put("Cookie", cookieValue);
                }
                log.debug("Added Cookie header: {}", headers.get("Cookie"));
            }

            content = backendCaller.call(baseUrl, path, resource.getMethod(), argsMap, headers);
            long latencyMs = System.currentTimeMillis() - startTime;
            audit(userToken, "resources/read", uri, resource.getServerId(), params, content, 200, (long) latencyMs, false, null, null, ctx);
        } catch (BackendCallException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Backend call failed for resource {}: {}", uri, e.getMessage());
            content = "Error: " + e.getMessage();
            audit(userToken, "resources/read", uri, resource.getServerId(), params, content, 500, (long) latencyMs, true, "BACKEND_ERROR", e.getMessage(), ctx);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Unexpected error for resource {}: {}", uri, e.getMessage());
            content = "Error: " + e.getMessage();
            audit(userToken, "resources/read", uri, resource.getServerId(), params, content, 500, (long) latencyMs, true, "INTERNAL_ERROR", e.getMessage(), ctx);
        }

        ObjectNode contentNode = MAPPER.createObjectNode();
        contentNode.put("uri", uri);
        contentNode.put("mimeType", resource.getMimeType() != null ? resource.getMimeType() : "text/plain");
        contentNode.put("text", content);

        ObjectNode result = MAPPER.createObjectNode();
        result.set("contents", MAPPER.createArrayNode().add(contentNode));
        return buildResponse(id, result);
    }

    private String handlePromptsList(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        log.info("MCP prompts/list");

        List<McpPrompt> prompts = promptMapper.selectAll();

        // Filter by API Key server scope
        if (ctx != null && ctx.getServerIds() != null) {
            prompts = prompts.stream()
                    .filter(p -> ctx.hasAccessToServer(p.getServerId()))
                    .toList();
        }

        List<ObjectNode> promptList = new ArrayList<>();

        for (McpPrompt prompt : prompts) {
            if (prompt.getEnabled() == null || !prompt.getEnabled()) {
                continue;
            }

            ObjectNode promptDef = MAPPER.createObjectNode();
            promptDef.put("name", prompt.getName());
            if (prompt.getDescription() != null) {
                promptDef.put("description", prompt.getDescription());
            }

            try {
                if (prompt.getArguments() != null) {
                    promptDef.set("arguments", MAPPER.readTree(prompt.getArguments()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse arguments for prompt {}: {}", prompt.getName(), e.getMessage());
            }

            promptList.add(promptDef);
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.set("prompts", MAPPER.valueToTree(promptList));
        return buildResponse(id, result);
    }

    private String handlePromptsGet(JsonNode params, JsonNode id, ApiKeyContext ctx) {
        if (params == null) {
            return buildError(id, -32602, "Invalid params: missing params object");
        }
        if (!params.has("name")) {
            return buildError(id, -32602, "Invalid params: missing 'name'");
        }
        if (!params.get("name").isTextual()) {
            return buildError(id, -32602, "Invalid params: 'name' must be a string");
        }

        String name = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : MAPPER.createObjectNode();
        if (arguments != null && !arguments.isObject()) {
            return buildError(id, -32602, "Invalid params: 'arguments' must be an object");
        }

        log.info("MCP prompts/get: name={}, arguments={}", name, arguments);

        // 提取 user_token 用于审计日志
        String userToken = extractUserTokenForAudit(params);

        List<McpPrompt> prompts = promptMapper.selectListByQuery(
                com.mybatisflex.core.query.QueryWrapper.create().eq("name", name)
        );

        if (prompts == null || prompts.isEmpty()) {
            audit(userToken, "prompts/get", name, null, params, null, null, null, true, "NOT_FOUND", "Prompt not found: " + name, ctx);
            throw new McpMethodNotFoundException("Prompt not found: " + name);
        }

        McpPrompt prompt = prompts.get(0);
        if (prompt.getEnabled() == null || !prompt.getEnabled()) {
            audit(userToken, "prompts/get", name, prompt.getServerId(), params, null, null, null, true, "FORBIDDEN", "Prompt disabled: " + name, ctx);
            throw new McpMethodNotFoundException("Prompt disabled: " + name);
        }

        // Deep check: prompt's server must be within key's scope
        if (ctx != null && !ctx.hasAccessToServer(prompt.getServerId())) {
            audit(userToken, "prompts/get", name, prompt.getServerId(),
                    params, null, null, null, true, "FORBIDDEN", "Access denied: key does not cover this prompt", ctx);
            return buildError(id, -32003, "Access denied: key does not cover this prompt");
        }

        // 应用 arguments 到 template
        String template = applyArgumentsToTemplate(prompt.getTemplate(), arguments);

        // 记录审计日志（prompts/get 不涉及后端调用，直接记录成功）
        audit(userToken, "prompts/get", name, prompt.getServerId(), params, template, 200, 0L, false, null, null, ctx);

        ObjectNode result = MAPPER.createObjectNode();
        if (prompt.getDescription() != null) {
            result.put("description", prompt.getDescription());
        }

        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        ObjectNode content = MAPPER.createObjectNode();
        content.put("type", "text");
        content.put("text", template);
        message.set("content", content);

        result.set("messages", MAPPER.createArrayNode().add(message));
        return buildResponse(id, result);
    }

    /**
     * 构建成功响应
     * JSON-RPC 2.0 规范要求所有响应都必须包含 id 字段（即使为 null）
     */
    private String buildResponse(JsonNode id, JsonNode result) {
        try {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            // 必须设置 id，即使为 null，否则 Pydantic 校验会失败
            response.set("id", id != null ? id : MAPPER.nullNode());
            response.set("result", result);
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to build response", e);
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32000,\"message\":\"Internal error\"}}";
        }
    }

    /**
     * 构建错误响应
     * JSON-RPC 2.0 规范要求所有响应都必须包含 id 字段（即使为 null）
     */
    private String buildError(JsonNode id, int code, String message) {
        try {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            // 必须设置 id，即使为 null，否则 Pydantic 校验会失败
            response.set("id", id != null ? id : MAPPER.nullNode());
            ObjectNode error = MAPPER.createObjectNode();
            error.put("code", code);
            error.put("message", message);
            response.set("error", error);
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to build error response", e);
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32000,\"message\":\"Internal error\"}}";
        }
    }

    /**
     * 从 McpTool 获取 baseUrl（使用 server 级别配置）
     */
    private String getBaseUrl(McpTool tool) {
        if (tool.getServerId() != null) {
            McpServer server = serverMapper.selectOneById(tool.getServerId());
            if (server != null && server.getBaseUrl() != null && !server.getBaseUrl().isEmpty()) {
                return server.getBaseUrl();
            }
        }
        return System.getProperty("mcp.backend.baseUrl", "http://localhost:8080");
    }

    /**
     * 从 McpResource 获取 baseUrl（使用 server 级别配置）
     */
    private String getBaseUrl(McpResource resource) {
        if (resource.getServerId() != null) {
            McpServer server = serverMapper.selectOneById(resource.getServerId());
            if (server != null && server.getBaseUrl() != null && !server.getBaseUrl().isEmpty()) {
                return server.getBaseUrl();
            }
        }
        return System.getProperty("mcp.backend.baseUrl", "http://localhost:8080");
    }

    /**
     * 将 JsonNode 转换为 Map
     */
    private Map<String, Object> convertToJsonMap(JsonNode node) {
        if (node == null) {
            return new HashMap<>();
        }
        return MAPPER.convertValue(node, Map.class);
    }

    /**
     * 应用 template 参数替换
     * 支持 {paramName} 格式的占位符
     */
    private String applyTemplate(String template, JsonNode params) {
        if (template == null || params == null) {
            return template;
        }

        StringBuilder result = new StringBuilder(template);
        params.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().asText();
            int pos = 0;
            while ((pos = result.indexOf("{" + key + "}", pos)) != -1) {
                result.replace(pos, pos + key.length() + 2, value);
                pos += value.length();
            }
        });
        return result.toString();
    }

    /**
     * 应用 arguments 到 prompt template
     * 支持 {paramName} 格式的占位符
     */
    private String applyArgumentsToTemplate(String template, JsonNode arguments) {
        if (template == null || arguments == null) {
            return template;
        }

        StringBuilder result = new StringBuilder(template);
        arguments.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().asText();
            int pos = 0;
            while ((pos = result.indexOf("{" + key + "}", pos)) != -1) {
                result.replace(pos, pos + key.length() + 2, value);
                pos += value.length();
            }
        });
        return result.toString();
    }

    /**
     * 审计日志封装 — 自动附加 API Key 别名
     */
    private void audit(String userToken, String requestType, String targetName,
                       Long serverId, JsonNode requestBody, String responseBody,
                       Integer statusCode, Long latencyMs, boolean isError,
                       String errorCode, String errorMessage, ApiKeyContext ctx) {
        auditLogger.log(userToken, requestType, targetName, serverId, requestBody, responseBody,
                statusCode, latencyMs, isError, errorCode, errorMessage,
                ctx != null ? ctx.getAlias() : null);
    }
}
