package org.veronicadavichi.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.veronicadavichi.entity.McpAuditLog;
import org.veronicadavichi.mapper.McpAuditLogMapper;
import org.veronicadavichi.service.AuditLogService;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final McpAuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(McpAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public List<McpAuditLog> list(String targetName, String requestType, Boolean isError, String startDate, String endDate, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        QueryWrapper query = QueryWrapper.create()
                .orderBy("created_at", true);

        if (targetName != null && !targetName.isEmpty()) {
            query.and("target_name", "like", "%" + targetName + "%");
        }
        if (requestType != null && !requestType.isEmpty()) {
            query.and("request_type", "like", "%" + requestType + "%");
        }
        if (isError != null) {
            query.and("is_error", "=", isError ? 1 : 0);
        }
        if (startDate != null && !startDate.isEmpty()) {
            query.and("DATE(created_at)", ">=", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.and("DATE(created_at)", "<=", endDate);
        }

        query.limit(offset, pageSize);

        List<McpAuditLog> result = auditLogMapper.selectListByQuery(query);
        System.out.println(">>> AuditLogService.list: targetName=" + targetName + ", requestType=" + requestType + ", result size=" + (result != null ? result.size() : "null"));
        if (result != null && !result.isEmpty()) {
            System.out.println(">>> First record: id=" + result.get(0).getId() + ", type=" + result.get(0).getRequestType());
        }
        return result;
    }

    @Override
    public McpAuditLog getById(Long id) {
        return auditLogMapper.selectOneById(id);
    }
}
