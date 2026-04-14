package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.Result;
import com.smartims.dto.RoleDTO;
import com.smartims.entity.SysRole;
import com.smartims.entity.SysPermission;
import com.smartims.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "角色管理", description = "角色和权限相关接口")
@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取所有角色列表")
    @GetMapping
    public Result<List<SysRole>> getAllRoles() {
        List<SysRole> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getRoleById(id);
        return Result.success(role);
    }

    @Operation(summary = "获取角色的权限列表")
    @GetMapping("/{id}/permissions")
    public Result<List<SysPermission>> getRolePermissions(@PathVariable Long id) {
        List<SysPermission> permissions = roleService.getRolePermissions(id);
        return Result.success(permissions);
    }

    @Operation(summary = "获取所有权限列表")
    @GetMapping("/permissions")
    public Result<List<SysPermission>> getAllPermissions() {
        List<SysPermission> permissions = roleService.getAllPermissions();
        return Result.success(permissions);
    }

    @Operation(summary = "更新角色权限")
    @OperationLog(module = "角色管理", action = "修改", description = "修改角色权限配置")
    @PutMapping("/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.updateRolePermissions(id, permissionIds);
        return Result.success("角色权限更新成功");
    }

    @Operation(summary = "更新角色信息")
    @OperationLog(module = "角色管理", action = "修改", description = "修改角色基本信息")
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success("角色信息更新成功");
    }

    @Operation(summary = "创建角色")
    @OperationLog(module = "角色管理", action = "新增", description = "创建新角色")
    @PostMapping
    public Result<Void> createRole(@RequestBody RoleDTO dto) {
        roleService.createRole(dto);
        return Result.success("角色创建成功");
    }

    @Operation(summary = "删除角色")
    @OperationLog(module = "角色管理", action = "删除", description = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("角色删除成功");
    }

}