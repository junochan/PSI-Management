package com.smartims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户数据传输对象
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class UserDTO {

    /**
     * 登录名（唯一，用于登录）
     */
    @NotBlank(message = "登录名不能为空")
    private String username;

    /**
     * 用户姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 密码
     */
    private String password;

    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;

    /**
     * 头像访问地址（如 /api/uploads/avatars/xxx.jpg，由头像上传接口返回）
     */
    private String avatar;

}