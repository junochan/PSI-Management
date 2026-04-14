package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.UserDTO;
import com.smartims.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResult<SysUser> getUserList(PageQuery pageQuery);

    /**
     * 根据ID查询用户详情
     */
    SysUser getUserById(Long id);

    /**
     * 添加用户
     */
    void addUser(UserDTO dto);

    /**
     * 更新用户
     */
    void updateUser(Long id, UserDTO dto);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 获取所有用户列表
     */
    List<SysUser> getAllUsers();

}