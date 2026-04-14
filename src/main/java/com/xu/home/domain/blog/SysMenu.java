package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统菜单表
 * @TableName sys_menu
 */
@TableName(value = "sys_menu")
@Data
public class SysMenu implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 父菜单ID 0表示顶级菜单
     */
    @TableField(value = "parent_id")
    private Integer parentId;

    /**
     * 菜单名称
     */
    @TableField(value = "menu_name")
    private String menuName;

    /**
     * 菜单编码
     */
    @TableField(value = "menu_code")
    private String menuCode;

    /**
     * 菜单类型 1-目录 2-菜单 3-按钮
     */
    @TableField(value = "menu_type")
    private Integer menuType;

    /**
     * 路由地址
     */
    @TableField(value = "path")
    private String path;

    /**
     * 组件路径
     */
    @TableField(value = "component")
    private String component;

    /**
     * 菜单图标
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 是否可见 0-隐藏 1-显示
     */
    @TableField(value = "visible")
    private Integer visible;

    /**
     * 状态 0-禁用 1-启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 权限标识
     */
    @TableField(value = "permission")
    private String permission;

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

    /**
     * 子菜单列表
     */
    @TableField(exist = false)
    private List<SysMenu> children;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
