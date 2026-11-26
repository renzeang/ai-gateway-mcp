package org.veronicadavichi.controller;

import org.springframework.web.bind.annotation.*;
import org.veronicadavichi.dto.ApiResponse;
import org.veronicadavichi.dto.PageResult;
import org.veronicadavichi.entity.McpAuditLog;
import org.veronicadavichi.service.AuditLogService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<McpAuditLog>> list(
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) Boolean isError,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<McpAuditLog> records = auditLogService.list(targetName, requestType, isError, startDate, endDate, page, pageSize);
        return ApiResponse.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<McpAuditLog> getById(@PathVariable Long id) {
        McpAuditLog log = auditLogService.getById(id);
        if (log == null) return ApiResponse.error("NOT_FOUND", "审计日志不存在：" + id);
        return ApiResponse.success(log);
    }
}
