package com.xu.home.param.blog.vo.blog;

import lombok.Data;

import java.util.Date;

@Data
public class BlogArticleVo {
    private Integer id;

    /**
     * 账号
     */
    private String account;

    /**
     * 博客标题
     */
    private String title;

    /**
     * 简介
     */
    private String intro;

    /**
     * 图片json数组，例[,,]
     */
    private String img;

    /**
     * 内容
     */
    private String content;

    /**
     * 状态 0 暂存，1 发布 -1 删除
     */
    private Integer status;

    /**
     * 已发布文章修改次数
     */
    private Integer publishedEditCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
