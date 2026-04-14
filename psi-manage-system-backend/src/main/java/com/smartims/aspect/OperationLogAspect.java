package com.smartims.aspect;

import com.smartims.annotation.OperationLog;
import com.smartims.entity.SysRole;
import com.smartims.entity.SysUser;
import com.smartims.mapper.SysRoleMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.security.UserContext;
import com.smartims.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 操作日志切面
 * 自动拦截带有@OperationLog注解的方法并记录操作日志
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 与常见 VARCHAR 长度兼容，避免异常堆栈写入日志表失败 */
    private static final int MAX_ERROR_MSG_LENGTH = 500;

    private final OperationLogService operationLogService;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 定义切点：所有带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.smartims.annotation.OperationLog)")
    public void operationLogPointcut() {}

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解信息
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户信息
        Long userId = UserContext.getCurrentUserId();
        String userName = UserContext.getCurrentUsername();
        String roleName = null;
        if (userName == null) {
            userName = "未知用户";
        }
        // 获取用户角色名称
        if (userId != null) {
            roleName = getRoleNameByUserId(userId);
        }

        // 获取请求信息
        String ip = getIpAddress();

        // 构建操作内容
        String module = annotation.module();
        String action = annotation.action();
        String content = annotation.description();

        // 如果内容为空，根据方法名自动生成
        if (content.isEmpty()) {
            content = generateContent(joinPoint, action);
        }

        Object result = null;
        Integer status = 1;
        String errorMsg = null;

        try {
            // 执行方法
            result = joinPoint.proceed();
            status = 1;
        } catch (Throwable e) {
            // 记录失败日志
            status = 0;
            errorMsg = truncateErrorMessage(e.getMessage());
            log.error("操作失败：module={}, action={}, error={}", module, action, errorMsg);
            throw e;
        } finally {
            // 记录操作日志
            try {
                long endTime = System.currentTimeMillis();
                String duration = (endTime - startTime) + "ms";

                // 如果内容中有耗时占位符，替换
                if (content.contains("{duration}")) {
                    content = content.replace("{duration}", duration);
                }

                operationLogService.log(userId, userName, roleName, action, content, module, ip, status, errorMsg);
            } catch (Exception e) {
                log.error("记录操作日志失败：{}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "未知IP";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "未知IP";
    }

    /**
     * 根据方法名自动生成操作内容
     */
    private String generateContent(ProceedingJoinPoint joinPoint, String action) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 根据方法名前缀生成描述
        if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("save")) {
            return "创建/新增操作";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "更新/修改操作";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除操作";
        } else if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("query") || methodName.startsWith("list")) {
            return "查询操作";
        } else if (methodName.startsWith("login")) {
            return "用户登录";
        } else if (methodName.startsWith("logout")) {
            return "用户退出";
        } else if (methodName.startsWith("import")) {
            return "导入操作";
        } else if (methodName.startsWith("export")) {
            return "导出操作";
        } else {
            return action + "操作 - " + className + "." + methodName;
        }
    }

    /**
     * 根据用户ID获取角色名称
     */
    private String getRoleNameByUserId(Long userId) {
        try {
            SysUser user = userMapper.selectById(userId);
            if (user != null && user.getRoleId() != null) {
                SysRole role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    return role.getName();
                }
            }
        } catch (Exception e) {
            log.error("获取用户角色失败：{}", e.getMessage());
        }
        return "未知角色";
    }

    private static String truncateErrorMessage(String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.length() <= MAX_ERROR_MSG_LENGTH) {
            return msg;
        }
        return msg.substring(0, MAX_ERROR_MSG_LENGTH) + "...(truncated)";
    }

}