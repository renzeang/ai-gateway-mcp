package org.veronicadavichi.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.veronicadavichi.dto.ApiResponse;
import org.veronicadavichi.dto.ExportRequest;
import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.dto.PageResult;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.service.ToolService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP Tool 管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public ApiResponse<PageResult<McpTool>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<McpTool> records = toolService.list(name, method, serverId, enabled, page, size);
        long total = records.size();
        return ApiResponse.success(PageResult.of(records, total, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<McpTool> getById(@PathVariable Long id) {
        McpTool tool = toolService.getById(id);
        if (tool == null) {
            return ApiResponse.error("NOT_FOUND", "工具不存在：" + id);
        }
        return ApiResponse.success(tool);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<McpTool>> create(@RequestBody McpTool tool) {
        try {
            McpTool created = toolService.create(tool);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("NAME_EXISTS", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<McpTool>> update(@PathVariable Long id, @RequestBody McpTool tool) {
        try {
            McpTool updated = toolService.update(id, tool);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            toolService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 导入 OpenAPI - 解析预览（已废弃，请使用 Server 级别导入）
     * @deprecated 使用 POST /api/v1/admin/servers/{serverId}/import/preview 代替
     */
    @Deprecated
    @PostMapping("/import")
    public ApiResponse<ImportPreview> importOpenApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serverId") Long serverId) throws IOException {

        return ApiResponse.error("DEPRECATED", "该端点已废弃，请使用 Server 级别导入：POST /api/v1/admin/servers/{serverId}/import/preview");
    }

    /**
     * 确认导入（已废弃，请使用 Server 级别导入）
     * @deprecated 使用 POST /api/v1/admin/servers/{serverId}/import/confirm 代替
     */
    @Deprecated
    @PostMapping("/confirm-import")
    public ApiResponse<Integer> confirmImport(@RequestBody ImportConfirmRequest request) {
        return ApiResponse.error("DEPRECATED", "该端点已废弃，请使用 Server 级别导入：POST /api/v1/admin/servers/{serverId}/import/confirm");
    }

    /**
     * 导出 OpenAPI（已废弃，请使用 Server 级别导出）
     * @deprecated 使用 POST /api/v1/admin/servers/{serverId}/export 代替
     */
    @Deprecated
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportOpenApi(@RequestBody(required = false) ExportRequest request) {
        return ResponseEntity.badRequest()
                .body("该端点已废弃，请使用 Server 级别导出：POST /api/v1/admin/servers/{serverId}/export".getBytes(StandardCharsets.UTF_8));
    }
}
