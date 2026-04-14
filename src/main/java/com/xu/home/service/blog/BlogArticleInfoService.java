package com.xu.home.service.blog;

import com.xu.home.domain.blog.BlogArticleInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.param.blog.po.blog.ArticlePo;
import com.xu.home.param.blog.po.blog.BlogArticlePo;
import com.xu.home.param.common.response.Response;

/**
* @author xubaolin
* @description 针对表【blog_article_info(博客文章详情表)】的数据库操作Service
* @createDate 2024-01-21 12:53:40
*/
public interface BlogArticleInfoService extends IService<BlogArticleInfo> {

    /**
     * 新增或更新博客文章
     * @param po
     * @return
     */
    Response createOrUpdateArticle(BlogArticlePo po);

    /**
     * 根据ID和账号删除博客
     * @param id
     * @param account
     * @return
     */
    Response deleteArticle(Integer id,String account);

    /**
     * 根据ID获取文章详情
     * @param id
     * @return
     */
    Response getArticleById(Integer id);

    Response getArticleById(Integer id, String viewerAccount);

    /**
     * 获取文章列表
     * @param po
     * @return
     */
    Response listArticle(ArticlePo po);
}
