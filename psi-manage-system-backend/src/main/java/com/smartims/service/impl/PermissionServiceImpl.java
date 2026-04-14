package com.smartims.service.impl;

import com.smartims.entity.SysPermission;
import com.smartims.entity.SysUser;
import com.smartims.mapper.SysPermissionMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.service.PermissionService;
import com.smartims.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper sysUserMapper;
    private final RoleService roleService;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }
        List<SysPermission> perms = roleService.getRolePermissions(user.getRoleId());
        if (perms.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (SysPermission p : perms) {
            if (p.getCode() != null && !p.getCode().isBlank()) {
                codes.add(p.getCode().trim());
            }
            addAncestorMenuCodes(p.getParentId(), codes);
        }
        return new ArrayList<>(codes);
    }

    /** 展开父级菜单码：仅勾选子权限时，JWT 中也带上如 inventory、products，与过滤器 OR 列表一致 */
    private void addAncestorMenuCodes(Long parentId, Set<String> codes) {
        int guard = 0;
        while (parentId != null && parentId > 0 && guard++ < 32) {
            SysPermission parent = sysPermissionMapper.selectById(parentId);
            if (parent == null) {
                break;
            }
            if (parent.getDeleted() != null && parent.getDeleted() == 1) {
                break;
            }
            if (parent.getCode() != null && !parent.getCode().isBlank()) {
                codes.add(parent.getCode().trim());
            }
            parentId = parent.getParentId();
        }
    }
}
