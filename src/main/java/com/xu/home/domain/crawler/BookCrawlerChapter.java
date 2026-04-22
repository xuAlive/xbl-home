package com.xu.home.domain.crawler;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书籍爬虫章节记录
 */
@Data
@TableName("book_crawler_chapter")
public class BookCrawlerChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer chapterIndex;

    private String chapterTitle;

    private String chapterUrl;

    private String savePath;

    private Integer contentLength;

    /**
     * 1-待抓取 2-成功 3-失败
     */
    private Integer crawlStatus;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
