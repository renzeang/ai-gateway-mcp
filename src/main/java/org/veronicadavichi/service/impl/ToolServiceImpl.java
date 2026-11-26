package org.veronicadavichi.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.mapper.McpToolMapper;
import org.veronicadavichi.mapper.McpResourceMapper;
import org.veronicadavichi.service.ResourceService;
import org.veronicadavichi.service.ToolService;
import org.veronicadavichi.util.OpenApiParser;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MCP Tool 服务实现
 */
@Service
public class  ToolServiceImpl implements ToolService {

    private final McpToolMapper toolMapper;
    private final McpResourceMapper resourceMapper;
    private final ResourceService resourceService;

    public ToolServiceImpl(McpToolMapper toolMapper, McpResourceMapper resourceMapper, @Lazy ResourceService resourceService) {
        this.toolMapper = toolMapper;
        this.resourceMapper = resourceMapper;
        this.resourceService = resourceService;
    }

    @Override
    public List<McpTool> list(String name, String method, Long serverId, Boolean enabled, int page, int size) {
        int offset = (page - 1) * size;
        return toolMapper.selectByCondition(name, method, serverId, enabled, offset, size);
    }

    @Override
    public McpTool getById(Long id) {
        return toolMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpTool create(McpTool tool) {
        List<McpTool> existing = toolMapper.selectListByQuery(QueryWrapper.create()
                .eq("name", tool.getName())
        );
        if (existing != null && !existing.isEmpty()) {
            throw new IllegalArgumentException("工具名称已存在：" + tool.getName());
        }
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());
        toolMapper.insert(tool);
        return tool;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpTool update(Long id, McpTool tool) {
        McpTool existing = toolMapper.selectOneById(id);
        if (existing == null) {
            throw new IllegalArgumentException("工具不存在：" + id);
        }
        if (!existing.getName().equals(tool.getName())) {
            List<McpTool> duplicate = toolMapper.selectListByQuery(QueryWrapper.create()
                    .eq("name", tool.getName())
                    .ne("id", id)
            );
            if (duplicate != null && !duplicate.isEmpty()) {
                throw new IllegalArgumentException("工具名称已存在：" + tool.getName());
            }
        }
        tool.setId(id);
        tool.setUpdatedAt(LocalDateTime.now());
        toolMapper.update(tool);
        return toolMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        toolMapper.deleteById(id);
    }

    @Override
    public ImportPreview parseOpenApi(String openApiJson, Long serverId) {
        List<McpTool> tools = OpenApiParser.parseToTools(openApiJson);
        List<McpResource> resources = OpenApiParser.parseToResources(openApiJson);

        tools.forEach(tool -> tool.setServerId(serverId));
        resources.forEach(resource -> resource.setServerId(serverId));

        // 查询当前 Server 已有的 Tools 和 Resources，用于检测重复
        List<McpTool> existingTools = getByServerId(serverId);
        List<McpResource> existingResources = resourceService.getByServerId(serverId);

        // 使用新的 of 方法，自动检测重复
        ImportPreview preview = ImportPreview.of(tools, resources, existingTools, existingResources);

        // 为重复的条目设置 conflictAction
        for (ImportPreview.ImportItem toolItem : preview.getTools()) {
            if (toolItem.isDuplicate()) {
                toolItem.setConflictAction("skip");
            }
        }

        for (ImportPreview.ImportItem resourceItem : preview.getResources()) {
            if (resourceItem.isDuplicate()) {
                resourceItem.setConflictAction("skip");
            }
        }

        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmImport(ImportConfirmRequest request) {
        int importedCount = 0;
        System.out.println("[ToolServiceImpl] 开始导入，条目数：" + request.getItems().size());

        for (ImportConfirmRequest.ImportItem item : request.getItems()) {
            if (item.isSkip()) {
                continue;
            }

            if ("tool".equals(item.getType())) {
                // 检测重复：同 serverId + 同 path + 同 method + 同 inputSchema
                List<McpTool> existing = toolMapper.selectListByQuery(QueryWrapper.create()
                        .eq("server_id", request.getServerId())
                        .eq("path", item.getPath())
                        .eq("method", item.getMethod())
                        .eq("input_schema", item.getInputSchema() != null ? item.getInputSchema() : "{}")
                );
                if (existing != null && !existing.isEmpty()) {
                    // 完全重复，跳过
                    System.out.println("[ToolServiceImpl] 检测到重复 Tool: " + item.getName() + " (path=" + item.getPath() + ", method=" + item.getMethod() + ")");
                    continue;
                }

                // 检查同名不同内容的情况
                List<McpTool> sameName = toolMapper.selectListByQuery(QueryWrapper.create()
                        .eq("server_id", request.getServerId())
                        .eq("name", item.getName())
                );
                if (sameName != null && !sameName.isEmpty()) {
                    if (!"override".equals(item.getConflictAction())) {
                        continue;
                    }
                    McpTool updateTool = sameName.get(0);
                    updateTool.setPath(item.getPath());
                    updateTool.setMethod(item.getMethod());
                    updateTool.setDescription(item.getDescription());
                    if (item.getInputSchema() != null) {
                        updateTool.setInputSchema(item.getInputSchema());
                    }
                    if (item.getHeaderMappings() != null) {
                        updateTool.setHeaderMappings(item.getHeaderMappings());
                    }
                    updateTool.setUpdatedAt(LocalDateTime.now());
                    toolMapper.update(updateTool);
                } else {
                    McpTool tool = new McpTool();
                    tool.setServerId(request.getServerId());
                    tool.setName(item.getName());
                    tool.setPath(item.getPath());
                    tool.setMethod(item.getMethod());
                    tool.setDescription(item.getDescription());
                    tool.setInputSchema(item.getInputSchema() != null ? item.getInputSchema() : "{}");
                    tool.setHeaderMappings(item.getHeaderMappings());
                    tool.setEnabled(true);
                    tool.setCreatedAt(LocalDateTime.now());
                    tool.setUpdatedAt(LocalDateTime.now());
                    toolMapper.insert(tool);
                }
                importedCount++;

            } else if ("resource".equals(item.getType())) {
                List<McpResource> existing = resourceMapper.selectListByQuery(QueryWrapper.create()
                        .eq("uri", item.getName())
                );
                if (existing != null && !existing.isEmpty()) {
                    if (!"override".equals(item.getConflictAction())) {
                        continue;
                    }
                    McpResource updateResource = existing.get(0);
                    updateResource.setPath(item.getPath());
                    updateResource.setMethod(item.getMethod());
                    updateResource.setDescription(item.getDescription());
                    if (item.getTemplate() != null) {
                        updateResource.setTemplate(item.getTemplate());
                    }
                    updateResource.setUpdatedAt(LocalDateTime.now());
                    resourceMapper.update(updateResource);
                } else {
                    McpResource resource = new McpResource();
                    resource.setServerId(request.getServerId());
                    resource.setUri(item.getName());
                    resource.setName(item.getName());
                    resource.setPath(item.getPath());
                    resource.setMethod(item.getMethod());
                    resource.setDescription(item.getDescription());
                    resource.setTemplate(item.getTemplate());
                    resource.setEnabled(true);
                    resource.setCreatedAt(LocalDateTime.now());
                    resource.setUpdatedAt(LocalDateTime.now());
                    resourceMapper.insert(resource);
                }
                importedCount++;
            }
        }

        return importedCount;
    }

    @Override
    public String exportOpenApi(List<Long> ids) {
        List<McpTool> tools;
        if (ids == null || ids.isEmpty()) {
            tools = toolMapper.selectAll();
        } else {
            tools = toolMapper.selectListByIds(ids);
        }
        return OpenApiParser.generateFromTools(tools, null, null);
    }

    @Override
    public List<McpTool> getByServerId(Long serverId) {
        return toolMapper.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create().eq("server_id", serverId)
        );
    }
}
