package com.xu.home.service.blog;

import com.xu.home.domain.blog.SysUserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.param.blog.po.sys.UserInfoPo;
import com.xu.home.param.common.response.Response;

/**
 * @author xubaolin
 * @description 针对表【sys_user_info(系统用户详情表)】的数据库操作Service
 * @createDate 2024-01-21 12:53:40
 */
public interface SysUserInfoService extends IService<SysUserInfo> {
    /**
     * 根据账号获取用户信息
     * @Param: [account]
     * @return: com.xu.home.Param.common.response.Response
     * @Author: xubaolin
     **/
    Response getUserInfoByAccount(String account);

    /**
     * 更新账号信息
     * @param po
     * @return
     */
    Response updateUserInfo(UserInfoPo po);

    /**
     * 注销账号
     * @param Account
     * @return
     */
    Response deleteAccount(String Account);

    /**
     * 获取用户列表
     * @return
     */
    Response getUserList();
}
