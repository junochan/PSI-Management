package com.smartims.service;

import java.util.List;

/**
 * 用户权限查询（基于角色-权限配置）
 */
public interface PermissionService {

    /**
     * 返回当前用户角色在系统中配置的全部权限标识（permission.code）
     */
    List<String> getPermissionCodesByUserId(Long userId);
}
