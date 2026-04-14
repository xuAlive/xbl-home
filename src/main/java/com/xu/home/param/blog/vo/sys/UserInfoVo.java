package com.xu.home.param.blog.vo.sys;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfoVo {
    /**
     * 账号
     */
    private String account;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String name;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 出生日期
     */
    private Date birthday;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别 0 女 1 男
     */
    private Integer sex;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 身份证
     */
    private String idCard;

    /**
     * 简介
     */
    private String intro;

}
