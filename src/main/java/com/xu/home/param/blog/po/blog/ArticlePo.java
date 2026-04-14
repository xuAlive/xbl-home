package com.xu.home.param.blog.po.blog;

import com.xu.home.param.blog.PageParam;
import lombok.Data;

@Data
public class ArticlePo extends PageParam {
    /**
     * 账号
     */
    private String account;
    /**
     * 标题
     */
    private String title;

}
