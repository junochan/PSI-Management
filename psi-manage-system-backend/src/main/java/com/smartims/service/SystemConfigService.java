package com.smartims.service;

import com.smartims.dto.SystemConfigDTO;

/**
 * 系统配置服务
 */
public interface SystemConfigService {

    /**
     * 获取系统配置（基本信息 + 库存设置）
     */
    SystemConfigDTO getSystemConfig();

    /**
     * 更新系统配置
     */
    void updateSystemConfig(SystemConfigDTO dto);
}
