package org.veronicadavichi.dto;

import lombok.Data;

import java.util.List;

/**
 * 确认导入请求
 */
@Data
public class ImportConfirmRequest {

    private Long serverId;
    private List<ImportItem> items;

    /**
     * 导入项配置
     */
    @Data
    public static class ImportItem {
        private String type; // "tool" or "resource"
        private String name;
        private String path;
        private String method;
        private String inputSchema;
        private String headerMappings;
        private String template;
        private String description;
        private boolean skip; // true=跳过，false=导入
        private String conflictAction; // "skip" or "override"
    }
}
