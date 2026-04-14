package com.xu.home.controller.blog;

import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.domain.blog.SysPermission;
import com.xu.home.service.blog.SysPermissionService;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理Controller
 */
@RequestMapping("/blog/permission")
@RestController
public class PermissionController {

    private final SysPermissionService sysPermissionService;

    public PermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    /**
     * 获取当前用户的权限列表
     */
    @GetMapping("/getUserPermissions")
    public Response getUserPermissions() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            return Response.error("未登录");
        }
        List<SysPermission> permissions = sysPermissionService.getPermissionsByAccount(account);
        return Response.success(permissions);
    }

    /**
     * 获取所有权限列表（管理员用）
     */
    @GetMapping("/getAllPermissions")
    @RequirePermission("system:permission:manage")
    public Response getAllPermissions() {
        List<SysPermission> permissions = sysPermissionService.getAllValidPermissions();
        return Response.success(permissions);
    }

    /**
     * 新增权限
     */
    @PostMapping("/add")
    @RequirePermission("system:permission:manage")
    public Response addPermission(@RequestBody SysPermission permission) {
        boolean result = sysPermissionService.save(permission);
        return result ? Response.success() : Response.error("新增权限失败");
    }

    /**
     * 修改权限
     */
    @PostMapping("/update")
    @RequirePermission("system:permission:manage")
    public Response updatePermission(@RequestBody SysPermission permission) {
        boolean result = sysPermissionService.updateById(permission);
        return result ? Response.success() : Response.error("修改权限失败");
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/assignToRole")
    @RequirePermission("system:permission:manage")
    public Response assignPermissionsToRole(@RequestParam("roleId") Integer roleId,
                                             @RequestBody List<Integer> permissionIds) {
        boolean result = sysPermissionService.assignPermissionsToRole(roleId, permissionIds);
        return result ? Response.success() : Response.error("分配权限失败");
    }

    /**
     * 获取角色已分配的权限ID列表（用于回显）
     */
    @GetMapping("/getPermissionIdsByRoleId")
    @RequirePermission("system:permission:manage")
    public Response getPermissionIdsByRoleId(@RequestParam("roleId") Integer roleId) {
        List<Integer> permissionIds = sysPermissionService.getPermissionIdsByRoleId(roleId);
        return Response.success(permissionIds);
    }
}
