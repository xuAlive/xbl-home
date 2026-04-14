package com.xu.home.Interceptor.blog;

import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.Interceptor.common.annotation.RequireRole;
import com.xu.home.utils.common.context.UserContext;
import com.xu.home.param.common.UserToken;
import com.xu.home.service.blog.SysPermissionService;
import com.xu.home.service.blog.SysRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 权限拦截器
 * 检查用户是否有访问接口的权限
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    public PermissionInterceptor(SysRoleService sysRoleService, SysPermissionService sysPermissionService) {
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 如果不是Controller方法，直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查角色权限
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            String requiredRole = requireRole.value();
            UserToken userToken = UserContext.getCurrentUser();

            if (userToken == null || userToken.getAccount() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
                return false;
            }

            String account = userToken.getAccount();
            String userRole = sysRoleService.getRoleCodeByAccount(account);

            // 权限等级: ADMIN > USER > GUEST
            if (!hasRequiredRole(userRole, requiredRole)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，需要" + requiredRole + "角色\"}");
                return false;
            }
        }

        // 检查细粒度权限
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission != null) {
            String requiredPermission = requirePermission.value();
            UserToken userToken = UserContext.getCurrentUser();

            if (userToken == null || userToken.getAccount() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
                return false;
            }

            String account = userToken.getAccount();

            // ADMIN角色拥有所有权限，直接放行
            String userRole = sysRoleService.getRoleCodeByAccount(account);
            if ("ADMIN".equals(userRole)) {
                return true;
            }

            boolean hasPermission = sysPermissionService.hasPermission(account, requiredPermission);

            if (!hasPermission) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，需要" + requiredPermission + "权限\"}");
                return false;
            }
        }

        return true;
    }

    /**
     * 检查用户角色是否满足要求
     * 权限等级: ADMIN > USER > GUEST
     */
    private boolean hasRequiredRole(String userRole, String requiredRole) {
        return switch (userRole) {
            case "ADMIN" -> true;
            case "USER" -> "USER".equals(requiredRole) || "GUEST".equals(requiredRole);
            case "GUEST" -> "GUEST".equals(requiredRole);
            default -> false;
        };
    }
}
