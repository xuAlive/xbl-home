package com.xu.home.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.blog.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户账号获取角色列表
     */
    List<SysRole> selectRolesByAccount(@Param("account") String account);

    /**
     * 根据角色编码获取角色
     */
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);
}
