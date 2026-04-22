package com.xu.home.service.blog;

import com.xu.home.domain.blog.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.param.blog.po.sys.ChangePasswordPo;
import com.xu.home.param.blog.po.sys.LoginUserPo;
import com.xu.home.param.common.response.Response;

import jakarta.servlet.http.HttpServletRequest;

/**
* @author xubaolin
* @description 针对表【sys_user(系统用户表)】的数据库操作Service
* @createDate 2024-01-21 12:53:40
*/
public interface SysUserService extends IService<SysUser> {

    /**
     * 登陆
     * @param po
     * @return
     */
    Response login(LoginUserPo po, HttpServletRequest servletRequest);
    /**
     * 注册
     * @param po
     * @return
     */
    Response register(LoginUserPo po);

    /**
     * 修改当前登录用户密码
     * @param account 当前登录账号
     * @param po 修改密码参数
     * @return 处理结果
     */
    Response changePassword(String account, ChangePasswordPo po);
}
