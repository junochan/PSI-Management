package com.smartims.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户认证信息
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Data
@AllArgsConstructor
public class UserInfo implements Serializable {

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
     * 角色ID（可为空）
     */
    private Long roleId;

}