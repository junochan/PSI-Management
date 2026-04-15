package com.smartims.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品 Excel 异步导入任务状态（供轮询）
 */
@Data
public class ProductImportTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private ProductImportTaskStatus status;
    private String message;

    private Integer totalRows;
    private Integer successCount;
    private Integer failCount;

    /** 行级错误摘要，最多保留若干条 */
    private List<String> errors = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
