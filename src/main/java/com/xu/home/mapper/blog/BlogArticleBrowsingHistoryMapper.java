package com.xu.home.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.blog.BlogArticleBrowsingHistory;
import com.xu.home.param.blog.vo.blog.BlogArticleBrowsingHistoryVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BlogArticleBrowsingHistoryMapper extends BaseMapper<BlogArticleBrowsingHistory> {

    List<BlogArticleBrowsingHistoryVo> selectHistoryListByAccount(@Param("account") String account);
}
