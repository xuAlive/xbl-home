package com.xu.home.param.blog.po.sys;

import lombok.Data;

@Data
public class LoginUserPo {
    private String account;
    private String password;
    /**
     * 昵称（注册时使用）
     */
    private String nickname;
}
