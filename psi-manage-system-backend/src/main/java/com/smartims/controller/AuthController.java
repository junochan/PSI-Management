package com.smartims.controller;

import com.smartims.common.Result;
import com.smartims.dto.ChangePasswordDTO;
import com.smartims.dto.LoginDTO;
import com.smartims.dto.SsoLoginDTO;
import com.smartims.security.UserContext;
import com.smartims.service.AuthService;
import com.smartims.service.NavigationService;
import com.smartims.vo.LoginVO;
import com.smartims.vo.NavigationVO;
import com.smartims.vo.TokenRefreshVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "认证管理", description = "用户登录登出相关接口")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final NavigationService navigationService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "中转页 SSO 登录（共享密钥换取 JWT）")
    @PostMapping("/sso-login")
    public Result<LoginVO> ssoLogin(@Valid @RequestBody SsoLoginDTO dto) {
        LoginVO loginVO = authService.ssoLogin(dto);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功");
    }

    @Operation(summary = "刷新当前登录用户 token")
    @PostMapping("/refresh")
    public Result<TokenRefreshVO> refreshToken() {
        String token = authService.refreshToken();
        return Result.success("续期成功", new TokenRefreshVO(token));
    }

    @Operation(summary = "当前用户导航（菜单树、权限码、前端路由表）")
    @GetMapping("/navigation")
    public Result<NavigationVO> navigation() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail(com.smartims.common.ResultCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return Result.success(navigationService.buildNavigation(userId));
    }

    @Operation(summary = "当前用户修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码已更新");
    }

}