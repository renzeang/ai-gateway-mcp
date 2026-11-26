package org.veronicadavichi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veronicadavichi.dto.ImportConfirmRequest;
import org.veronicadavichi.dto.ImportPreview;
import org.veronicadavichi.entity.McpPrompt;
import org.veronicadavichi.mapper.McpPromptMapper;
import org.veronicadavichi.service.PromptService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptServiceImpl implements PromptService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final McpPromptMapper promptMapper;

    public PromptServiceImpl(McpPromptMapper promptMapper) {
        this.promptMapper = promptMapper;
    }

    @Override
    public List<McpPrompt> list(String name, Long serverId, Boolean enabled, int page, int size) {
        int offset = (page - 1) * size;
        return promptMapper.selectByCondition(name, serverId, enabled, offset, size);
    }

    @Override
    public McpPrompt getById(Long id) {
        return promptMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpPrompt create(McpPrompt prompt) {
        List<McpPrompt> existing = promptMapper.selectListByQuery(QueryWrapper.create()
                .eq("server_id", prompt.getServerId())
                .eq("name", prompt.getName())
        );
        if (existing != null && !existing.isEmpty()) {
            throw new IllegalArgumentException("提示词名称已存在：" + prompt.getName());
        }
        prompt.setCreatedAt(LocalDateTime.now());
        prompt.setUpdatedAt(LocalDateTime.now());
        promptMapper.insert(prompt);
        return prompt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpPrompt update(Long id, McpPrompt prompt) {
        McpPrompt existing = promptMapper.selectOneById(id);
        if (existing == null) {
            throw new IllegalArgumentException("提示词不存在：" + id);
        }
        if (!existing.getName().equals(prompt.getName()) || !existing.getServerId().equals(prompt.getServerId())) {
            List<McpPrompt> duplicate = promptMapper.selectListByQuery(QueryWrapper.create()
                    .eq("server_id", prompt.getServerId())
                    .eq("name", prompt.getName())
                    .ne("id", id)
            );
            if (duplicate != null && !duplicate.isEmpty()) {
                throw new IllegalArgumentException("提示词名称已存在：" + prompt.getName());
            }
        }
        prompt.setId(id);
        prompt.setUpdatedAt(LocalDateTime.now());
        promptMapper.update(prompt);
        return promptMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        promptMapper.deleteById(id);
    }

    @Override
    public ImportPreview parseJson(String json, Long serverId) {
        List<McpPrompt> prompts = new ArrayList<>();
        try {
            JsonNode root = JSON_MAPPER.readTree(json);
            JsonNode promptsNode = root.get("prompts");
            if (promptsNode != null && promptsNode.isArray()) {
                for (JsonNode p : promptsNode) {
                    McpPrompt prompt = new McpPrompt();
                    prompt.setServerId(serverId);
                    prompt.setName(p.has("name") ? p.get("name").asText() : "");
                    prompt.setDescription(p.has("description") ? p.get("description").asText(null) : null);
                    prompt.setTemplate(p.has("template") ? p.get("template").asText() : "");
                    if (p.has("arguments")) {
                        prompt.setArguments(JSON_MAPPER.writeValueAsString(p.get("arguments")));
                    }
                    prompt.setEnabled(true);
                    prompts.add(prompt);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 Prompts JSON 失败：" + e.getMessage(), e);
        }

        ImportPreview preview = new ImportPreview();
        preview.setTools(new ArrayList<>());
        preview.setResources(prompts.stream().map(p -> {
            ImportPreview.ImportItem item = new ImportPreview.ImportItem();
            item.setName(p.getName());
            item.setDescription(p.getDescription());
            return item;
        }).collect(Collectors.toList()));

        for (ImportPreview.ImportItem item : preview.getResources()) {
            List<McpPrompt> existing = promptMapper.selectListByQuery(QueryWrapper.create()
                    .eq("name", item.getName())
            );
            if (existing != null && !existing.isEmpty()) {
                item.setDuplicate(true);
                item.setConflictAction("skip");
            }
        }

        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmImport(ImportConfirmRequest request) {
        int importedCount = 0;
        for (ImportConfirmRequest.ImportItem item : request.getItems()) {
            if (item.isSkip()) {
                continue;
            }
            List<McpPrompt> existing = promptMapper.selectListByQuery(QueryWrapper.create()
                    .eq("name", item.getName())
            );
            if (existing != null && !existing.isEmpty()) {
                if (!"override".equals(item.getConflictAction())) {
                    continue;
                }
                McpPrompt updatePrompt = existing.get(0);
                updatePrompt.setDescription(item.getDescription());
                updatePrompt.setUpdatedAt(LocalDateTime.now());
                promptMapper.update(updatePrompt);
            } else {
                McpPrompt prompt = new McpPrompt();
                prompt.setServerId(request.getServerId());
                prompt.setName(item.getName());
                prompt.setDescription(item.getDescription());
                prompt.setEnabled(true);
                prompt.setCreatedAt(LocalDateTime.now());
                prompt.setUpdatedAt(LocalDateTime.now());
                promptMapper.insert(prompt);
            }
            importedCount++;
        }
        return importedCount;
    }

    @Override
    public String exportJson(List<Long> ids) {
        List<McpPrompt> prompts;
        if (ids == null || ids.isEmpty()) {
            prompts = promptMapper.selectAll();
        } else {
            prompts = promptMapper.selectListByIds(ids);
        }
        try {
            ObjectNode root = JSON_MAPPER.createObjectNode();
            root.put("exportedAt", LocalDateTime.now().toString());
            ArrayNode promptsArray = root.putArray("prompts");
            for (McpPrompt prompt : prompts) {
                ObjectNode p = promptsArray.addObject();
                p.put("name", prompt.getName());
                if (prompt.getDescription() != null) {
                    p.put("description", prompt.getDescription());
                }
                p.put("template", prompt.getTemplate());
                if (prompt.getArguments() != null) {
                    p.set("arguments", JSON_MAPPER.readTree(prompt.getArguments()));
                }
            }
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("导出 Prompts 失败：" + e.getMessage(), e);
        }
    }
}
