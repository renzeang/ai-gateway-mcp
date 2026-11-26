package org.veronicadavichi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Server 关联数据统计
 */
@Data
@AllArgsConstructor
public class ServerRelationCount {
    private final long toolCount;
    private final long resourceCount;
    private final long promptCount;

    public long getTotal() { return toolCount + resourceCount + promptCount; }
}
