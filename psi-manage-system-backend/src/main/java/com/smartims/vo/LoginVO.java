package com.smartims.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应VO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 当前角色在系统中配置的权限标识（permission.code），与菜单/接口校验一致
     */
    private List<String> permissions;

}