package org.veronicadavichi.util;

import org.junit.jupiter.api.Test;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.entity.McpResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenApiParser 单元测试
 */
public class OpenApiParserTest {

    // OpenAPI 3.0 JSON 示例
    private static final String OPENAPI_3_JSON = """
        {
          "openapi": "3.0.0",
          "info": { "title": "Test API", "version": "1.0.0" },
          "paths": {
            "/users": {
              "post": {
                "operationId": "createUser",
                "requestBody": {
                  "content": {
                    "application/json": {
                      "schema": {
                        "type": "object",
                        "properties": {
                          "name": { "type": "string" }
                        }
                      }
                    }
                  }
                },
                "responses": { "200": { "description": "Success" } }
              }
            }
          }
        }
        """;

    // Swagger 2.0 JSON 示例
    private static final String SWAGGER_2_JSON = """
        {
          "swagger": "2.0",
          "info": { "title": "Test API", "version": "1.0.0" },
          "paths": {
            "/users": {
              "post": {
                "operationId": "createUser",
                "parameters": [
                  {
                    "name": "body",
                    "in": "body",
                    "schema": {
                      "type": "object",
                      "properties": {
                        "name": { "type": "string" }
                      }
                    }
                  }
                ],
                "responses": { "200": { "description": "Success" } }
              }
            }
          }
        }
        """;

    // OpenAPI 3.0 YAML 示例
    private static final String OPENAPI_3_YAML = """
        openapi: 3.0.0
        info:
          title: Test API
          version: 1.0.0
        paths:
          /users:
            post:
              operationId: createUser
              requestBody:
                content:
                  application/json:
                    schema:
                      type: object
                      properties:
                        name:
                          type: string
              responses:
                '200':
                  description: Success
        """;

    // Swagger 2.0 YAML 示例
    private static final String SWAGGER_2_YAML = """
        swagger: '2.0'
        info:
          title: Test API
          version: 1.0.0
        paths:
          /users:
            post:
              operationId: createUser
              parameters:
                - name: body
                  in: body
                  schema:
                    type: object
                    properties:
                      name:
                        type: string
              responses:
                '200':
                  description: Success
        """;

    @Test
    void testParseOpenApi3Json() {
        List<McpTool> tools = OpenApiParser.parseToTools(OPENAPI_3_JSON);
        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertEquals("createUser", tools.get(0).getName());
        assertEquals("POST", tools.get(0).getMethod());
    }

    @Test
    void testParseSwagger2Json() {
        List<McpTool> tools = OpenApiParser.parseToTools(SWAGGER_2_JSON);
        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertEquals("createUser", tools.get(0).getName());
        assertEquals("POST", tools.get(0).getMethod());
    }

    @Test
    void testParseOpenApi3Yaml() {
        List<McpTool> tools = OpenApiParser.parseToTools(OPENAPI_3_YAML);
        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertEquals("createUser", tools.get(0).getName());
        assertEquals("POST", tools.get(0).getMethod());
    }

    @Test
    void testParseSwagger2Yaml() {
        List<McpTool> tools = OpenApiParser.parseToTools(SWAGGER_2_YAML);
        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertEquals("createUser", tools.get(0).getName());
        assertEquals("POST", tools.get(0).getMethod());
    }

    @Test
    void testParseResources() {
        // GET 无请求体 → Resource
        List<McpResource> resources = OpenApiParser.parseToResources(OPENAPI_3_JSON);
        assertNotNull(resources);
        // OPENAPI_3_JSON 中的 POST 方法不是 Resource，所以应该是 0 个
        assertEquals(0, resources.size());
    }

    @Test
    void testParseEmptyContent() {
        List<McpTool> tools = OpenApiParser.parseToTools("");
        assertNotNull(tools);
        assertEquals(0, tools.size());

        List<McpResource> resources = OpenApiParser.parseToResources("");
        assertNotNull(resources);
        assertEquals(0, resources.size());
    }

    @Test
    void testGenerateOpenApiWithExtensions() throws Exception {
        // 创建测试 Tool
        McpTool tool = new McpTool();
        tool.setName("testTool");
        tool.setDescription("Test Tool Description");
        tool.setPath("/api/test");
        tool.setMethod("POST");
        tool.setInputSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        tool.setEnabled(true);

        List<McpTool> tools = List.of(tool);

        // 生成 OpenAPI
        String openApiJson = OpenApiParser.generateOpenApi(tools, null, 1, "TestServer");

        // 验证生成的格式
        com.fasterxml.jackson.databind.JsonNode root =
            OpenApiParser.JSON_MAPPER.readTree(openApiJson);

        // 验证 openapi 版本
        assertEquals("3.0.0", root.get("openapi").asText());

        // 验证 info 字段
        com.fasterxml.jackson.databind.JsonNode info = root.get("info");
        assertEquals("MCP Server Export - TestServer", info.get("title").asText());
        assertEquals("1.0.0", info.get("version").asText());
        assertEquals(1, info.get("x-mcp-server-id").asInt());
        assertEquals("TestServer", info.get("x-mcp-server-name").asText());
        assertNotNull(info.get("x-mcp-export-timestamp"));

        // 验证 paths
        com.fasterxml.jackson.databind.JsonNode paths = root.get("paths");
        assertNotNull(paths.get("/api/test"));

        // 验证 operation
        com.fasterxml.jackson.databind.JsonNode postOp = paths.get("/api/test").get("post");
        assertEquals("testTool", postOp.get("operationId").asText());
        assertEquals("tool", postOp.get("x-mcp-type").asText());
        assertTrue(postOp.get("x-mcp-enabled").asBoolean());
        assertNotNull(postOp.get("requestBody"));
    }

    @Test
    void testGenerateOpenApiForResource() throws Exception {
        // 创建测试 Resource
        McpResource resource = new McpResource();
        resource.setUri("getData");
        resource.setName("Get Data");
        resource.setDescription("Get data resource");
        resource.setPath("/api/data/{id}");
        resource.setMethod("GET");
        resource.setTemplate("{\"id\":\"string\",\"filter\":\"string\"}");
        resource.setEnabled(true);

        List<McpResource> resources = List.of(resource);

        // 生成 OpenAPI
        String openApiJson = OpenApiParser.generateOpenApi(null, resources, 2, "DataServer");

        // 验证生成的格式
        com.fasterxml.jackson.databind.JsonNode root =
            OpenApiParser.JSON_MAPPER.readTree(openApiJson);

        // 验证 info 字段
        com.fasterxml.jackson.databind.JsonNode info = root.get("info");
        assertEquals("MCP Server Export - DataServer", info.get("title").asText());
        assertEquals(2, info.get("x-mcp-server-id").asInt());

        // 验证 paths
        com.fasterxml.jackson.databind.JsonNode paths = root.get("paths");
        assertNotNull(paths.get("/api/data/{id}"));

        // 验证 operation
        com.fasterxml.jackson.databind.JsonNode getOp = paths.get("/api/data/{id}").get("get");
        assertEquals("getData", getOp.get("operationId").asText());
        assertEquals("resource", getOp.get("x-mcp-type").asText());

        // 验证 parameters
        com.fasterxml.jackson.databind.JsonNode params = getOp.get("parameters");
        assertNotNull(params);
        assertEquals(2, params.size());
    }

    @Test
    void testValidateUserTokenInQuery() {
        // 包含 user_token 作为 query 参数的 OpenAPI
        String openApiWithUserToken = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/api/test": {
                  "get": {
                    "operationId": "test",
                    "parameters": [
                      { "name": "user_token", "in": "query", "schema": { "type": "string" } }
                    ],
                    "responses": { "200": { "description": "Success" } }
                  }
                }
              }
            }
            """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OpenApiParser.parseToTools(openApiWithUserToken));
        assertTrue(ex.getMessage().contains("user_token"));
        assertTrue(ex.getMessage().contains("query"));
    }

    @Test
    void testValidateUserTokenInHeader() {
        // 包含 user_token 作为 header 参数的 OpenAPI
        String openApiWithUserToken = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/api/test": {
                  "post": {
                    "operationId": "test",
                    "parameters": [
                      { "name": "user_token", "in": "header", "schema": { "type": "string" } }
                    ],
                    "responses": { "200": { "description": "Success" } }
                  }
                }
              }
            }
            """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OpenApiParser.parseToTools(openApiWithUserToken));
        assertTrue(ex.getMessage().contains("user_token"));
        assertTrue(ex.getMessage().contains("header"));
    }

    @Test
    void testValidateUserTokenInBody() {
        // 包含 user_token 作为 body 参数的 OpenAPI
        String openApiWithUserToken = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/api/test": {
                  "post": {
                    "operationId": "test",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "user_token": { "type": "string" },
                              "name": { "type": "string" }
                            }
                          }
                        }
                      }
                    },
                    "responses": { "200": { "description": "Success" } }
                  }
                }
              }
            }
            """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OpenApiParser.parseToTools(openApiWithUserToken));
        assertTrue(ex.getMessage().contains("user_token"));
        assertTrue(ex.getMessage().contains("body"));
    }

    @Test
    void testValidateUserTokenPass() {
        // 不包含 user_token 的 OpenAPI，应该正常通过
        String validOpenApi = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/api/test": {
                  "post": {
                    "operationId": "test",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "name": { "type": "string" }
                            }
                          }
                        }
                      }
                    },
                    "responses": { "200": { "description": "Success" } }
                  }
                }
              }
            }
            """;

        List<McpTool> tools = OpenApiParser.parseToTools(validOpenApi);
        assertNotNull(tools);
        assertEquals(1, tools.size());
    }

    @Test
    void testParseNestedObjectTwoLevels() throws Exception {
        // 使用 src/test/resources/ai-gateway-mcp.openapi.json 中的结构
        // requestBody 包含 paramA (object, 内含 param1) 和 param2 (string)
        String openApiWithNested = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/": {
                  "post": {
                    "summary": "restServerPostTest",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "paramA": {
                                "type": "object",
                                "properties": {
                                  "param1": { "type": "string" }
                                },
                                "required": ["param1"]
                              },
                              "param2": { "type": "string" }
                            },
                            "required": ["paramA", "param2"]
                          }
                        }
                      },
                      "required": true
                    },
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

        List<McpTool> tools = OpenApiParser.parseToTools(openApiWithNested);
        assertNotNull(tools);
        assertEquals(1, tools.size());

        String inputSchema = tools.get(0).getInputSchema();
        com.fasterxml.jackson.databind.JsonNode schema =
            OpenApiParser.JSON_MAPPER.readTree(inputSchema);

        // 验证顶层字段
        assertNotNull(schema.get("properties").get("paramA"));
        assertEquals("object", schema.get("properties").get("paramA").get("type").asText());
        assertNotNull(schema.get("properties").get("param2"));
        assertEquals("string", schema.get("properties").get("param2").get("type").asText());

        // 验证嵌套字段 paramA.param1
        com.fasterxml.jackson.databind.JsonNode paramA = schema.get("properties").get("paramA");
        assertNotNull(paramA.get("properties"), "paramA 应包含 properties");
        assertNotNull(paramA.get("properties").get("param1"), "paramA.properties 应包含 param1");
        assertEquals("string", paramA.get("properties").get("param1").get("type").asText());

        // 验证嵌套 required
        assertNotNull(paramA.get("required"), "paramA 应包含 required");
        assertEquals(1, paramA.get("required").size());
        assertEquals("param1", paramA.get("required").get(0).asText());

        // 验证顶层 required
        assertNotNull(schema.get("required"));
        assertEquals(2, schema.get("required").size());
    }

    @Test
    void testParseNestedObjectThreeLevels() throws Exception {
        // 3 层嵌套：A.B.C
        String openApiDeep = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/deep": {
                  "post": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "level1": {
                                "type": "object",
                                "properties": {
                                  "level2": {
                                    "type": "object",
                                    "properties": {
                                      "level3": { "type": "string" }
                                    },
                                    "required": ["level3"]
                                  }
                                },
                                "required": ["level2"]
                              }
                            },
                            "required": ["level1"]
                          }
                        }
                      },
                      "required": true
                    },
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

        List<McpTool> tools = OpenApiParser.parseToTools(openApiDeep);
        assertEquals(1, tools.size());

        com.fasterxml.jackson.databind.JsonNode schema =
            OpenApiParser.JSON_MAPPER.readTree(tools.get(0).getInputSchema());

        // 验证 level1
        com.fasterxml.jackson.databind.JsonNode level1 = schema.get("properties").get("level1");
        assertEquals("object", level1.get("type").asText());

        // 验证 level2
        com.fasterxml.jackson.databind.JsonNode level2 = level1.get("properties").get("level2");
        assertNotNull(level2, "level2 不应丢失");
        assertEquals("object", level2.get("type").asText());

        // 验证 level3
        com.fasterxml.jackson.databind.JsonNode level3 = level2.get("properties").get("level3");
        assertNotNull(level3, "level3 不应丢失");
        assertEquals("string", level3.get("type").asText());

        // 验证各层 required
        assertEquals("level2", level1.get("required").get(0).asText());
        assertEquals("level3", level2.get("required").get(0).asText());
    }

    @Test
    void testParseArrayWithObjectItems() throws Exception {
        // array 类型，items 为 object 且包含 properties
        String openApiArray = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/items": {
                  "post": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "tags": {
                                "type": "array",
                                "items": {
                                  "type": "object",
                                  "properties": {
                                    "name": { "type": "string" },
                                    "value": { "type": "number" }
                                  },
                                  "required": ["name"]
                                }
                              }
                            }
                          }
                        }
                      },
                      "required": true
                    },
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

        List<McpTool> tools = OpenApiParser.parseToTools(openApiArray);
        assertEquals(1, tools.size());

        com.fasterxml.jackson.databind.JsonNode schema =
            OpenApiParser.JSON_MAPPER.readTree(tools.get(0).getInputSchema());

        com.fasterxml.jackson.databind.JsonNode tags = schema.get("properties").get("tags");
        assertEquals("array", tags.get("type").asText());

        com.fasterxml.jackson.databind.JsonNode items = tags.get("items");
        assertNotNull(items, "array items 不应丢失");
        assertEquals("object", items.get("type").asText());

        com.fasterxml.jackson.databind.JsonNode itemsProps = items.get("properties");
        assertNotNull(itemsProps, "items.properties 不应丢失");
        assertEquals("string", itemsProps.get("name").get("type").asText());
        assertEquals("number", itemsProps.get("value").get("type").asText());

        com.fasterxml.jackson.databind.JsonNode itemsRequired = items.get("required");
        assertNotNull(itemsRequired, "items.required 不应丢失");
        assertEquals("name", itemsRequired.get(0).asText());
    }

    @Test
    void testValidateUserTokenInNestedBody() {
        // user_token 嵌套在 object 子层级中应被检测
        String openApiWithNestedUserToken = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/api/test": {
                  "post": {
                    "operationId": "test",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "metadata": {
                                "type": "object",
                                "properties": {
                                  "user_token": { "type": "string" },
                                  "name": { "type": "string" }
                                }
                              }
                            }
                          }
                        }
                      },
                      "required": true
                    },
                    "responses": { "200": { "description": "Success" } }
                  }
                }
              }
            }
            """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OpenApiParser.parseToTools(openApiWithNestedUserToken));
        assertTrue(ex.getMessage().contains("user_token"));
        assertTrue(ex.getMessage().contains("body"));
    }

    @Test
    void testParseEmptyObjectProperty() throws Exception {
        // object 字段但无 properties 定义，应仅保留 type: object
        String openApiEmptyObject = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0.0" },
              "paths": {
                "/empty": {
                  "post": {
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "emptyObj": { "type": "object" },
                              "name": { "type": "string" }
                            }
                          }
                        }
                      },
                      "required": true
                    },
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

        List<McpTool> tools = OpenApiParser.parseToTools(openApiEmptyObject);
        assertEquals(1, tools.size());

        com.fasterxml.jackson.databind.JsonNode schema =
            OpenApiParser.JSON_MAPPER.readTree(tools.get(0).getInputSchema());

        com.fasterxml.jackson.databind.JsonNode emptyObj = schema.get("properties").get("emptyObj");
        assertEquals("object", emptyObj.get("type").asText());
        // 空 object 不应有 properties
        assertNull(emptyObj.get("properties"));

        // 验证扁平字段不受影响
        assertEquals("string", schema.get("properties").get("name").get("type").asText());
    }

    @Test
    void testParseFlatSchemaRegression() throws Exception {
        // 回归测试：扁平 schema（无嵌套）行为与修复前一致
        List<McpTool> tools = OpenApiParser.parseToTools(OPENAPI_3_JSON);
        assertNotNull(tools);
        assertEquals(1, tools.size());
        assertEquals("createUser", tools.get(0).getName());

        String inputSchema = tools.get(0).getInputSchema();
        com.fasterxml.jackson.databind.JsonNode schema =
            OpenApiParser.JSON_MAPPER.readTree(inputSchema);

        assertNotNull(schema.get("properties").get("name"));
        assertEquals("string", schema.get("properties").get("name").get("type").asText());
    }
}
