package com.xu.home.service.crawler;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.crawler.BookCrawlerTask;
import com.xu.home.param.crawler.po.BookCrawlerPreviewPO;
import com.xu.home.param.crawler.po.BookCrawlerTaskSavePO;
import com.xu.home.param.crawler.vo.BookCrawlerPreviewChapterVO;
import com.xu.home.param.crawler.vo.BookCrawlerTaskDetailVO;

import java.util.List;

/**
 * 书籍爬虫任务服务
 */
public interface BookCrawlerTaskService extends IService<BookCrawlerTask> {

    /**
     * 获取当前用户任务列表
     */
    List<BookCrawlerTask> getTaskList(String account);

    /**
     * 保存任务
     */
    Long saveTask(String account, BookCrawlerTaskSavePO po);

    /**
     * 删除任务
     */
    boolean deleteTask(String account, Long taskId);

    /**
     * 获取任务详情
     */
    BookCrawlerTaskDetailVO getTaskDetail(String account, Long taskId);

    /**
     * 预览章节
     */
    List<BookCrawlerPreviewChapterVO> previewChapters(BookCrawlerPreviewPO po);

    /**
     * 启动抓取任务
     */
    void startTask(String account, Long taskId);

    /**
     * 暂停抓取任务
     */
    void pauseTask(String account, Long taskId);
}
