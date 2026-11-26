package org.veronicadavichi.dto;

import lombok.Data;

import java.util.List;

/**
 * 导出请求
 */
@Data
public class ExportRequest {

    private List<Long> ids;
}
