package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.UserDTO;
import com.smartims.entity.SysUser;
import com.smartims.exception.BusinessException;
import com.smartims.service.UserAvatarStorageService;
import com.smartims.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAvatarStorageService userAvatarStorageService;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Result<PageResult<SysUser>> getUserList(PageQuery pageQuery) {
        PageResult<SysUser> result = userService.getUserList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询所有用户")
    @GetMapping("/all")
    public Result<List<SysUser>> getAllUsers() {
        List<SysUser> list = userService.getAllUsers();
        return Result.success(list);
    }

    @Operation(summary = "上传用户头像（本地存储，返回可写入用户资料的图片地址）")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = userAvatarStorageService.saveAvatar(file);
            return Result.success(Map.of("url", url));
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("保存用户头像失败", e);
            throw new BusinessException("头像保存失败");
        }
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "添加用户")
    @OperationLog(module = "用户管理", action = "新增", description = "新增用户账号")
    @PostMapping
    public Result<Void> addUser(@Valid @RequestBody UserDTO dto) {
        userService.addUser(dto);
        return Result.success("用户添加成功");
    }

    @Operation(summary = "更新用户")
    @OperationLog(module = "用户管理", action = "修改", description = "修改用户信息")
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.updateUser(id, dto);
        return Result.success("用户更新成功");
    }

    @Operation(summary = "删除用户")
    @OperationLog(module = "用户管理", action = "删除", description = "删除用户账号")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功");
    }

}