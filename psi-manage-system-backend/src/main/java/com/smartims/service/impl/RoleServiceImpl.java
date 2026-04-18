package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.RoleDTO;
import com.smartims.entity.SysRole;
import com.smartims.entity.SysPermission;
import com.smartims.entity.SysRolePermission;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.SysRoleMapper;
import com.smartims.mapper.SysPermissionMapper;
import com.smartims.mapper.SysRolePermissionMapper;
import com.smartims.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<SysRole> getAllRoles() {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getDeleted, 0);
        queryWrapper.orderByAsc(SysRole::getId);
        return sysRoleMapper.selectList(queryWrapper);
    }

    @Override
    public PageResult<SysRole> getRolePage(PageQuery pageQuery) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getDeleted, 0);
        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword().trim();
            queryWrapper.and(w -> w.like(SysRole::getName, kw)
                    .or()
                    .like(SysRole::getCode, kw)
                    .or()
                    .like(SysRole::getDescription, kw));
        }
        queryWrapper.orderByAsc(SysRole::getId);
        Page<SysRole> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<SysRole> result = sysRoleMapper.selectPage(page, queryWrapper);
        return PageResult.build(result.getTotal(), pageQuery.getPage(), pageQuery.getSize(), result.getRecords());
    }

    @Override
    public SysRole getRoleById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null || role.getDeleted() == 1) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Override
    public List<SysPermission> getRolePermissions(Long roleId) {
        // 查询角色权限关联
        LambdaQueryWrapper<SysRolePermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRolePermission::getRoleId, roleId);
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(queryWrapper);

        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取权限ID列表
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 查询权限详情
        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permissionIds);
        return permissions.stream()
                .filter(p -> p.getDeleted() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysPermission> getAllPermissions() {
        LambdaQueryWrapper<SysPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPermission::getDeleted, 0);
        queryWrapper.eq(SysPermission::getStatus, 1);
        queryWrapper.orderByAsc(SysPermission::getSort);
        return sysPermissionMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        // 验证角色是否存在
        getRoleById(roleId);

        // 删除原有权限关联（物理删除）
        LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRolePermission::getRoleId, roleId);
        sysRolePermissionMapper.delete(deleteWrapper);

        // 添加新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rolePermission = new SysRolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(rolePermission);
            }
        }

        log.info("更新角色权限成功：roleId={}, permissionCount={}", roleId, permissionIds != null ? permissionIds.size() : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleDTO dto) {
        SysRole role = getRoleById(id);

        // 超级管理员不能修改
        if (id == 1) {
            throw new BusinessException("超级管理员角色不能修改");
        }

        if (dto.getName() != null) {
            role.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            role.setCode(dto.getCode());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }

        sysRoleMapper.updateById(role);
        log.info("更新角色信息成功：roleId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleDTO dto) {
        SysRole role = new SysRole();
        role.setName(dto.getName());

        // 自动生成角色编码：ROLE_ + 时间戳
        String code = "ROLE_" + System.currentTimeMillis();
        role.setCode(code);

        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        sysRoleMapper.insert(role);
        log.info("创建角色成功：roleId={}, name={}, code={}", role.getId(), role.getName(), role.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 超级管理员不能删除
        if (id == 1) {
            throw new BusinessException("超级管理员角色不能删除");
        }

        SysRole role = getRoleById(id);

        // 删除角色权限关联
        LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRolePermission::getRoleId, id);
        sysRolePermissionMapper.delete(deleteWrapper);

        // 删除角色
        sysRoleMapper.deleteById(id);
        log.info("删除角色成功：roleId={}, name={}", id, role.getName());
    }

}