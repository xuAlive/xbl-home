package com.xu.home.mapper.blog;

import com.xu.home.domain.blog.BlogArticleInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author xubaolin
* @description 针对表【blog_article_info(博客文章详情表)】的数据库操作Mapper
* @createDate 2024-01-21 13:49:27
* @Entity com.xu.home.domain.blog.BlogArticleInfo
*/
public interface BlogArticleInfoMapper extends BaseMapper<BlogArticleInfo> {

    void deleteByAccount(@Param("account") String account);
}




