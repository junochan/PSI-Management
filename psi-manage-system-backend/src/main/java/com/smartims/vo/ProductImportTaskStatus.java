package com.smartims.vo;

/**
 * 商品 Excel 异步导入任务状态
 */
public enum ProductImportTaskStatus {
    /** 已入队，等待执行 */
    QUEUED,
    /** 正在解析与写入 */
    RUNNING,
    /** 已结束（含部分行失败） */
    COMPLETED,
    /** 整体失败（如文件无法解析） */
    FAILED
}
