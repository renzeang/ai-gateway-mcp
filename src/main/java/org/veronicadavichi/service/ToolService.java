package org.veronicadavichi.service;

import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpTool;

import java.util.List;

/**
 * MCP Tool 服务接口
 */
public interface ToolService {

    List<McpTool> list(String name, String method, Long serverId, Boolean enabled, int page, int size);

    McpTool getById(Long id);

    McpTool create(McpTool tool);

    McpTool update(Long id, McpTool tool);

    void delete(Long id);

    /**
     * 解析 OpenAPI 为预览数据
     */
    ImportPreview parseOpenApi(String openApiJson, Long serverId);

    /**
     * 确认导入 Tools
     */
    int confirmImport(ImportConfirmRequest request);

    /**
     * 导出 Tools 为 OpenAPI JSON
     */
    String exportOpenApi(List<Long> ids);

    /**
     * 根据 ServerId 查询所有 Tools
     */
    List<McpTool> getByServerId(Long serverId);
}
