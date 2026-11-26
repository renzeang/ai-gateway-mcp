package org.veronicadavichi.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veronicadavichi.dto.ServerRelationCount;
import org.veronicadavichi.entity.McpServer;
import java.time.LocalDateTime;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.entity.McpPrompt;
import org.veronicadavichi.mapper.McpServerMapper;
import org.veronicadavichi.mapper.McpToolMapper;
import org.veronicadavichi.mapper.McpResourceMapper;
import org.veronicadavichi.mapper.McpPromptMapper;
import org.veronicadavichi.service.ServerService;
import org.veronicadavichi.util.OpenApiParser;

import java.util.List;

/**
 * MCP Server 服务实现
 */
@Service
public class ServerServiceImpl implements ServerService {

    private final McpServerMapper serverMapper;
    private final McpToolMapper toolMapper;
    private final McpResourceMapper resourceMapper;
    private final McpPromptMapper promptMapper;

    public ServerServiceImpl(McpServerMapper serverMapper, McpToolMapper toolMapper,
                             McpResourceMapper resourceMapper, McpPromptMapper promptMapper) {
        this.serverMapper = serverMapper;
        this.toolMapper = toolMapper;
        this.resourceMapper = resourceMapper;
        this.promptMapper = promptMapper;
    }

    @Override
    public List<McpServer> list(String name, Boolean enabled, int page, int size) {
        int offset = (page - 1) * size;
        return serverMapper.selectByCondition(name, enabled, offset, size);
    }

    @Override
    public McpServer getById(Long id) {
        return serverMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer create(McpServer server) {
        // 校验 Base URL 格式和端口号
        validateBaseUrl(server.getBaseUrl());

        List<McpServer> existing = serverMapper.selectListByQuery(QueryWrapper.create()
                .eq("name", server.getName())
        );
        if (existing != null && !existing.isEmpty()) {
            throw new IllegalArgumentException("服务名称已存在：" + server.getName());
        }
        server.setCreatedAt(LocalDateTime.now());
        server.setUpdatedAt(LocalDateTime.now());
        serverMapper.insert(server);
        return server;
    }

    /**
     * 校验 Base URL 格式和端口号
     */
    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL 不能为空");
        }

        // 使用 Java URL 类解析并校验
        java.net.URL url;
        try {
            url = new java.net.URL(baseUrl);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Base URL 格式不正确：" + baseUrl);
        }

        // 校验端口号
        int port = url.getPort();
        if (port != -1 && (port < 0 || port > 65535)) {
            throw new IllegalArgumentException("Base URL 端口号必须在 0-65535 之间，当前端口：" + port);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer update(Long id, McpServer server) {
        // 校验 Base URL 格式和端口号
        validateBaseUrl(server.getBaseUrl());

        McpServer existing = serverMapper.selectOneById(id);
        if (existing == null) {
            throw new IllegalArgumentException("服务不存在：" + id);
        }
        if (!existing.getName().equals(server.getName())) {
            List<McpServer> duplicate = serverMapper.selectListByQuery(QueryWrapper.create()
                    .eq("name", server.getName())
                    .ne("id", id)
            );
            if (duplicate != null && !duplicate.isEmpty()) {
                throw new IllegalArgumentException("服务名称已存在：" + server.getName());
            }
        }
        server.setId(id);
        server.setUpdatedAt(LocalDateTime.now());
        serverMapper.update(server);
        return serverMapper.selectOneById(id);
    }

    @Override
    public org.veronicadavichi.dto.ServerRelationCount countRelations(Long serverId) {
        long toolCount = toolMapper.selectCountByQuery(QueryWrapper.create().eq("server_id", serverId));
        long resourceCount = resourceMapper.selectCountByQuery(QueryWrapper.create().eq("server_id", serverId));
        long promptCount = promptMapper.selectCountByQuery(QueryWrapper.create().eq("server_id", serverId));
        return new org.veronicadavichi.dto.ServerRelationCount(toolCount, resourceCount, promptCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, boolean cascade) {
        // 检查是否有关联的数据
        ServerRelationCount counts = countRelations(id);

        if (!cascade && (counts.getToolCount() > 0 || counts.getResourceCount() > 0 || counts.getPromptCount() > 0)) {
            throw new IllegalArgumentException(String.format(
                "该服务下仍存在关联数据：Tools %d 个，Resources %d 个，Prompts %d 个。请先删除或勾选'级联删除'选项",
                counts.getToolCount(), counts.getResourceCount(), counts.getPromptCount()));
        }

        // 级联删除：先删除关联数据
        if (cascade) {
            if (counts.getToolCount() > 0) {
                toolMapper.deleteByQuery(QueryWrapper.create().eq("server_id", id));
            }
            if (counts.getResourceCount() > 0) {
                resourceMapper.deleteByQuery(QueryWrapper.create().eq("server_id", id));
            }
            if (counts.getPromptCount() > 0) {
                promptMapper.deleteByQuery(QueryWrapper.create().eq("server_id", id));
            }
        }

        serverMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        delete(id, false);
    }

    @Override
    public String exportByServerId(Long serverId, List<Long> toolIds, List<Long> resourceIds) {
        // 查询 Server 信息
        McpServer server = serverMapper.selectOneById(serverId);
        if (server == null) {
            throw new IllegalArgumentException("服务不存在：" + serverId);
        }

        // 查询 Tools
        List<McpTool> tools;
        if (toolIds == null || toolIds.isEmpty()) {
            // 查询该 Server 下所有 Tools
            tools = toolMapper.selectListByQuery(
                com.mybatisflex.core.query.QueryWrapper.create()
                    .eq("server_id", serverId)
            );
        } else {
            // 按 ID 列表查询
            tools = toolMapper.selectListByIds(toolIds);
        }

        // 查询 Resources
        List<McpResource> resources;
        if (resourceIds == null || resourceIds.isEmpty()) {
            // 查询该 Server 下所有 Resources
            resources = resourceMapper.selectListByQuery(
                com.mybatisflex.core.query.QueryWrapper.create()
                    .eq("server_id", serverId)
            );
        } else {
            // 按 ID 列表查询
            resources = resourceMapper.selectListByIds(resourceIds);
        }

        // 生成 OpenAPI JSON（带 x-mcp-* 扩展）
        return OpenApiParser.generateOpenApi(tools, resources,
            serverId != null ? serverId.intValue() : null, server.getName());
    }
}
