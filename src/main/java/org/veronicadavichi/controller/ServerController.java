package org.veronicadavichi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.veronicadavichi.dto.ApiResponse;
import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.dto.PageResult;
import org.veronicadavichi.dto.ServerRelationCount;
import org.veronicadavichi.entity.McpServer;
import org.veronicadavichi.service.ResourceService;
import org.veronicadavichi.service.ServerService;
import org.veronicadavichi.service.ToolService;
import org.veronicadavichi.util.OpenApiParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MCP Server 管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/servers")
public class ServerController {

    private final ServerService serverService;
    private final ToolService toolService;
    private final ResourceService resourceService;

    public ServerController(ServerService serverService, ToolService toolService, ResourceService resourceService) {
        this.serverService = serverService;
        this.toolService = toolService;
        this.resourceService = resourceService;
    }

    /**
     * 分页查询服务器列表
     */
    @GetMapping
    public ApiResponse<PageResult<McpServer>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<McpServer> records = serverService.list(name, enabled, page, size);
        long total = records.size(); // 实际项目中应使用 count 查询

        return ApiResponse.success(PageResult.of(records, total, page, size));
    }

    /**
     * 获取服务器详情
     */
    @GetMapping("/{id}")
    public ApiResponse<McpServer> getById(@PathVariable Long id) {
        McpServer server = serverService.getById(id);
        if (server == null) {
            return ApiResponse.error("NOT_FOUND", "服务不存在：" + id);
        }
        return ApiResponse.success(server);
    }

    /**
     * 创建服务器
     */
    @PostMapping
    public ResponseEntity<ApiResponse<McpServer>> create(@Valid @RequestBody McpServer server, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", result.getFieldError().getDefaultMessage()));
        }
        try {
            McpServer created = serverService.create(server);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("NAME_EXISTS", e.getMessage()));
        }
    }

    /**
     * 更新服务器
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<McpServer>> update(
            @PathVariable Long id,
            @Valid @RequestBody McpServer server,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", result.getFieldError().getDefaultMessage()));
        }
        try {
            McpServer updated = serverService.update(id, server);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("NAME_EXISTS", e.getMessage()));
        }
    }

    /**
     * 获取服务器关联数据数量
     */
    @GetMapping("/{id}/relations")
    public ApiResponse<ServerRelationCount> getRelations(@PathVariable Long id) {
        try {
            ServerRelationCount counts = serverService.countRelations(id);
            return ApiResponse.success(counts);
        } catch (Exception e) {
            return ApiResponse.error("INTERNAL_ERROR", e.getMessage());
        }
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean cascade) {
        try {
            serverService.delete(id, cascade);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 导出 Server 配置为 OpenAPI JSON
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long id,
            @RequestBody(required = false) ExportRequest request) {

        try {
            McpServer server = serverService.getById(id);
            if (server == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 调用服务导出
            List<Long> toolIds = request != null ? request.getToolIds() : null;
            List<Long> resourceIds = request != null ? request.getResourceIds() : null;
            String openApiJson = serverService.exportByServerId(id, toolIds, resourceIds);

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = String.format("server-export-%s-%s.json",
                    URLEncoder.encode(server.getName(), StandardCharsets.UTF_8), timestamp);

            // 返回文件流
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(openApiJson.getBytes(StandardCharsets.UTF_8));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 导出请求 DTO
     */
    public static class ExportRequest {
        private List<Long> toolIds;
        private List<Long> resourceIds;

        public List<Long> getToolIds() { return toolIds; }
        public void setToolIds(List<Long> toolIds) { this.toolIds = toolIds; }
        public List<Long> getResourceIds() { return resourceIds; }
        public void setResourceIds(List<Long> resourceIds) { this.resourceIds = resourceIds; }
    }

    /**
     * 导入预览 - 解析上传的 OpenAPI 文件
     */
    @PostMapping("/{id}/import/preview")
    public ApiResponse<ImportPreview> importPreview(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        try {
            // 校验 Server 是否存在
            McpServer server = serverService.getById(id);
            if (server == null) {
                return ApiResponse.error("NOT_FOUND", "服务不存在：" + id);
            }

            // 读取文件内容
            if (file.isEmpty()) {
                return ApiResponse.error("INVALID_PARAM", "上传文件为空");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 解析为 Tools 和 Resources
            // 注意：parseToTools 和 parseToResources 内部会调用 validateUserToken
            List<org.veronicadavichi.entity.McpTool> tools = OpenApiParser.parseToTools(content);
            List<org.veronicadavichi.entity.McpResource> resources = OpenApiParser.parseToResources(content);

            // 查询当前 Server 已有的 Tools 和 Resources，用于检测重复
            List<org.veronicadavichi.entity.McpTool> existingTools = toolService.getByServerId(id);
            List<org.veronicadavichi.entity.McpResource> existingResources = resourceService.getByServerId(id);

            // 生成预览数据（包含重复检测）
            ImportPreview preview = ImportPreview.of(tools, resources, existingTools, existingResources);
            return ApiResponse.success(preview);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("user_token")) {
                return ApiResponse.error("USER_TOKEN_FORBIDDEN", e.getMessage());
            }
            return ApiResponse.error("PARSE_ERROR", "解析失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("INTERNAL_ERROR", "内部错误：" + e.getMessage());
        }
    }

    /**
     * 确认导入 - 将勾选的条目写入数据库
     */
    @PostMapping("/{id}/import/confirm")
    public ApiResponse<ImportResult> importConfirm(
            @PathVariable Long id,
            @RequestBody ImportConfirmRequest request) {

        try {
            // 校验 Server 是否存在
            McpServer server = serverService.getById(id);
            if (server == null) {
                return ApiResponse.error("NOT_FOUND", "服务不存在：" + id);
            }

            // 校验 request
            if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
                return ApiResponse.error("INVALID_PARAM", "导入数据为空");
            }

            // 分离 Tools 和 Resources
            List<ImportConfirmRequest.ImportItem> toolItems = request.getItems().stream()
                .filter(item -> "tool".equals(item.getType()))
                .toList();
            List<ImportConfirmRequest.ImportItem> resourceItems = request.getItems().stream()
                .filter(item -> "resource".equals(item.getType()))
                .toList();

            System.out.println("[ServerController] 总条目数：" + request.getItems().size());
            System.out.println("[ServerController] Tools: " + toolItems.size());
            System.out.println("[ServerController] Resources: " + resourceItems.size());

            // 导入 Tools
            int toolCount = 0;
            if (!toolItems.isEmpty()) {
                ImportConfirmRequest toolRequest = new ImportConfirmRequest();
                toolRequest.setServerId(id);
                toolRequest.setItems(toolItems);
                toolCount = toolService.confirmImport(toolRequest);
            }

            // 导入 Resources
            int resourceCount = 0;
            if (!resourceItems.isEmpty()) {
                ImportConfirmRequest resourceRequest = new ImportConfirmRequest();
                resourceRequest.setServerId(id);
                resourceRequest.setItems(resourceItems);
                resourceCount = resourceService.confirmImport(resourceRequest);
            }

            // 返回结果
            ImportResult result = new ImportResult();
            result.setSuccessCount(toolCount + resourceCount);
            result.setToolCount(toolCount);
            result.setResourceCount(resourceCount);

            return ApiResponse.success(result);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("user_token")) {
                return ApiResponse.error("USER_TOKEN_FORBIDDEN", e.getMessage());
            }
            return ApiResponse.error("IMPORT_ERROR", "导入失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("INTERNAL_ERROR", "内部错误：" + e.getMessage());
        }
    }

    /**
     * 导入结果
     */
    public static class ImportResult {
        private int successCount;
        private int toolCount;
        private int resourceCount;

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getToolCount() { return toolCount; }
        public void setToolCount(int toolCount) { this.toolCount = toolCount; }
        public int getResourceCount() { return resourceCount; }
        public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
    }
}
