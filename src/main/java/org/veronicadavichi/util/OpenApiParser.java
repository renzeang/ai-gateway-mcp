package org.veronicadavichi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.entity.McpTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 解析器
 * 支持 OpenAPI 3.0/3.1、Swagger 2.0 与 McpTool/McpResource 之间的双向转换
 * 支持 JSON 和 YAML 格式
 */
public class OpenApiParser {

    static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 解析 OpenAPI/Swagger 为 Tools 列表
     * 自动分类：非 GET 方法或 GET 但有请求体 → Tools
     * 支持 OpenAPI 3.0/3.1、Swagger 2.0、JSON/YAML 格式
     */
    public static List<McpTool> parseToTools(String openApiContent) {
        List<McpTool> tools = new ArrayList<>();

        try {
            // 解析内容（自动检测 JSON/YAML 和 Swagger 2.0/OpenAPI 3.x）
            SwaggerParseResult result = parseContent(openApiContent);
            if (result == null) {
                return tools;
            }

            // user_token 校验
            validateUserToken(result.getOpenAPI());

            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                // 记录警告但不中断
                System.out.println("OpenAPI 解析警告：" + result.getMessages());
            }

            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null || openAPI.getPaths() == null) {
                return tools;
            }

            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                // 检查每个 HTTP 方法
                Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
                for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : operations.entrySet()) {
                    PathItem.HttpMethod method = opEntry.getKey();
                    Operation operation = opEntry.getValue();

                    // 分类规则：GET + (无参或只有路径参数) → Resources，其他 → Tools
                    if (isResourceMethod(method, operation)) {
                        continue; // 跳过，这是 Resource
                    }

                    McpTool tool = new McpTool();
                    tool.setName(extractOperationId(operation, path, method));
                    tool.setDescription(operation.getSummary() != null ? operation.getSummary() : operation.getDescription());
                    tool.setPath(path);
                    tool.setMethod(method.name().toUpperCase());
                    tool.setInputSchema(buildInputSchema(operation));
                    tool.setEnabled(true);

                    tools.add(tool);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 OpenAPI 失败：" + e.getMessage(), e);
        }

        return tools;
    }

    /**
     * 解析 OpenAPI/Swagger 为 Resources 列表
     * 自动分类：GET + (无参或只有路径参数) → Resources
     * 支持 OpenAPI 3.0/3.1、Swagger 2.0、JSON/YAML 格式
     */
    public static List<McpResource> parseToResources(String openApiContent) {
        List<McpResource> resources = new ArrayList<>();

        try {
            // 解析内容（自动检测 JSON/YAML 和 Swagger 2.0/OpenAPI 3.x）
            SwaggerParseResult result = parseContent(openApiContent);
            if (result == null) {
                return resources;
            }

            // user_token 校验
            validateUserToken(result.getOpenAPI());

            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null || openAPI.getPaths() == null) {
                return resources;
            }

            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                Operation getOperation = pathItem.getGet();
                if (getOperation != null && isResourceMethod(PathItem.HttpMethod.GET, getOperation)) {
                    McpResource resource = new McpResource();
                    resource.setUri(extractOperationId(getOperation, path, PathItem.HttpMethod.GET));
                    resource.setName(getOperation.getSummary() != null ? getOperation.getSummary() : getOperation.getDescription());
                    resource.setDescription(getOperation.getDescription());
                    resource.setPath(path);
                    resource.setMethod("GET");
                    resource.setTemplate(buildTemplate(getOperation));
                    resource.setEnabled(true);

                    resources.add(resource);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 OpenAPI 失败：" + e.getMessage(), e);
        }

        return resources;
    }

    /**
     * 从 Tools 生成 OpenAPI JSON（带 x-mcp-* 扩展）
     *
     * @param tools      Tools 列表
     * @param serverId   Server ID（可选）
     * @param serverName Server 名称（可选）
     * @return OpenAPI JSON 字符串
     */
    public static String generateFromTools(List<McpTool> tools, Integer serverId, String serverName) {
        return generateOpenApi(tools, null, serverId, serverName);
    }

    /**
     * 从 Resources 生成 OpenAPI JSON（带 x-mcp-* 扩展）
     *
     * @param resources  Resources 列表
     * @param serverId   Server ID（可选）
     * @param serverName Server 名称（可选）
     * @return OpenAPI JSON 字符串
     */
    public static String generateFromResources(List<McpResource> resources, Integer serverId, String serverName) {
        return generateOpenApi(null, resources, serverId, serverName);
    }


    /**
     * 从 Tools + Resources 生成统一的 OpenAPI JSON（带 x-mcp-* 扩展）
     *
     * @param tools      Tools 列表（可选）
     * @param resources  Resources 列表（可选）
     * @param serverId   Server ID（可选）
     * @param serverName Server 名称（可选）
     * @return OpenAPI JSON 字符串
     */
    public static String generateOpenApi(List<McpTool> tools, List<McpResource> resources,
                                         Integer serverId, String serverName) {
        try {
            ObjectNode root = JSON_MAPPER.createObjectNode();
            root.put("openapi", "3.0.0");

            // Info
            ObjectNode info = root.putObject("info");
            String title = serverName != null ? "MCP Server Export - " + serverName : "MCP Server Export";
            info.put("title", title);
            info.put("version", "1.0.0");
            info.put("description", "Auto-generated from MCP Server");

            // x-mcp-* 扩展字段
            if (serverId != null) {
                info.put("x-mcp-server-id", serverId);
            }
            if (serverName != null) {
                info.put("x-mcp-server-name", serverName);
            }
            info.put("x-mcp-export-timestamp", java.time.Instant.now().toString());

            // Paths
            ObjectNode paths = root.putObject("paths");

            // 添加 Tools
            if (tools != null) {
                for (McpTool tool : tools) {
                    if (tool.getPath() == null || tool.getMethod() == null) {
                        continue;
                    }

                    ObjectNode pathItem = paths.putObject(tool.getPath());
                    ObjectNode operation = pathItem.putObject(tool.getMethod().toLowerCase());

                    // Operation 基本信息
                    operation.put("operationId", tool.getName());
                    if (tool.getDescription() != null) {
                        operation.put("summary", tool.getDescription());
                        operation.put("description", tool.getDescription());
                    }

                    // x-mcp-type 扩展
                    operation.put("x-mcp-type", "tool");
                    if (tool.getEnabled() != null) {
                        operation.put("x-mcp-enabled", tool.getEnabled());
                    }

                    // Request Body (非 GET 方法)
                    if (!"GET".equalsIgnoreCase(tool.getMethod())) {
                        JsonNode inputSchema = tool.getInputSchema() != null ?
                                JSON_MAPPER.readTree(tool.getInputSchema()) : null;
                        if (inputSchema != null && inputSchema.isObject()) {
                            ObjectNode requestBody = operation.putObject("requestBody");
                            requestBody.put("required", true);

                            ObjectNode content = requestBody.putObject("content");
                            ObjectNode jsonContent = content.putObject("application/json");
                            jsonContent.set("schema", inputSchema);
                        }
                    }

                    // Responses
                    ObjectNode responses = operation.putObject("responses");
                    ObjectNode successResponse = responses.putObject("200");
                    successResponse.put("description", "Successful response");
                }
            }

            // 添加 Resources
            if (resources != null) {
                for (McpResource resource : resources) {
                    if (resource.getPath() == null) {
                        continue;
                    }

                    ObjectNode pathItem = paths.putObject(resource.getPath());
                    ObjectNode getOperation = pathItem.putObject("get");

                    // Operation 基本信息
                    getOperation.put("operationId", resource.getUri());
                    if (resource.getName() != null) {
                        getOperation.put("summary", resource.getName());
                    }
                    if (resource.getDescription() != null) {
                        getOperation.put("description", resource.getDescription());
                    }

                    // x-mcp-type 扩展
                    getOperation.put("x-mcp-type", "resource");
                    if (resource.getEnabled() != null) {
                        getOperation.put("x-mcp-enabled", resource.getEnabled());
                    }

                    // Parameters (从 template 中提取)
                    if (resource.getTemplate() != null) {
                        JsonNode template = JSON_MAPPER.readTree(resource.getTemplate());
                        if (template.isObject()) {
                            ArrayNode parameters = getOperation.putArray("parameters");

                            Iterator<Map.Entry<String, JsonNode>> fields = template.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> field = fields.next();
                                ObjectNode param = parameters.addObject();
                                param.put("name", field.getKey());
                                param.put("in", "query");
                                param.put("required", false);

                                String type = field.getValue().isTextual() ? field.getValue().asText() : "string";
                                ObjectNode schema = param.putObject("schema");
                                schema.put("type", type);
                            }
                        }
                    }

                    // Responses
                    ObjectNode responses = getOperation.putObject("responses");
                    ObjectNode successResponse = responses.putObject("200");
                    successResponse.put("description", "Successful response");
                }
            }

            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("生成 OpenAPI 失败：" + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统一解析内容（自动检测 JSON/YAML 和 Swagger 2.0/OpenAPI 3.x）
     */
    private static SwaggerParseResult parseContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试解析为 OpenAPI 3.0/3.1
            ParseOptions options = createParseOptions();
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);

            // 如果 OpenAPI 3.x 解析失败，尝试 Swagger 2.0
            if (result == null || result.getOpenAPI() == null) {
                try {
                    // 使用 Swagger 1.x 解析器解析 Swagger 2.0
                    Swagger swagger = new SwaggerParser().parse(content);
                    if (swagger != null) {
                        // 手动转换为 OpenAPI 3.0 模型
                        OpenAPI openAPI = convertSwagger2ToOpenAPI3(swagger);
                        result = new SwaggerParseResult();
                        result.setOpenAPI(openAPI);
                    }
                } catch (Exception e) {
                    // Swagger 2.0 解析失败，尝试作为 YAML 解析
                    try {
                        JsonNode yamlNode = YAML_MAPPER.readTree(content);
                        String jsonContent = JSON_MAPPER.writeValueAsString(yamlNode);
                        result = new OpenAPIV3Parser().readContents(jsonContent, null, options);

                        // 如果还是失败，再尝试作为 Swagger 2.0 YAML 解析
                        if (result == null || result.getOpenAPI() == null) {
                            Swagger swagger = new SwaggerParser().parse(content);
                            if (swagger != null) {
                                OpenAPI openAPI = convertSwagger2ToOpenAPI3(swagger);
                                result = new SwaggerParseResult();
                                result.setOpenAPI(openAPI);
                            }
                        }
                    } catch (Exception yamlEx) {
                        System.out.println("YAML/Swagger 解析失败：" + yamlEx.getMessage());
                    }
                }
            }

            return result;
        } catch (Exception e) {
            System.out.println("解析失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 手动转换 Swagger 2.0 到 OpenAPI 3.0
     */
    private static OpenAPI convertSwagger2ToOpenAPI3(Swagger swagger) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.0.0");

        // Info
        if (swagger.getInfo() != null) {
            io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();
            info.setTitle(swagger.getInfo().getTitle());
            info.setVersion(swagger.getInfo().getVersion());
            info.setDescription(swagger.getInfo().getDescription());
            openAPI.setInfo(info);
        }

        // Paths
        if (swagger.getPaths() != null) {
            io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
            for (Map.Entry<String, Path> entry : swagger.getPaths().entrySet()) {
                Path swaggerPath = entry.getValue();
                PathItem pathItem = new PathItem();

                // GET
                if (swaggerPath.getGet() != null) {
                    pathItem.get(convertOperation(swaggerPath.getGet()));
                }
                // POST
                if (swaggerPath.getPost() != null) {
                    pathItem.post(convertOperation(swaggerPath.getPost()));
                }
                // PUT
                if (swaggerPath.getPut() != null) {
                    pathItem.put(convertOperation(swaggerPath.getPut()));
                }
                // DELETE
                if (swaggerPath.getDelete() != null) {
                    pathItem.delete(convertOperation(swaggerPath.getDelete()));
                }
                // PATCH
                if (swaggerPath.getPatch() != null) {
                    pathItem.patch(convertOperation(swaggerPath.getPatch()));
                }

                paths.addPathItem(entry.getKey(), pathItem);
            }
            openAPI.setPaths(paths);
        }

        return openAPI;
    }

    /**
     * 转换 Operation（Swagger 2.0 → OpenAPI 3.0）
     */
    private static io.swagger.v3.oas.models.Operation convertOperation(io.swagger.models.Operation swaggerOp) {
        io.swagger.v3.oas.models.Operation operation = new io.swagger.v3.oas.models.Operation();
        operation.setOperationId(swaggerOp.getOperationId());
        operation.setSummary(swaggerOp.getSummary());
        operation.setDescription(swaggerOp.getDescription());

        // 转换响应
        if (swaggerOp.getResponses() != null) {
            ApiResponses responses = new ApiResponses();
            for (Map.Entry<String, io.swagger.models.Response> entry : swaggerOp.getResponses().entrySet()) {
                ApiResponse response = new ApiResponse();
                response.setDescription(entry.getValue().getDescription());
                responses.addApiResponse(entry.getKey(), response);
            }
            operation.setResponses(responses);
        }

        return operation;
    }

    /**
     * 创建解析选项
     */
    private static ParseOptions createParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        return options;
    }

    /**
     * 判断是否为 Resource 方法（GET + 无参或只有路径参数）
     */
    private static boolean isResourceMethod(PathItem.HttpMethod method, Operation operation) {
        if (method != PathItem.HttpMethod.GET) {
            return false;
        }

        // 检查是否有 requestBody
        if (operation.getRequestBody() != null) {
            return false;
        }

        // 检查参数类型：只有 path/query 参数且都是简单类型
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if (param.getIn() != null && !param.getIn().equals("path") && !param.getIn().equals("query")) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 提取操作 ID（用作 name/uri）
     */
    private static String extractOperationId(Operation operation, String path, PathItem.HttpMethod method) {
        if (operation.getOperationId() != null) {
            return operation.getOperationId();
        }

        // 从路径生成：/api/v1/users/{id} GET → getUser
        String sanitizedPath = path.replaceAll("\\{([^}]+)}", "$1");
        String[] segments = sanitizedPath.split("/");
        StringBuilder operationId = new StringBuilder();

        operationId.append(method.name().toLowerCase());
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                operationId.append(Character.toUpperCase(segment.charAt(0)));
                if (segment.length() > 1) {
                    operationId.append(segment.substring(1));
                }
            }
        }

        return operationId.toString();
    }

    /**
     * 构建 inputSchema
     */
    private static String buildInputSchema(Operation operation) {
        ObjectNode schema = JSON_MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        // 从 requestBody 中提取
        if (operation.getRequestBody() != null &&
                operation.getRequestBody().getContent() != null) {

            io.swagger.v3.oas.models.media.MediaType mediaType =
                    operation.getRequestBody().getContent().get("application/json");
            if (mediaType != null && mediaType.getSchema() != null) {
                Schema<?> bodySchema = mediaType.getSchema();
                mergeSchema(properties, required, bodySchema);
            }
        }

        // 从参数中提取
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if (param.getName() == null) continue;

                ObjectNode prop = properties.putObject(param.getName());
                if (param.getSchema() != null) {
                    prop.put("type", param.getSchema().getType());
                } else {
                    prop.put("type", "string");
                }
                if (param.getDescription() != null) {
                    prop.put("description", param.getDescription());
                }
                if (param.getRequired() != null && param.getRequired()) {
                    required.add(param.getName());
                }
            }
        }

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }

    /**
     * 合并 Schema 到 properties
     */
    @SuppressWarnings("rawtypes")
    private static void mergeSchema(ObjectNode properties, ArrayNode required, Schema<?> schema) {
        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                mergeSchemaProperty(properties, required, entry.getKey(), entry.getValue(), schema);
            }
        }
    }

    /**
     * 递归合并单个 Schema 属性到目标 properties 和 required
     * 支持嵌套 object 的 properties/required 和 array 的 items
     */
    @SuppressWarnings("rawtypes")
    private static void mergeSchemaProperty(ObjectNode targetProperties, ArrayNode targetRequired,
                                            String fieldName, Schema<?> propSchema, Schema<?> parentSchema) {
        ObjectNode prop = targetProperties.putObject(fieldName);

        if (propSchema.getType() != null) {
            prop.put("type", propSchema.getType());
        }
        if (propSchema.getDescription() != null) {
            prop.put("description", propSchema.getDescription());
        }
        if (parentSchema.getRequired() != null && parentSchema.getRequired().contains(fieldName)) {
            targetRequired.add(fieldName);
        }

        // 递归处理嵌套 object
        if (propSchema.getProperties() != null) {
            ObjectNode nestedProps = prop.putObject("properties");
            ArrayNode nestedRequired = prop.putArray("required");
            for (Map.Entry<String, Schema> nestedEntry : propSchema.getProperties().entrySet()) {
                mergeSchemaProperty(nestedProps, nestedRequired, nestedEntry.getKey(), nestedEntry.getValue(), propSchema);
            }
        }

        // 递归处理 array 的 items
        if (propSchema.getItems() != null) {
            Schema<?> itemsSchema = propSchema.getItems();
            ObjectNode itemsNode = prop.putObject("items");
            if (itemsSchema.getType() != null) {
                itemsNode.put("type", itemsSchema.getType());
            }
            if (itemsSchema.getDescription() != null) {
                itemsNode.put("description", itemsSchema.getDescription());
            }
            if (itemsSchema.getProperties() != null) {
                ObjectNode itemsProps = itemsNode.putObject("properties");
                ArrayNode itemsRequired = itemsNode.putArray("required");
                for (Map.Entry<String, Schema> itemEntry : itemsSchema.getProperties().entrySet()) {
                    mergeSchemaProperty(itemsProps, itemsRequired, itemEntry.getKey(), itemEntry.getValue(), itemsSchema);
                }
            }
        }
    }

    /**
     * 构建 URI template
     */
    private static String buildTemplate(Operation operation) {
        ObjectNode template = JSON_MAPPER.createObjectNode();

        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if (param.getIn() != null && param.getIn().equals("query") && param.getName() != null) {
                    if (param.getSchema() != null && param.getSchema().getType() != null) {
                        template.put(param.getName(), param.getSchema().getType());
                    } else {
                        template.put(param.getName(), "string");
                    }
                }
            }
        }

        if (template.isEmpty()) {
            return null;
        }

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(template);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 校验 user_token 字段（不允许出现在 query/header/body 中）
     *
     * @param openAPI OpenAPI 模型
     * @throws RuntimeException 如果检测到 user_token
     */
    public static void validateUserToken(OpenAPI openAPI) {
        if (openAPI == null || openAPI.getPaths() == null) {
            return;
        }

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = pathEntry.getValue();

            // 检查所有操作
            Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : operations.entrySet()) {
                Operation operation = opEntry.getValue();

                // 1. 检查 query 参数
                if (operation.getParameters() != null) {
                    for (Parameter param : operation.getParameters()) {
                        if ("user_token".equals(param.getName())) {
                            String position = "query";
                            if ("header".equals(param.getIn())) {
                                position = "header";
                            } else if ("path".equals(param.getIn())) {
                                position = "path";
                            }
                            throw new RuntimeException("与预设字段冲突：user_token 不允许作为 " + position + " 参数");
                        }
                    }
                }

                // 2. 检查 requestBody 中的 properties（递归检测所有层级）
                if (operation.getRequestBody() != null &&
                        operation.getRequestBody().getContent() != null) {

                    io.swagger.v3.oas.models.media.MediaType mediaType =
                            operation.getRequestBody().getContent().get("application/json");
                    if (mediaType != null && mediaType.getSchema() != null) {
                        Schema<?> schema = mediaType.getSchema();
                        detectUserTokenInSchema(schema);
                    }
                }
            }
        }
    }

    /**
     * 递归检测 Schema 中是否包含 user_token 字段
     * 检查 properties、嵌套 object 的 properties、以及 array 的 items
     */
    @SuppressWarnings("rawtypes")
    private static void detectUserTokenInSchema(Schema<?> schema) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
            if ("user_token".equals(entry.getKey())) {
                throw new RuntimeException("与预设字段冲突：user_token 不允许作为 body 参数");
            }

            Schema<?> nestedSchema = entry.getValue();

            // 递归检查嵌套 object
            if (nestedSchema.getProperties() != null) {
                detectUserTokenInSchema(nestedSchema);
            }

            // 递归检查 array 的 items
            if (nestedSchema.getItems() != null) {
                detectUserTokenInSchema(nestedSchema.getItems());
            }
        }
    }
}
