package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 博客文章详情表
 * @TableName blog_article_info
 */
@TableName(value ="blog_article_info")
@Data
public class BlogArticleInfo implements Serializable {
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
     * 博客标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 简介
     */
    @TableField(value = "intro")
    private String intro;

    /**
     * 图片json数组，例[,,]
     */
    @TableField(value = "img")
    private String img;

    /**
     * 内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 状态 0 暂存，1 发布 -1 删除
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 已发布文章修改次数
     */
    @TableField(value = "published_edit_count")
    private Integer publishedEditCount;

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
