package org.veronicadavichi.service;

import org.veronicadavichi.entity.McpAuditLog;
import java.util.List;

public interface AuditLogService {
    List<McpAuditLog> list(String targetName, String requestType, Boolean isError, String startDate, String endDate, int page, int pageSize);
    McpAuditLog getById(Long id);
}
