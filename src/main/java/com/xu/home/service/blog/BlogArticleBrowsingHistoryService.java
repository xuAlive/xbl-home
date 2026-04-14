package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.BlogArticleBrowsingHistory;
import com.xu.home.param.common.response.Response;

public interface BlogArticleBrowsingHistoryService extends IService<BlogArticleBrowsingHistory> {

    void recordView(String account, Integer articleId);

    Response listBrowsingHistory(String account);

    Response deleteBrowsingHistory(String account, Integer id);

    Response clearBrowsingHistory(String account);
}
