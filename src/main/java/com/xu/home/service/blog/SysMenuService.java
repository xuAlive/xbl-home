package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.SysMenu;

import java.util.List;

/**
 * 菜单Service
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 根据用户账号获取菜单树
     */
    List<SysMenu> getMenuTreeByAccount(String account);

    /**
     * 获取所有菜单树（管理员用）
     */
    List<SysMenu> getAllMenuTree();

    /**
     * 保存菜单
     */
    boolean saveMenu(SysMenu menu);

    /**
     * 更新菜单
     */
    boolean updateMenu(SysMenu menu);

    /**
     * 删除菜单
     */
    boolean deleteMenu(Integer menuId);

    /**
     * 为角色分配菜单
     */
    boolean assignMenusToRole(Integer roleId, List<Integer> menuIds);

    /**
     * 根据角色ID获取已分配的菜单ID列表
     */
    List<Integer> getMenuIdsByRoleId(Integer roleId);
}
