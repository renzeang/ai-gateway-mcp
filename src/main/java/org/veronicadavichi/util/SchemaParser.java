package org.veronicadavichi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

/**
 * 嵌套 Schema 解析器
 * 支持对象/数组嵌套结构的解析、验证、序列化/反序列化
 */
public class SchemaParser {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final int MAX_DEPTH = 10;
    private static final String X_PARAM_TYPE = "x-param-type";
    private static final String X_HEADER_NAME = "x-header-name";
    private static final String X_REQUIRED = "x-required";
    private static final String X_CATEGORY = "x-category";

    /**
     * 解析 JSON 字符串为嵌套 Schema 对象
     * @param json JSON Schema 字符串
     * @return 解析后的 JsonNode
     * @throws SchemaValidationException 当 Schema 格式不合法时
     */
    public JsonNode parse(String json) throws SchemaValidationException {
        try {
            JsonNode schema = JSON_MAPPER.readTree(json);
            validate(schema);
            return schema;
        } catch (Exception e) {
            throw new SchemaValidationException("INVALID_SCHEMA", "嵌套 Schema 格式不合法：" + e.getMessage());
        }
    }

    /**
     * 验证嵌套 Schema 的合法性
     * @param schema 待验证的 Schema
     * @throws SchemaValidationException 当验证失败时
     */
    public void validate(JsonNode schema) throws SchemaValidationException {
        if (schema == null || !schema.isObject()) {
            throw new SchemaValidationException("INVALID_SCHEMA", "Schema 必须为 JSON 对象");
        }

        // 验证类型
        JsonNode typeNode = schema.get("type");
        if (typeNode != null && !"object".equals(typeNode.asText())) {
            throw new SchemaValidationException("INVALID_SCHEMA", "根 Schema 的 type 必须为 object");
        }

        // 验证嵌套深度
        validateDepth(schema, MAX_DEPTH);

        // 验证循环引用
        detectCircularReference(schema);

        // 验证入参方式与类型的匹配性
        validateParamType(schema);

        // 验证字段名唯一性
        validateFieldNameUniqueness(schema);
    }

    /**
     * 计算嵌套深度
     * @param schema Schema 节点
     * @return 最大嵌套深度
     */
    public int getMaxDepth(JsonNode schema) {
        return calculateDepth(schema, 0);
    }

    /**
     * 验证嵌套深度不超过限制
     * @param schema Schema 节点
     * @param maxDepth 最大允许深度
     * @throws SchemaValidationException 当深度超过限制时
     */
    public void validateDepth(JsonNode schema, int maxDepth) throws SchemaValidationException {
        int depth = getMaxDepth(schema);
        if (depth > maxDepth) {
            throw new SchemaValidationException("DEPTH_EXCEEDED",
                "嵌套深度超过限制：" + depth + " > " + maxDepth);
        }
    }

    /**
     * 检测循环引用
     * @param schema Schema 节点
     * @throws SchemaValidationException 当检测到循环引用时
     */
    public void detectCircularReference(JsonNode schema) throws SchemaValidationException {
        Set<String> visitedPaths = new HashSet<>();
        detectCircularReferenceRecursive(schema, "", visitedPaths);
    }

    /**
     * 验证入参方式与类型的匹配性
     * @param schema Schema 节点
     * @throws SchemaValidationException 当入参方式与类型不匹配时
     */
    public void validateParamType(JsonNode schema) throws SchemaValidationException {
        validateParamTypeRecursive(schema, "");
    }

    /**
     * 将嵌套 Schema 序列化为标准 JSON Schema 格式
     * @param schema 嵌套 Schema
     * @return 标准 JSON Schema 字符串
     */
    public String toJsonSchema(JsonNode schema) throws SchemaValidationException {
        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception e) {
            throw new SchemaValidationException("INVALID_SCHEMA", "Schema 序列化失败：" + e.getMessage());
        }
    }

    /**
     * 从嵌套 Schema 中提取 QueryParams 参数列表
     * @param schema 嵌套 Schema
     * @return QueryParams 参数名列表
     */
    public Set<String> extractQueryParams(JsonNode schema) {
        Set<String> params = new HashSet<>();
        extractParamsByType(schema, "query", params);
        return params;
    }

    /**
     * 从嵌套 Schema 中提取 Header 参数列表
     * @param schema 嵌套 Schema
     * @return Header 参数映射 {参数名 -> Header 名}
     */
    public java.util.Map<String, String> extractHeaderParams(JsonNode schema) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        if (schema == null) return headers;

        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            extractHeaderParamsFromProperties(properties, headers);
        }
        return headers;
    }

    /**
     * 从嵌套 Schema 中提取 JSON Body 结构
     * @param schema 嵌套 Schema
     * @return JSON Body Schema（仅包含 x-param-type 为 json 的字段）
     */
    public JsonNode extractJsonBody(JsonNode schema) {
        ObjectNode bodySchema = JSON_MAPPER.createObjectNode();
        bodySchema.put("type", "object");

        ObjectNode properties = bodySchema.putObject("properties");
        ArrayNode required = bodySchema.putArray("required");

        JsonNode propertiesNode = schema.get("properties");
        if (propertiesNode != null && propertiesNode.isObject()) {
            extractJsonBodyRecursive(propertiesNode, properties, required);
        }

        return bodySchema;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 递归计算深度
     */
    private int calculateDepth(JsonNode node, int currentDepth) {
        if (node == null || !node.isObject()) {
            return currentDepth;
        }

        int[] maxDepthRef = new int[]{currentDepth};
        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                JsonNode child = entry.getValue();
                int childDepth = calculateDepth(child, currentDepth + 1);
                if (childDepth > maxDepthRef[0]) {
                    maxDepthRef[0] = childDepth;
                }

                // 处理数组的 items
                JsonNode items = child.get("items");
                if (items != null) {
                    int itemsDepth = calculateDepth(items, currentDepth + 1);
                    if (itemsDepth > maxDepthRef[0]) {
                        maxDepthRef[0] = itemsDepth;
                    }
                }
            });
        }

        // 处理当前节点的 items
        JsonNode items = node.get("items");
        if (items != null && items.isObject()) {
            int itemsDepth = calculateDepth(items, currentDepth + 1);
            if (itemsDepth > maxDepthRef[0]) {
                maxDepthRef[0] = itemsDepth;
            }
        }

        return maxDepthRef[0];
    }

    /**
     * 递归检测循环引用（基于路径追踪）
     */
    private void detectCircularReferenceRecursive(JsonNode node, String path, Set<String> visitedPaths)
            throws SchemaValidationException {
        if (node == null || !node.isObject()) {
            return;
        }

        // 检查当前路径是否已访问
        if (visitedPaths.contains(path)) {
            throw new SchemaValidationException("CIRCULAR_REFERENCE",
                "检测到循环引用：" + path);
        }

        visitedPaths.add(path);

        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            for (var entry : properties.properties()) {
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                detectCircularReferenceRecursive(entry.getValue(), childPath, new HashSet<>(visitedPaths));
            }
        }

        // 处理数组的 items
        JsonNode items = node.get("items");
        if (items != null && items.isObject()) {
            detectCircularReferenceRecursive(items, path + "[]", new HashSet<>(visitedPaths));
        }
    }

    /**
     * 递归验证入参方式与类型的匹配性
     */
    private void validateParamTypeRecursive(JsonNode node, String path) throws SchemaValidationException {
        if (node == null || !node.isObject()) {
            return;
        }

        JsonNode paramTypeNode = node.get(X_PARAM_TYPE);
        JsonNode typeNode = node.get("type");

        if (paramTypeNode != null && typeNode != null) {
            String paramType = paramTypeNode.asText();
            String type = typeNode.asText();

            // query/header 参数只能是基础类型
            if (("query".equals(paramType) || "header".equals(paramType)) &&
                !isPrimitiveType(type)) {
                throw new SchemaValidationException("INVALID_PARAM_TYPE",
                    "入参方式 '" + paramType + "' 只能是基础类型（string/number/boolean），但字段 '" +
                    path + "' 的类型为 '" + type + "'");
            }
        }

        // 递归验证子字段
        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            for (var entry : properties.properties()) {
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                validateParamTypeRecursive(entry.getValue(), childPath);
            }
        }

        // 验证数组的 items
        JsonNode items = node.get("items");
        if (items != null && items.isObject()) {
            validateParamTypeRecursive(items, path + "[]");
        }
    }

    /**
     * 验证字段名唯一性（同层级）
     */
    private void validateFieldNameUniqueness(JsonNode node) throws SchemaValidationException {
        validateFieldNameUniquenessRecursive(node, "");
    }

    private void validateFieldNameUniquenessRecursive(JsonNode node, String path)
            throws SchemaValidationException {
        if (node == null || !node.isObject()) {
            return;
        }

        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            Set<String> fieldNames = new HashSet<>();
            for (var entry : properties.properties()) {
                String fieldName = entry.getKey();
                if (fieldNames.contains(fieldName)) {
                    throw new SchemaValidationException("INVALID_SCHEMA",
                        "字段名重复：'" + fieldName + "' 在路径 '" + path + "' 下出现多次");
                }
                fieldNames.add(fieldName);

                String childPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                validateFieldNameUniquenessRecursive(entry.getValue(), childPath);
            }
        }

        // 验证数组的 items
        JsonNode items = node.get("items");
        if (items != null && items.isObject()) {
            validateFieldNameUniquenessRecursive(items, path + "[]");
        }
    }

    /**
     * 判断是否为基础类型
     */
    private boolean isPrimitiveType(String type) {
        return "string".equals(type) || "number".equals(type) || "boolean".equals(type);
    }

    /**
     * 提取指定类型的参数
     */
    private void extractParamsByType(JsonNode node, String paramType, Set<String> params) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 参数名在调用上下文中处理，此处仅递归遍历

        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                JsonNode child = entry.getValue();
                JsonNode childParamType = child.get(X_PARAM_TYPE);
                if (childParamType != null && paramType.equals(childParamType.asText())) {
                    params.add(entry.getKey());
                }
                extractParamsByType(child, paramType, params);
            });
        }
    }

    private void extractHeaderParamsFromProperties(JsonNode properties, java.util.Map<String, String> headers) {
        if (properties == null || !properties.isObject()) {
            return;
        }

        properties.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldSchema = entry.getValue();
            JsonNode paramTypeNode = fieldSchema.get(X_PARAM_TYPE);
            JsonNode headerNameNode = fieldSchema.get(X_HEADER_NAME);

            if (paramTypeNode != null && "header".equals(paramTypeNode.asText())) {
                String headerName = headerNameNode != null ? headerNameNode.asText() : fieldName.toUpperCase().replace("_", "-");
                headers.put(fieldName, headerName);
            }

            // 递归处理嵌套对象的 properties
            JsonNode nestedProperties = fieldSchema.get("properties");
            if (nestedProperties != null && nestedProperties.isObject()) {
                extractHeaderParamsFromProperties(nestedProperties, headers);
            }
        });
    }

    /**
     * 提取 JSON Body 字段
     */
    private void extractJsonBodyRecursive(JsonNode propertiesNode, ObjectNode targetProperties,
                                          ArrayNode targetRequired) {
        propertiesNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldSchema = entry.getValue();
            JsonNode paramType = fieldSchema.get(X_PARAM_TYPE);

            // 只处理 json 类型的字段或没有明确 paramType 的字段（默认为 json）
            if (paramType == null || "json".equals(paramType.asText())) {
                targetProperties.set(fieldName, fieldSchema);

                // 检查是否必填
                JsonNode requiredNode = fieldSchema.get(X_REQUIRED);
                if (requiredNode != null && requiredNode.asBoolean()) {
                    targetRequired.add(fieldName);
                }
            }
        });
    }

    /**
     * 自定义 Schema 验证异常
     */
    public static class SchemaValidationException extends Exception {
        private final String errorCode;

        public SchemaValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
