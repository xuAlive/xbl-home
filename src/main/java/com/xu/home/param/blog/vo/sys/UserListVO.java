package com.xu.home.param.blog.vo.sys;

import lombok.Data;

import java.util.Date;

/**
 * 用户列表VO
 */
@Data
public class UserListVO {
    /**
     * 账号
     */
    private String account;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 昵称
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
