package com.xu.home.mapper.crawler;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.crawler.BookCrawlerTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 书籍爬虫任务 Mapper
 */
@Mapper
public interface BookCrawlerTaskMapper extends BaseMapper<BookCrawlerTask> {
}
