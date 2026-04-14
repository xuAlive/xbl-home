package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.SysRole;

import java.util.List;

/**
 * 角色Service
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户账号获取角色列表
     */
    List<SysRole> getRolesByAccount(String account);

    /**
     * 为用户分配角色
     */
    boolean assignRoleToUser(String account, Integer roleId);

    /**
     * 移除用户指定角色
     */
    boolean removeRoleFromUser(String account, Integer roleId);

    /**
     * 获取角色编码（ADMIN/USER/GUEST）
     */
    String getRoleCodeByAccount(String account);

    /**
     * 根据角色编码获取角色
     */
    SysRole selectByRoleCode(String roleCode);
}
