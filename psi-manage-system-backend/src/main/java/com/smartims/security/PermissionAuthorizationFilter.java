package com.smartims.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartims.common.Result;
import com.smartims.config.registry.ApplicationPermissionRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 按 API 路径校验当前用户是否具备菜单/模块级权限（与 sys_permission.code 一致）
 */
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ApplicationPermissionRegistry permissionRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = stripContextPath(request);
        if (!uri.startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isPublicApi(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isAuthWithoutModulePermissionCheck(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        if (hasAuthority(authentication, SUPER_ADMIN)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<List<String>> resolved = permissionRegistry.resolveApiPermissions(uri, request.getMethod());
        if (resolved.isEmpty()) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    Result.fail(com.smartims.common.ResultCode.FORBIDDEN.getCode(), "接口未配置权限规则"));
            return;
        }
        List<String> requiredAny = resolved.get();
        if (!hasAnyAuthority(authentication, requiredAny)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    Result.fail(com.smartims.common.ResultCode.FORBIDDEN.getCode(), "无权限访问"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    private boolean isPublicApi(String uri) {
        return pathMatcher.match("/v1/auth/login", uri)
                || pathMatcher.match("/v1/util/**", uri);
    }

    /** 已登录即可，不按业务模块鉴权 */
    private boolean isAuthWithoutModulePermissionCheck(String uri) {
        return pathMatcher.match("/v1/auth/logout", uri)
                || pathMatcher.match("/v1/auth/navigation", uri)
                || pathMatcher.match("/v1/auth/change-password", uri);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyAuthority(Authentication authentication, List<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }
        for (String a : authorities) {
            if (hasAuthority(authentication, a)) {
                return true;
            }
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, Result<?> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
