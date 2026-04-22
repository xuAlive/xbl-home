package com.xu.home.service.crawler;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.crawler.BookCrawlerChapter;

import java.util.List;

/**
 * 书籍爬虫章节服务
 */
public interface BookCrawlerChapterService extends IService<BookCrawlerChapter> {

    /**
     * 获取任务章节记录
     */
    List<BookCrawlerChapter> getByTaskId(Long taskId);
}
