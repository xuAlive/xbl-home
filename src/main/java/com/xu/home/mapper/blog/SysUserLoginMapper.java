package com.xu.home.mapper.blog;

import com.xu.home.domain.blog.SysUserLogin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.param.blog.vo.sys.ProvinceStatVO;
import com.xu.home.param.blog.vo.sys.UserLoginVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author xubaolin
* @description 针对表【sys_user_login(用户登陆信息表)】的数据库操作Mapper
* @createDate 2024-01-21 13:49:27
* @Entity com.xu.home.domain.blog.SysUserLogin
*/
public interface SysUserLoginMapper extends BaseMapper<SysUserLogin> {

    /**
     * 按账号和IP分组查询登录记录（分页）
     */
    List<UserLoginVO> selectLoginRecordsGroupByAccountAndIp(@Param("account") String account,
                                                            @Param("offset") int offset,
                                                            @Param("size") int size);

    /**
     * 按账号和IP分组查询登录记录总数
     */
    int countLoginRecordsGroupByAccountAndIp(@Param("account") String account);

    /**
     * 查询所有登录地点及次数（用于地图标点）
     */
    List<UserLoginVO> selectAllLoginLocations(@Param("account") String account);

    /**
     * 按省份统计登录次数（用于饼形图）
     */
    List<ProvinceStatVO> selectLoginCountByProvince(@Param("account") String account);
}




