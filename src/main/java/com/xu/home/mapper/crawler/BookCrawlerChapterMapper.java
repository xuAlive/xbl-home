package com.xu.home.mapper.crawler;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.crawler.BookCrawlerChapter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 书籍爬虫章节 Mapper
 */
@Mapper
public interface BookCrawlerChapterMapper extends BaseMapper<BookCrawlerChapter> {
}
