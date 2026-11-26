package org.veronicadavichi.service;

import org.veronicadavichi.dto.ServerRelationCount;
import org.veronicadavichi.entity.McpServer;
import java.util.List;

/**
 * MCP Server 服务接口
 */
public interface ServerService {

    /**
     * 分页查询服务器列表
     */
    List<McpServer> list(String name, Boolean enabled, int page, int size);

    /**
     * 获取服务器详情
     */
    McpServer getById(Long id);

    /**
     * 创建服务器
     */
    McpServer create(McpServer server);

    /**
     * 更新服务器
     */
    McpServer update(Long id, McpServer server);

    /**
     * 删除服务器
     */
    void delete(Long id);

    /**
     * 删除服务器（支持级联删除）
     * @param id 服务器 ID
     * @param cascade 是否级联删除关联的 Tools/Resources/Prompts
     */
    void delete(Long id, boolean cascade);

    /**
     * 统计 Server 下关联的 Tools/Resources/Prompts 数量
     */
    ServerRelationCount countRelations(Long id);

    /**
     * 按 Server ID 导出 Tools 和 Resources 为 OpenAPI JSON
     * @param serverId Server ID
     * @param toolIds Tool ID 列表（可选，null 表示全选）
     * @param resourceIds Resource ID 列表（可选，null 表示全选）
     * @return OpenAPI JSON 字符串
     */
    String exportByServerId(Long serverId, List<Long> toolIds, List<Long> resourceIds);
}
