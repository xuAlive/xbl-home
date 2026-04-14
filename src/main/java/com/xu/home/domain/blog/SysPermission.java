package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统权限表
 * @TableName sys_permission
 */
@TableName(value = "sys_permission")
@Data
public class SysPermission implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 权限编码
     */
    @TableField(value = "permission_code")
    private String permissionCode;

    /**
     * 权限名称
     */
    @TableField(value = "permission_name")
    private String permissionName;

    /**
     * 资源类型(API/BUTTON/DATA)
     */
    @TableField(value = "resource_type")
    private String resourceType;

    /**
     * 资源路径
     */
    @TableField(value = "resource_path")
    private String resourcePath;

    /**
     * 权限描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 状态 0-禁用 1-启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 是否删除 0-否 1-是
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
