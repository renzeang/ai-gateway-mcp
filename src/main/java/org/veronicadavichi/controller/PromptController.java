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
import org.veronicadavichi.entity.McpPrompt;
import org.veronicadavichi.service.PromptService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public ApiResponse<PageResult<McpPrompt>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<McpPrompt> records = promptService.list(name, serverId, enabled, page, size);
        return ApiResponse.success(PageResult.of(records, records.size(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<McpPrompt> getById(@PathVariable Long id) {
        McpPrompt p = promptService.getById(id);
        if (p == null) return ApiResponse.error("NOT_FOUND", "Prompt 不存在：" + id);
        return ApiResponse.success(p);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<McpPrompt>> create(@RequestBody McpPrompt prompt) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(promptService.create(prompt)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("NAME_EXISTS", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<McpPrompt>> update(@PathVariable Long id, @RequestBody McpPrompt prompt) {
        try {
            return ResponseEntity.ok(ApiResponse.success(promptService.update(id, prompt)));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("NAME_EXISTS", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            promptService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage().contains("不存在") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 导入 Prompts JSON - 解析预览
     */
    @PostMapping("/import")
    public ApiResponse<ImportPreview> importJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serverId") Long serverId) throws IOException {

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        ImportPreview preview = promptService.parseJson(content, serverId);
        return ApiResponse.success(preview);
    }

    /**
     * 确认导入
     */
    @PostMapping("/confirm-import")
    public ApiResponse<Integer> confirmImport(@RequestBody ImportConfirmRequest request) {
        int count = promptService.confirmImport(request);
        return ApiResponse.success(count);
    }

    /**
     * 导出 Prompts JSON
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportJson(@RequestBody(required = false) ExportRequest request) {
        String json = promptService.exportJson(request != null ? request.getIds() : null);

        // 生成含时间戳的文件名
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = "prompts-export-" + timestamp + ".json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 处理中文文件名，使用 URLEncoder 编码
        try {
            String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        } catch (Exception e) {
            headers.setContentDispositionFormData("attachment", fileName);
        }

        return new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }
}
