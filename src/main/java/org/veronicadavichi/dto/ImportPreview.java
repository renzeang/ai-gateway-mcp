package org.veronicadavichi.dto;

import lombok.Data;
import org.veronicadavichi.entity.McpResource;
import org.veronicadavichi.entity.McpTool;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 导入预览数据
 */
@Data
public class ImportPreview {

    private List<ImportItem> tools;
    private List<ImportItem> resources;

    public static ImportPreview of(List<McpTool> tools, List<McpResource> resources,
                                    List<McpTool> existingTools, List<McpResource> existingResources) {
        ImportPreview preview = new ImportPreview();
        preview.setTools(mapToItemsFromTools(tools, existingTools, "tool"));
        preview.setResources(mapToItemsFromResources(resources, existingResources, "resource"));
        return preview;
    }

    private static List<ImportItem> mapToItemsFromTools(List<McpTool> tools, List<McpTool> existingTools, String type) {
        Set<String> existingKeys = existingTools.stream()
            .map(t -> t.getServerId() + "|" + t.getPath() + "|" + t.getMethod() + "|" +
                (t.getInputSchema() != null ? t.getInputSchema() : "{}"))
            .collect(Collectors.toSet());

        return tools.stream()
            .map(tool -> {
                ImportItem item = new ImportItem();
                item.setType(type);
                item.setName(tool.getName());
                item.setDescription(tool.getDescription());
                item.setPath(tool.getPath());
                item.setMethod(tool.getMethod());
                item.setInputSchema(tool.getInputSchema());
                item.setHeaderMappings(tool.getHeaderMappings());

                String key = tool.getServerId() + "|" + tool.getPath() + "|" + tool.getMethod() + "|" +
                    (tool.getInputSchema() != null ? tool.getInputSchema() : "{}");
                item.setDuplicate(existingKeys.contains(key));

                return item;
            })
            .toList();
    }

    private static List<ImportItem> mapToItemsFromResources(List<McpResource> resources, List<McpResource> existingResources, String type) {
        Set<String> existingKeys = existingResources.stream()
            .map(r -> r.getServerId() + "|" + r.getPath() + "|" + r.getMethod() + "|" +
                (r.getTemplate() != null ? r.getTemplate() : ""))
            .collect(Collectors.toSet());

        return resources.stream()
            .map(resource -> {
                ImportItem item = new ImportItem();
                item.setType(type);
                item.setName(resource.getUri());
                item.setDescription(resource.getName());
                item.setPath(resource.getPath());
                item.setMethod(resource.getMethod());
                item.setTemplate(resource.getTemplate());

                String key = resource.getServerId() + "|" + resource.getPath() + "|" + resource.getMethod() + "|" +
                    (resource.getTemplate() != null ? resource.getTemplate() : "");
                item.setDuplicate(existingKeys.contains(key));

                return item;
            })
            .toList();
    }

    /**
     * 导入项
     */
    @Data
    public static class ImportItem {
        private String type;
        private String name;
        private String description;
        private String path;
        private String method;
        private String inputSchema;
        private String headerMappings;
        private String template;
        private boolean duplicate;
        private String conflictAction;
    }
}
