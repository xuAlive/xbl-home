package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.SysMenu;
import com.xu.home.domain.blog.SysRoleMenu;
import com.xu.home.mapper.blog.SysMenuMapper;
import com.xu.home.mapper.blog.SysRoleMenuMapper;
import com.xu.home.service.blog.SysMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单Service实现
 */
@Slf4j
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    public SysMenuServiceImpl(SysRoleMenuMapper sysRoleMenuMapper) {
        this.sysRoleMenuMapper = sysRoleMenuMapper;
    }

    @Override
    public List<SysMenu> getMenuTreeByAccount(String account) {
        List<SysMenu> menus = baseMapper.selectMenusByAccount(account);
        List<SysMenu> result = new ArrayList<>();
        // 添加侧边栏菜单（parent_id=0的一级目录及其子菜单）
        result.addAll(buildMenuTree(menus, 0));
        // 添加顶部菜单（parent_id=-1的特殊菜单，显示在右上角）
        result.addAll(buildMenuTree(menus, -1));
        return result;
    }

    @Override
    public List<SysMenu> getAllMenuTree() {
        List<SysMenu> menus = baseMapper.selectAllEnabledMenus();
        List<SysMenu> result = new ArrayList<>();
        // 添加侧边栏菜单（parent_id=0的一级目录及其子菜单）
        result.addAll(buildMenuTree(menus, 0));
        // 添加顶部菜单（parent_id=-1的特殊菜单，显示在右上角）
        result.addAll(buildMenuTree(menus, -1));
        return result;
    }

    @Override
    public boolean saveMenu(SysMenu menu) {
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(SysMenu menu) {
        return this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Integer menuId) {
        // 逻辑删除
        SysMenu menu = this.getById(menuId);
        if (menu != null) {
            menu.setIsDelete(1);
            return this.updateById(menu);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenusToRole(Integer roleId, List<Integer> menuIds) {
        try {
            // 先删除原有的菜单分配
            QueryWrapper<SysRoleMenu> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("role_id", roleId);
            sysRoleMenuMapper.delete(deleteWrapper);

            // 分配新菜单
            if (menuIds != null && !menuIds.isEmpty()) {
                for (Integer menuId : menuIds) {
                    SysRoleMenu roleMenu = new SysRoleMenu();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuId(menuId);
                    sysRoleMenuMapper.insert(roleMenu);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("分配菜单失败", e);
            throw new RuntimeException("分配菜单失败");
        }
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus, Integer parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId().equals(parentId)) {
                List<SysMenu> children = buildMenuTree(menus, menu.getId());
                menu.setChildren(children);
                tree.add(menu);
            }
        }
        return tree;
    }

    @Override
    public List<Integer> getMenuIdsByRoleId(Integer roleId) {
        QueryWrapper<SysRoleMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(queryWrapper);
        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }
}
