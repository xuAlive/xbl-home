package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 博客评论表
 * @TableName blog_comment
 */
@TableName(value = "blog_comment")
@Data
public class BlogComment implements Serializable {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 文章ID
     */
    @TableField(value = "article_id")
    private Integer articleId;

    /**
     * 父评论ID，0表示对文章的直接评论
     */
    @TableField(value = "parent_id")
    private Integer parentId;

    /**
     * 评论内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 评论者昵称
     */
    @TableField(value = "commenter_name")
    private String commenterName;

    /**
     * 评论者账号
     */
    @TableField(value = "commenter_account")
    private String commenterAccount;

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
