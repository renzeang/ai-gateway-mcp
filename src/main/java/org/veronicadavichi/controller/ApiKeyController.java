package org.veronicadavichi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.veronicadavichi.dto.ApiKeyCreateResult;
import org.veronicadavichi.dto.ApiResponse;
import org.veronicadavichi.dto.CreateApiKeyRequest;
import org.veronicadavichi.dto.PageResult;
import org.veronicadavichi.entity.McpApiKey;
import org.veronicadavichi.service.McpApiKeyService;

import java.util.List;

/**
 * API Key 管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/api-keys")
public class ApiKeyController {

    private final McpApiKeyService apiKeyService;

    public ApiKeyController(McpApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * 分页查询 API Key 列表
     */
    @GetMapping
    public ApiResponse<PageResult<McpApiKey>> list(
            @RequestParam(required = false) String alias,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<McpApiKey> records = apiKeyService.list(alias, enabled, page, size);
        long total = apiKeyService.countList(alias, enabled);
        return ApiResponse.success(PageResult.of(records, total, page, size));
    }

    /**
     * 创建 API Key — 返回包含明文 key 的结果（仅一次）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyCreateResult>> create(@RequestBody CreateApiKeyRequest request) {
        if (request.getAlias() == null || request.getAlias().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", "别名不能为空"));
        }
        try {
            ApiKeyCreateResult result = apiKeyService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CREATE_ERROR", "创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取 API Key 详情（不含明文）
     */
    @GetMapping("/{id}")
    public ApiResponse<McpApiKey> getById(@PathVariable Long id) {
        McpApiKey apiKey = apiKeyService.getById(id);
        if (apiKey == null) {
            return ApiResponse.error("NOT_FOUND", "API Key 不存在: " + id);
        }
        return ApiResponse.success(apiKey);
    }

    /**
     * 启用/停用 API Key
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<McpApiKey>> toggle(
            @PathVariable Long id,
            @RequestBody ToggleApiKeyRequest request) {
        try {
            McpApiKey updated = apiKeyService.toggle(id, request.isEnabled());
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 删除 API Key（物理删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            apiKeyService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 启用/停用请求 DTO
     */
    public static class ToggleApiKeyRequest {
        private boolean enabled;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
