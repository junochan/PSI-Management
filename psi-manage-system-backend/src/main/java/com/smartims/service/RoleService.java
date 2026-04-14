package com.smartims.service;

import com.smartims.entity.SysRole;
import com.smartims.entity.SysPermission;
import com.smartims.dto.RoleDTO;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface RoleService {

    /**
     * 获取所有角色列表
     */
    List<SysRole> getAllRoles();

    /**
     * 获取角色详情
     */
    SysRole getRoleById(Long id);

    /**
     * 获取角色的权限列表
     */
    List<SysPermission> getRolePermissions(Long roleId);

    /**
     * 获取所有权限列表
     */
    List<SysPermission> getAllPermissions();

    /**
     * 创建新角色
     */
    void createRole(RoleDTO dto);

    /**
     * 更新角色信息
     */
    void updateRole(Long id, RoleDTO dto);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 更新角色权限
     */
    void updateRolePermissions(Long roleId, List<Long> permissionIds);

}