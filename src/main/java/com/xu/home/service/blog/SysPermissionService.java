package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.SysPermission;

import java.util.List;

/**
 * 权限Service
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据用户账号获取权限列表
     */
    List<SysPermission> getPermissionsByAccount(String account);

    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(String account, String permissionCode);

    /**
     * 为角色分配权限
     */
    boolean assignPermissionsToRole(Integer roleId, List<Integer> permissionIds);

    /**
     * 获取所有有效权限列表
     */
    List<SysPermission> getAllValidPermissions();

    /**
     * 根据角色ID获取已分配的权限ID列表
     */
    List<Integer> getPermissionIdsByRoleId(Integer roleId);
}
