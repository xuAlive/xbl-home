package com.xu.home.param.blog.vo.blog;

import lombok.Data;

import java.util.Date;

@Data
public class BlogArticleBrowsingHistoryVo {

    private Integer id;

    private Integer articleId;

    private String title;

    private String author;

    private Date articleCreateTime;

    private Date lastViewTime;
}
