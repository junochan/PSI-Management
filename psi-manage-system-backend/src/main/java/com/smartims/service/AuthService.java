package com.smartims.service;

import com.smartims.dto.ChangePasswordDTO;
import com.smartims.dto.LoginDTO;
import com.smartims.dto.SsoLoginDTO;
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
     * 中转页 SSO：校验共享密钥后为指定用户签发 JWT（需在配置中开启）
     */
    LoginVO ssoLogin(SsoLoginDTO dto);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 刷新当前登录用户的 JWT
     */
    String refreshToken();

    /**
     * 当前登录用户修改密码（需校验原密码）
     */
    void changePassword(ChangePasswordDTO dto);

}