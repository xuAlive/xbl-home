package com.xu.home.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.blog.SysUserRole;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联Mapper
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 删除用户的所有角色
     */
    int deleteByAccount(@Param("account") String account);
}
