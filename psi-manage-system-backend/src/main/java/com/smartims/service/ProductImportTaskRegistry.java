package com.smartims.service;

import com.smartims.exception.BusinessException;
import com.smartims.vo.ProductImportTaskStatus;
import com.smartims.vo.ProductImportTaskVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中保存异步导入任务状态（单机）；集群需改为 Redis 等。
 */
@Component
public class ProductImportTaskRegistry {

    private static final int MAX_TASKS = 500;

    private final ConcurrentHashMap<String, ProductImportTaskVO> tasks = new ConcurrentHashMap<>();

    public String createTask() {
        if (tasks.size() >= MAX_TASKS) {
            throw new BusinessException("当前导入任务过多，请稍后再试");
        }
        String jobId = UUID.randomUUID().toString();
        ProductImportTaskVO vo = new ProductImportTaskVO();
        vo.setJobId(jobId);
        vo.setStatus(ProductImportTaskStatus.QUEUED);
        vo.setSuccessCount(0);
        vo.setFailCount(0);
        vo.setTotalRows(0);
        vo.setCreatedAt(LocalDateTime.now());
        vo.setMessage("任务已排队");
        tasks.put(jobId, vo);
        return jobId;
    }

    public ProductImportTaskVO get(String jobId) {
        return tasks.get(jobId);
    }

    public void update(String jobId, ProductImportTaskVO vo) {
        tasks.put(jobId, vo);
    }

    public ProductImportTaskVO getRequired(String jobId) {
        ProductImportTaskVO vo = tasks.get(jobId);
        if (vo == null) {
            throw new BusinessException("导入任务不存在或已过期");
        }
        return vo;
    }
}
