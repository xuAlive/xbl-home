package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 系统用户详情表
 * @TableName sys_user_info
 */
@TableName(value ="sys_user_info")
@Data
public class SysUserInfo implements Serializable {
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
     * 昵称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 真实姓名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 出生日期
     */
    @TableField(value = "birthday")
    private Date birthday;

    /**
     * 年龄
     */
    @TableField(value = "age")
    private Integer age;

    /**
     * 性别 0 女 1 男
     */
    @TableField(value = "sex")
    private Integer sex;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 身份证
     */
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 简介
     */
    @TableField(value = "intro")
    private String intro;

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