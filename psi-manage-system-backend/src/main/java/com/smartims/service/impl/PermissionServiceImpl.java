package com.smartims.service.impl;

import com.smartims.entity.SysUser;
import com.smartims.mapper.SysUserMapper;
import com.smartims.service.PermissionService;
import com.smartims.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper sysUserMapper;
    private final RoleService roleService;

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }
        return roleService.getRolePermissions(user.getRoleId()).stream()
                .map(p -> p.getCode())
                .collect(Collectors.toList());
    }
}
