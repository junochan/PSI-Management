package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.entity.SysOperationLog;

/**
 * 操作日志服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志列表
     */
    PageResult<SysOperationLog> getLogList(PageQuery pageQuery);

    /**
     * 记录操作日志
     */
    void log(Long userId, String userName, String roleName, String action, String content, String module, String ip);

    /**
     * 记录操作日志（带状态）
     */
    void log(Long userId, String userName, String roleName, String action, String content, String module, String ip, Integer status, String errorMsg);

}