package com.smartims.controller;

import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.entity.SysOperationLog;
import com.smartims.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "操作日志", description = "操作日志相关接口")
@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "查询操作日志列表")
    @GetMapping
    public Result<PageResult<SysOperationLog>> getLogList(PageQuery pageQuery) {
        PageResult<SysOperationLog> result = operationLogService.getLogList(pageQuery);
        return Result.success(result);
    }

}