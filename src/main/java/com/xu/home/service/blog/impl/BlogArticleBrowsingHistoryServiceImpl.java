package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BlogArticleBrowsingHistory;
import com.xu.home.mapper.blog.BlogArticleBrowsingHistoryMapper;
import com.xu.home.param.blog.vo.blog.BlogArticleBrowsingHistoryVo;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.blog.BlogArticleBrowsingHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class BlogArticleBrowsingHistoryServiceImpl extends ServiceImpl<BlogArticleBrowsingHistoryMapper, BlogArticleBrowsingHistory>
        implements BlogArticleBrowsingHistoryService {

    @Override
    public void recordView(String account, Integer articleId) {
        if (!StringUtils.hasText(account) || articleId == null) {
            return;
        }

        Date now = new Date();
        BlogArticleBrowsingHistory history = getOne(new LambdaQueryWrapper<BlogArticleBrowsingHistory>()
                .eq(BlogArticleBrowsingHistory::getAccount, account)
                .eq(BlogArticleBrowsingHistory::getArticleId, articleId)
                .last("limit 1"));

        if (history == null) {
            history = new BlogArticleBrowsingHistory();
            history.setAccount(account);
            history.setArticleId(articleId);
            history.setLastViewTime(now);
            history.setCreateTime(now);
            history.setUpdateTime(now);
            save(history);
            return;
        }

        history.setLastViewTime(now);
        history.setUpdateTime(now);
        updateById(history);
    }

    @Override
    public Response listBrowsingHistory(String account) {
        if (!StringUtils.hasText(account)) {
            return Response.error("请先登录");
        }

        List<BlogArticleBrowsingHistoryVo> list = baseMapper.selectHistoryListByAccount(account);
        return Response.success(list);
    }

    @Override
    public Response deleteBrowsingHistory(String account, Integer id) {
        if (!StringUtils.hasText(account)) {
            return Response.error("请先登录");
        }
        if (id == null) {
            return Response.error("记录ID不能为空");
        }

        boolean removed = remove(new LambdaQueryWrapper<BlogArticleBrowsingHistory>()
                .eq(BlogArticleBrowsingHistory::getId, id)
                .eq(BlogArticleBrowsingHistory::getAccount, account));
        return Response.checkResult(removed);
    }

    @Override
    public Response clearBrowsingHistory(String account) {
        if (!StringUtils.hasText(account)) {
            return Response.error("请先登录");
        }

        remove(new LambdaQueryWrapper<BlogArticleBrowsingHistory>()
                .eq(BlogArticleBrowsingHistory::getAccount, account));
        return Response.success();
    }
}
