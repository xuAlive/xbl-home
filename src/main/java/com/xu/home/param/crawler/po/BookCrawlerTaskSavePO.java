package com.xu.home.param.crawler.po;

import lombok.Data;

/**
 * 书籍爬虫任务保存参数
 */
@Data
public class BookCrawlerTaskSavePO {

    private Long id;

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
}
