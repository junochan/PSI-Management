package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.entity.SysOperationLog;
import com.smartims.entity.SysRole;
import com.smartims.entity.SysUser;
import com.smartims.mapper.SysOperationLogMapper;
import com.smartims.mapper.SysRoleMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    /**
     * 错误信息写入上限（含截断后缀）。兼容仍为 VARCHAR(255) 的旧库表，避免 MyBatis 长异常信息撑爆列。
     */
    private static final int MAX_ERROR_MSG_TOTAL = 255;

    private static final String ERROR_MSG_TRUNCATED_SUFFIX = "...(truncated)";

    private final SysOperationLogMapper operationLogMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public PageResult<SysOperationLog> getLogList(PageQuery pageQuery) {
        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysOperationLog::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysOperationLog::getUserName, pageQuery.getKeyword())
                    .or()
                    .like(SysOperationLog::getAction, pageQuery.getKeyword())
                    .or()
                    .like(SysOperationLog::getContent, pageQuery.getKeyword())
                    .or()
                    .like(SysOperationLog::getModule, pageQuery.getKeyword())
            );
        }

        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);

        Page<SysOperationLog> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<SysOperationLog> result = operationLogMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), pageQuery.getPage(), pageQuery.getSize(), result.getRecords());
    }

    @Override
    public void log(Long userId, String userName, String roleName, String action, String content, String module, String ip) {
        log(userId, userName, roleName, action, content, module, ip, 1, null);
    }

    @Override
    public void log(Long userId, String userName, String roleName, String action, String content, String module, String ip, Integer status, String errorMsg) {
        // 如果角色名称为空，尝试从数据库获取
        if (roleName == null && userId != null) {
            roleName = getRoleNameByUserId(userId);
        }

        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(userId);
        operationLog.setUserName(userName);
        operationLog.setRoleName(roleName);
        operationLog.setAction(action);
        operationLog.setContent(content);
        operationLog.setModule(module);
        operationLog.setIp(ip);
        operationLog.setStatus(status != null ? status : 1);
        operationLog.setErrorMsg(truncateErrorMsg(errorMsg));

        operationLogMapper.insert(operationLog);
        log.debug("记录操作日志：userId={}, userName={}, roleName={}, action={}, content={}", userId, userName, roleName, action, content);
    }

    private static String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) {
            return null;
        }
        int maxBody = MAX_ERROR_MSG_TOTAL - ERROR_MSG_TRUNCATED_SUFFIX.length();
        if (errorMsg.length() <= maxBody) {
            return errorMsg;
        }
        return errorMsg.substring(0, maxBody) + ERROR_MSG_TRUNCATED_SUFFIX;
    }

    /**
     * 根据用户ID获取角色名称
     */
    private String getRoleNameByUserId(Long userId) {
        try {
            SysUser user = userMapper.selectById(userId);
            if (user != null && user.getRoleId() != null) {
                SysRole role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    return role.getName();
                }
            }
        } catch (Exception e) {
            log.error("获取用户角色失败：{}", e.getMessage());
        }
        return "未知角色";
    }

}