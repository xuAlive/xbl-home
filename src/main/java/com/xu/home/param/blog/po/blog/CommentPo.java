package com.xu.home.param.blog.po.blog;

import lombok.Data;

/**
 * 添加评论参数
 */
@Data
public class CommentPo {

    /**
     * 文章ID
     */
    private Integer articleId;

    /**
     * 父评论ID，0表示对文章的直接评论
     */
    private Integer parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论者昵称
     */
    private String commenterName;

    /**
     * 评论者账号
     */
    private String commenterAccount;
}
