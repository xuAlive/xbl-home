package com.xu.home.domain.crawler;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书籍爬虫任务
 */
@Data
@TableName("book_crawler_task")
public class BookCrawlerTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String taskName;

    private String siteName;

    private String bookName;

    private String catalogUrl;

    private String chapterLinkSelector;

    private String chapterTitleSelector;

    private String contentSelector;

    private String contentRemoveSelectors;

    private Integer startChapterNum;

    private Integer endChapterNum;

    private Integer intervalSeconds;

    private String saveDirectory;

    /**
     * 1-待执行 2-执行中 3-成功 4-部分失败 5-失败 6-已暂停
     */
    private Integer status;

    private Integer totalChapters;

    private Integer successChapters;

    private Integer failedChapters;

    private String lastMessage;

    private LocalDateTime lastStartTime;

    private LocalDateTime lastFinishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
