package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户登陆信息表
 * @TableName sys_user_login
 */
@TableName(value ="sys_user_login")
@Data
public class SysUserLogin implements Serializable {
    /**
     * 
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    /**
     * 账号
     */
    @TableField(value = "account")
    private String account;

    /**
     * 登录时ip
     */
    @TableField(value = "ip")
    private String ip;

    /**
     * 登录IP地址对应的地理位置
     */
    @TableField(value = "address")
    private String address;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}