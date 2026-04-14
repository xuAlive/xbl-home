package com.xu.home.mapper.blog;

import com.xu.home.domain.blog.SysUserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.param.blog.vo.sys.UserInfoVo;
import com.xu.home.param.blog.vo.sys.UserListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author xubaolin
* @description 针对表【sys_user_info(系统用户详情表)】的数据库操作Mapper
* @createDate 2024-01-21 13:49:27
* @Entity com.xu.home.domain.blog.SysUserInfo
*/
public interface SysUserInfoMapper extends BaseMapper<SysUserInfo> {

    UserInfoVo selectByAccount(@Param("account") String account);

    void delteUserInfo(@Param("account") String account);

    /**
     * 获取用户列表（关联sys_user和sys_user_info）
     */
    List<UserListVO> selectUserList();

    /**
     * 插入或更新用户信息（如果account存在则更新，不存在则插入）
     */
    int insertOrUpdateUserInfo(SysUserInfo sysUserInfo);
}




