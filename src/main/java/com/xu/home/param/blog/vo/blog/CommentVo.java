package com.xu.home.param.blog.vo.blog;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评论返回VO
 */
@Data
public class CommentVo {

    /**
     * 评论ID
     */
    private Integer id;

    /**
     * 文章ID
     */
    private Integer articleId;

    /**
     * 父评论ID
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 文章标题（用于最新评论列表）
     */
    private String articleTitle;

    /**
     * 子评论列表（回复）
     */
    private List<CommentVo> replies;
}
