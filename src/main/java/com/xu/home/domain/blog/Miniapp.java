package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 小程序表
 * @TableName miniapp
 */
@TableName(value = "miniapp")
@Data
public class Miniapp implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 简介
     */
    @TableField(value = "intro")
    private String intro;

    /**
     * 图标
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 图标颜色
     */
    @TableField(value = "color")
    private String color;

    /**
     * 分类
     */
    @TableField(value = "category")
    private String category;

    /**
     * 标签类型(primary/success/warning/danger/info)
     */
    @TableField(value = "tag_type")
    private String tagType;

    /**
     * 前端路由
     */
    @TableField(value = "route")
    private String route;

    /**
     * 外链地址
     */
    @TableField(value = "external_link")
    private String externalLink;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

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
