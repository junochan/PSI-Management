package com.smartims.service;

import com.smartims.dto.LoginDTO;
import com.smartims.vo.LoginVO;

/**
 * 认证服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户登出
     */
    void logout();

}