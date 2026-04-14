package com.xu.home.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.blog.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

/**
 * 角色菜单关联Mapper
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 删除角色的所有菜单
     */
    int deleteByRoleId(@Param("roleId") Integer roleId);
}
