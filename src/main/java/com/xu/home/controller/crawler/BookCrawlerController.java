package com.xu.home.controller.crawler;

import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.param.crawler.po.BookCrawlerPreviewPO;
import com.xu.home.param.crawler.po.BookCrawlerTaskSavePO;
import com.xu.home.service.crawler.BookCrawlerTaskService;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 书籍爬虫接口
 */
@RestController
@RequestMapping("/crawler/book")
public class BookCrawlerController {

    private final BookCrawlerTaskService bookCrawlerTaskService;

    public BookCrawlerController(BookCrawlerTaskService bookCrawlerTaskService) {
        this.bookCrawlerTaskService = bookCrawlerTaskService;
    }

    @GetMapping("/task/list")
    public Response<?> getTaskList() {
        return Response.success(bookCrawlerTaskService.getTaskList(getCurrentAccount()));
    }

    @GetMapping("/task/detail/{id}")
    public Response<?> getTaskDetail(@PathVariable("id") Long id) {
        return Response.success(bookCrawlerTaskService.getTaskDetail(getCurrentAccount(), id));
    }

    @PostMapping("/task/save")
    public Response<?> saveTask(@RequestBody BookCrawlerTaskSavePO po) {
        return Response.success(bookCrawlerTaskService.saveTask(getCurrentAccount(), po));
    }

    @PostMapping("/task/delete")
    public Response<?> deleteTask(@RequestBody IdPO po) {
        return Response.checkResult(bookCrawlerTaskService.deleteTask(getCurrentAccount(), po.getId()));
    }

    @PostMapping("/task/start")
    public Response<?> startTask(@RequestBody IdPO po) {
        bookCrawlerTaskService.startTask(getCurrentAccount(), po.getId());
        return Response.success();
    }

    @PostMapping("/task/pause")
    public Response<?> pauseTask(@RequestBody IdPO po) {
        bookCrawlerTaskService.pauseTask(getCurrentAccount(), po.getId());
        return Response.success();
    }

    @PostMapping("/preview")
    public Response<?> previewChapters(@RequestBody BookCrawlerPreviewPO po) {
        return Response.success(bookCrawlerTaskService.previewChapters(po));
    }

    private String getCurrentAccount() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            throw new RuntimeException("未登录");
        }
        return account;
    }
}
