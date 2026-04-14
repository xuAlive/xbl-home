package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 系统用户表
 * @TableName sys_user
 */
@TableName(value ="sys_user")
@Data
public class SysUser implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 账号
     */
    @TableField(value = "account")
    private String account;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 手机号是否验证 0-未验证(访客) 1-已验证(用户)
     */
    @TableField(value = "phone_verified")
    private Integer phoneVerified;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 微信openid
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 微信unionid
     */
    @TableField(value = "unionid")
    private String unionid;

    /**
     * 微信昵称
     */
    @TableField(value = "wx_nickname")
    private String wxNickname;

    /**
     * 微信头像URL
     */
    @TableField(value = "wx_avatar")
    private String wxAvatar;

    /**
     * 0 否 1 是
     */
    @TableField(value = "is_delete")
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}