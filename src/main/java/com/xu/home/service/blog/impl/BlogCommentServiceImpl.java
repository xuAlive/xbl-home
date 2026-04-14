package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BlogArticleInfo;
import com.xu.home.domain.blog.BlogComment;
import com.xu.home.mapper.blog.BlogArticleInfoMapper;
import com.xu.home.mapper.blog.BlogCommentMapper;
import com.xu.home.param.blog.po.blog.CommentListPo;
import com.xu.home.param.blog.po.blog.CommentPo;
import com.xu.home.param.blog.vo.blog.CommentVo;
import com.xu.home.service.blog.BlogCommentService;
import com.xu.home.param.common.response.Response;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 博客评论 Service 实现类
 */
@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment>
        implements BlogCommentService {

    private final BlogArticleInfoMapper articleMapper;

    public BlogCommentServiceImpl(BlogArticleInfoMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public Response addComment(CommentPo po) {
        if (po == null || po.getArticleId() == null || StringUtils.isEmpty(po.getContent())) {
            return Response.error("参数不能为空");
        }

        // 验证文章是否存在
        BlogArticleInfo article = articleMapper.selectById(po.getArticleId());
        if (article == null || article.getStatus() != 1) {
            return Response.error("文章不存在或未发布");
        }

        // 如果是回复，验证父评论是否存在
        if (po.getParentId() != null && po.getParentId() > 0) {
            BlogComment parent = baseMapper.selectById(po.getParentId());
            if (parent == null || parent.getIsDelete() == 1) {
                return Response.error("被回复的评论不存在");
            }
        }

        BlogComment comment = new BlogComment();
        BeanUtils.copyProperties(po, comment);
        comment.setParentId(po.getParentId() == null ? 0 : po.getParentId());
        comment.setIsDelete(0);

        boolean result = save(comment);
        return Response.checkResult(result);
    }

    @Override
    public Response listCommentsByArticle(CommentListPo po) {
        if (po == null || po.getArticleId() == null) {
            return Response.error("文章ID不能为空");
        }

        // 查询所有评论
        QueryWrapper<BlogComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("article_id", po.getArticleId())
                .eq("is_delete", 0)
                .orderByDesc("create_time");

        List<BlogComment> allComments = list(queryWrapper);

        // 转换为VO并构建树形结构
        List<CommentVo> voList = allComments.stream().map(comment -> {
            var vo = new CommentVo();
            BeanUtils.copyProperties(comment, vo);
            return vo;
        }).toList();

        // 构建树形结构
        List<CommentVo> treeComments = buildCommentTree(voList);

        return Response.success(treeComments);
    }

    @Override
    public Response listLatestComments(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        QueryWrapper<BlogComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0)
                .orderByDesc("create_time")
                .last("LIMIT " + limit);

        List<BlogComment> comments = list(queryWrapper);

        // 获取文章标题
        List<CommentVo> voList = comments.stream().map(comment -> {
            var vo = new CommentVo();
            BeanUtils.copyProperties(comment, vo);

            BlogArticleInfo article = articleMapper.selectById(comment.getArticleId());
            if (article != null) {
                vo.setArticleTitle(article.getTitle());
            }
            return vo;
        }).toList();

        return Response.success(voList);
    }

    @Override
    public Response deleteComment(Integer id, String account) {
        BlogComment comment = new BlogComment();
        comment.setIsDelete(1);

        int update = baseMapper.update(comment, new QueryWrapper<BlogComment>()
                .eq("id", id)
                .eq("commenter_account", account));

        return Response.checkResult(update == 1);
    }

    /**
     * 构建评论树形结构
     */
    private List<CommentVo> buildCommentTree(List<CommentVo> comments) {
        var commentMap = new HashMap<Integer, CommentVo>();
        var rootComments = new ArrayList<CommentVo>();

        // 先将所有评论放入Map
        for (CommentVo comment : comments) {
            comment.setReplies(new ArrayList<>());
            commentMap.put(comment.getId(), comment);
        }

        // 构建树形结构
        for (CommentVo comment : comments) {
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                rootComments.add(comment);
            } else {
                CommentVo parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getReplies().add(comment);
                }
            }
        }

        return rootComments;
    }
}
