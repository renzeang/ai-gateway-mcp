package org.veronicadavichi.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.entity.McpTool;
import org.veronicadavichi.mapper.McpResourceMapper;
import org.veronicadavichi.service.ResourceService;
import org.veronicadavichi.service.ToolService;
import org.veronicadavichi.util.OpenApiParser;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceServiceImpl implements ResourceService {

    private final McpResourceMapper resourceMapper;
    private final ToolService toolService;

    public ResourceServiceImpl(McpResourceMapper resourceMapper, @Lazy ToolService toolService) {
        this.resourceMapper = resourceMapper;
        this.toolService = toolService;
    }

    @Override
    public List<McpResource> list(String name, String method, Long serverId, Boolean enabled, int page, int size) {
        int offset = (page - 1) * size;
        return resourceMapper.selectByCondition(name, method, serverId, enabled, offset, size);
    }

    @Override
    public McpResource getById(Long id) {
        return resourceMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpResource create(McpResource resource) {
        List<McpResource> existing = resourceMapper.selectListByQuery(QueryWrapper.create()
                .eq("uri", resource.getUri())
        );
        if (existing != null && !existing.isEmpty()) {
            throw new IllegalArgumentException("Resource URI 已存在：" + resource.getUri());
        }
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return resource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpResource update(Long id, McpResource resource) {
        McpResource existing = resourceMapper.selectOneById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Resource 不存在：" + id);
        }
        if (!existing.getUri().equals(resource.getUri())) {
            List<McpResource> duplicate = resourceMapper.selectListByQuery(QueryWrapper.create()
                    .eq("uri", resource.getUri())
                    .ne("id", id)
            );
            if (duplicate != null && !duplicate.isEmpty()) {
                throw new IllegalArgumentException("Resource URI 已存在：" + resource.getUri());
            }
        }
        resource.setId(id);
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.update(resource);
        return resourceMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        resourceMapper.deleteById(id);
    }

    @Override
    public ImportPreview parseOpenApi(String openApiJson, Long serverId) {
        List<McpTool> tools = OpenApiParser.parseToTools(openApiJson);
        List<McpResource> resources = OpenApiParser.parseToResources(openApiJson);

        resources.forEach(resource -> resource.setServerId(serverId));

        // 查询当前 Server 已有的 Tools 和 Resources，用于检测重复
        List<McpTool> existingTools = toolService.getByServerId(serverId);
        List<McpResource> existingResources = getByServerId(serverId);

        // 使用新的 of 方法，自动检测重复
        ImportPreview preview = ImportPreview.of(tools, resources, existingTools, existingResources);

        // 为重复的条目设置 conflictAction
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
        System.out.println("[ResourceServiceImpl] 开始导入，条目数：" + request.getItems().size());

        for (ImportConfirmRequest.ImportItem item : request.getItems()) {
            if (item.isSkip()) {
                continue;
            }

            if ("resource".equals(item.getType())) {
                // 检测重复：同 serverId + 同 path + 同 method + 同 template
                List<McpResource> existing = resourceMapper.selectListByQuery(QueryWrapper.create()
                        .eq("server_id", request.getServerId())
                        .eq("path", item.getPath())
                        .eq("method", item.getMethod())
                        .eq("template", item.getTemplate() != null ? item.getTemplate() : "")
                );
                if (existing != null && !existing.isEmpty()) {
                    // 完全重复，跳过
                    System.out.println("[ResourceServiceImpl] 检测到重复 Resource: " + item.getName() + " (path=" + item.getPath() + ", method=" + item.getMethod() + ")");
                    continue;
                }

                // 检查同名不同内容的情况
                List<McpResource> sameName = resourceMapper.selectListByQuery(QueryWrapper.create()
                        .eq("server_id", request.getServerId())
                        .eq("uri", item.getName())
                );
                if (sameName != null && !sameName.isEmpty()) {
                    if (!"override".equals(item.getConflictAction())) {
                        continue;
                    }
                    McpResource updateResource = sameName.get(0);
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
        List<McpResource> resources;
        if (ids == null || ids.isEmpty()) {
            resources = resourceMapper.selectAll();
        } else {
            resources = resourceMapper.selectListByIds(ids);
        }
        return OpenApiParser.generateFromResources(resources, null, null);
    }

    @Override
    public List<McpResource> getByServerId(Long serverId) {
        return resourceMapper.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create().eq("server_id", serverId)
        );
    }
}
