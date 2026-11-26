package org.veronicadavichi.service;

import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpPrompt;

import java.util.List;

public interface PromptService {
    List<McpPrompt> list(String name, Long serverId, Boolean enabled, int page, int size);
    McpPrompt getById(Long id);
    McpPrompt create(McpPrompt prompt);
    McpPrompt update(Long id, McpPrompt prompt);
    void delete(Long id);

    /**
     * 解析 Prompts JSON 为预览数据
     */
    ImportPreview parseJson(String json, Long serverId);

    /**
     * 确认导入 Prompts
     */
    int confirmImport(ImportConfirmRequest request);

    /**
     * 导出 Prompts 为 JSON
     */
    String exportJson(List<Long> ids);
}
