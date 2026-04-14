package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BlogArticleInfo;
import com.xu.home.param.blog.po.blog.ArticlePo;
import com.xu.home.param.blog.po.blog.BlogArticlePo;
import com.xu.home.param.blog.vo.blog.BlogArticleVo;
import com.xu.home.service.blog.BlogArticleInfoService;
import com.xu.home.service.blog.BlogArticleBrowsingHistoryService;
import com.xu.home.mapper.blog.BlogArticleInfoMapper;
import com.xu.home.param.common.response.Response;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
* @author xubaolin
* @description 针对表【blog_article_info(博客文章详情表)】的数据库操作Service实现
* @createDate 2024-01-21 12:53:40
*/
@Service
public class BlogArticleInfoServiceImpl extends ServiceImpl<BlogArticleInfoMapper, BlogArticleInfo> implements BlogArticleInfoService{

    private final BlogArticleBrowsingHistoryService browsingHistoryService;

    public BlogArticleInfoServiceImpl(BlogArticleBrowsingHistoryService browsingHistoryService) {
        this.browsingHistoryService = browsingHistoryService;
    }

    @Override
    public Response createOrUpdateArticle(BlogArticlePo po) {
        if (Objects.isNull(po)){
            return Response.error("参数不能为空");
        }
        BlogArticleInfo blogArticleInfo;
        Date now = new Date();

        if (po.getId() != null) {
            blogArticleInfo = getById(po.getId());
            if (blogArticleInfo == null) {
                return Response.error("文章不存在");
            }
            if (!StringUtils.hasText(po.getAccount()) || !po.getAccount().equals(blogArticleInfo.getAccount())) {
                return Response.error("无权编辑该文章");
            }

            if (Objects.equals(blogArticleInfo.getStatus(), 1) && Objects.equals(po.getStatus(), 0)) {
                return Response.error("已发布文章不能再保存为草稿");
            }

            if (Objects.equals(blogArticleInfo.getStatus(), 1)
                    && Objects.equals(po.getStatus(), 1)
                    && isPublishedContentChanged(blogArticleInfo, po)) {
                int publishedEditCount = blogArticleInfo.getPublishedEditCount() == null ? 0 : blogArticleInfo.getPublishedEditCount();
                if (publishedEditCount >= 3) {
                    return Response.error("已发布文章最多只能修改3次");
                }
                blogArticleInfo.setPublishedEditCount(publishedEditCount + 1);
            }
        } else {
            blogArticleInfo = new BlogArticleInfo();
            blogArticleInfo.setCreateTime(now);
            blogArticleInfo.setPublishedEditCount(0);
        }

        BeanUtils.copyProperties(po, blogArticleInfo);
        blogArticleInfo.setUpdateTime(now);

        boolean result = saveOrUpdate(blogArticleInfo);
        if (!result) {
            return Response.error("操作失败");
        }

        BlogArticleVo vo = new BlogArticleVo();
        BeanUtils.copyProperties(blogArticleInfo, vo);
        return Response.success(vo);
    }

    private boolean isPublishedContentChanged(BlogArticleInfo current, BlogArticlePo incoming) {
        return !Objects.equals(current.getTitle(), incoming.getTitle())
                || !Objects.equals(current.getIntro(), incoming.getIntro())
                || !Objects.equals(current.getContent(), incoming.getContent())
                || !Objects.equals(current.getImg(), incoming.getImg());
    }

    @Override
    public Response deleteArticle(Integer id, String account) {
        BlogArticleInfo blogArticleInfo = new BlogArticleInfo();
        blogArticleInfo.setStatus(-1);
        int update = baseMapper.update(blogArticleInfo, new QueryWrapper<BlogArticleInfo>()
                .eq("id", id)
                .eq("account", account));
        return Response.checkResult(update == 1);
    }

    @Override
    public Response getArticleById(Integer id) {
        return getArticleById(id, null);
    }

    @Override
    public Response getArticleById(Integer id, String viewerAccount) {
        BlogArticleInfo blogArticleInfo = baseMapper.selectById(id);
        if (blogArticleInfo == null) {
            return Response.error("文章不存在");
        }

        if (StringUtils.hasText(viewerAccount)) {
            browsingHistoryService.recordView(viewerAccount, id);
        }

        BlogArticleVo vo = new BlogArticleVo();
        BeanUtils.copyProperties(blogArticleInfo,vo);
        return Response.success(vo);
    }

    @Override
    public Response listArticle(ArticlePo po) {
        if (Objects.isNull(po)){
            return Response.error("参数不能为空");
        }
        Page<BlogArticleInfo> page = new Page<BlogArticleInfo>(po.getPage(),po.getSize());
        QueryWrapper<BlogArticleInfo> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(po.getAccount())){
            queryWrapper.eq("account",po.getAccount());
            // 查询用户自己的文章时，排除已删除的
            queryWrapper.ne("status", -1);
        } else {
            // 博客广场：不传account时只查询已发布的文章
            queryWrapper.eq("status", 1);
        }
        if (!StringUtils.isEmpty(po.getTitle())){
            queryWrapper.like("title",po.getTitle());
        }
        queryWrapper.orderByDesc("create_time");
        Page<BlogArticleInfo> articleInfoPage = page(page, queryWrapper);
        return Response.success(articleInfoPage);
    }
}

