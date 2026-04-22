package com.xu.home.service.crawler.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.crawler.BookCrawlerChapter;
import com.xu.home.mapper.crawler.BookCrawlerChapterMapper;
import com.xu.home.service.crawler.BookCrawlerChapterService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 书籍爬虫章节服务实现
 */
@Service
public class BookCrawlerChapterServiceImpl extends ServiceImpl<BookCrawlerChapterMapper, BookCrawlerChapter>
        implements BookCrawlerChapterService {

    @Override
    public List<BookCrawlerChapter> getByTaskId(Long taskId) {
        LambdaQueryWrapper<BookCrawlerChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookCrawlerChapter::getTaskId, taskId)
                .orderByAsc(BookCrawlerChapter::getChapterIndex);
        return list(wrapper);
    }
}
