package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.Result;
import com.smartims.dto.SystemConfigDTO;
import com.smartims.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置控制器
 */
@Tag(name = "系统配置", description = "系统基本信息和库存设置配置")
@RestController
@RequestMapping("/v1/settings/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @Operation(summary = "获取系统配置")
    @GetMapping
    public Result<SystemConfigDTO> getSystemConfig() {
        return Result.success(systemConfigService.getSystemConfig());
    }

    @Operation(summary = "更新系统配置")
    @OperationLog(module = "系统设置", action = "配置", description = "修改系统配置")
    @PutMapping
    public Result<Void> updateSystemConfig(@RequestBody SystemConfigDTO dto) {
        systemConfigService.updateSystemConfig(dto);
        return Result.success("系统配置更新成功");
    }
}
