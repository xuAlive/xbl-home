package com.xu.home.param.blog.po.sys;

import lombok.Data;

@Data
public class ChangePasswordPo {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String confirmPassword;
}
