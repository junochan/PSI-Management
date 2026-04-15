package com.smartims.service;

import com.smartims.exception.BusinessException;
import com.smartims.vo.ProductImportSubmitVO;
import com.smartims.vo.ProductImportTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 商品 Excel 异步导入：提交任务与查询进度
 */
@Service
@RequiredArgsConstructor
public class ProductImportService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ProductImportTaskRegistry taskRegistry;
    private final ProductImportProcessor importProcessor;

    public ProductImportSubmitVO submit(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            throw new BusinessException("文件名无效");
        }
        String lower = original.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new BusinessException("仅支持 .xlsx 或 .xls 格式");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("文件大小不能超过 10MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("读取上传文件失败");
        }

        String jobId = taskRegistry.createTask();
        importProcessor.runImport(jobId, bytes);
        return new ProductImportSubmitVO(jobId, "任务已提交，请轮询查询进度");
    }

    public ProductImportTaskVO getTask(String jobId) {
        return taskRegistry.getRequired(jobId);
    }
}
