package com.xu.home.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.blog.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据角色ID列表获取菜单
     */
    List<SysMenu> selectMenusByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /**
     * 根据用户账号获取菜单
     */
    List<SysMenu> selectMenusByAccount(@Param("account") String account);

    /**
     * 获取所有启用的菜单
     */
    List<SysMenu> selectAllEnabledMenus();
}
