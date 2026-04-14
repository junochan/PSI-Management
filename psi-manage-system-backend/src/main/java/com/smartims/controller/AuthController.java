package com.smartims.controller;

import com.smartims.common.Result;
import com.smartims.dto.LoginDTO;
import com.smartims.security.UserContext;
import com.smartims.service.AuthService;
import com.smartims.service.NavigationService;
import com.smartims.vo.LoginVO;
import com.smartims.vo.NavigationVO;
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

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功");
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

}