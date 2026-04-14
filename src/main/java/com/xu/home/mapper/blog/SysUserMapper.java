package com.xu.home.mapper.blog;

import com.xu.home.domain.blog.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author xubaolin
* @description 针对表【sys_user(系统用户表)】的数据库操作Mapper
* @createDate 2024-01-21 13:49:27
* @Entity com.xu.home.domain.blog.SysUser
*/
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    // 插入
//    int insert(SysUser sysUser);

    void deleteUser(@Param("account") String account);

    SysUser selectUser(@Param("account") String account,@Param("password") String password,@Param("phone") String phone);

    /**
     * 根据微信openid查询用户
     */
    SysUser selectByOpenid(@Param("openid") String openid);
}




