package org.veronicadavichi.service;

import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpResource;

import java.util.List;

public interface ResourceService {
    List<McpResource> list(String name, String method, Long serverId, Boolean enabled, int page, int size);
    McpResource getById(Long id);
    McpResource create(McpResource resource);
    McpResource update(Long id, McpResource resource);
    void delete(Long id);

    /**
     * 解析 OpenAPI 为预览数据
     */
    ImportPreview parseOpenApi(String openApiJson, Long serverId);

    /**
     * 确认导入 Resources
     */
    int confirmImport(ImportConfirmRequest request);

    /**
     * 导出 Resources 为 OpenAPI JSON
     */
    String exportOpenApi(List<Long> ids);

    /**
     * 根据 ServerId 查询所有 Resources
     */
    List<McpResource> getByServerId(Long serverId);
}
