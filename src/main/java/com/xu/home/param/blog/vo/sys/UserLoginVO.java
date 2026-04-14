package com.xu.home.param.blog.vo.sys;

import lombok.Data;

import java.util.Date;

/**
 * 用户登录记录VO
 */
@Data
public class UserLoginVO {
    /**
     * 账号
     */
    private String account;

    /**
     * 登录IP
     */
    private String ip;

    /**
     * 登录地址
     */
    private String address;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 最近登录时间
     */
    private Date lastLoginTime;
}
