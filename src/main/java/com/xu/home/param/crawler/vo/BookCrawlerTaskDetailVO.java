package com.xu.home.param.crawler.vo;

import com.xu.home.domain.crawler.BookCrawlerChapter;
import com.xu.home.domain.crawler.BookCrawlerTask;
import lombok.Data;

import java.util.List;

/**
 * 书籍爬虫任务详情
 */
@Data
public class BookCrawlerTaskDetailVO {

    private BookCrawlerTask task;

    private List<BookCrawlerChapter> chapters;
}
