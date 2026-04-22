package com.xu.home.param.crawler.vo;

import lombok.Data;

/**
 * 书籍章节预览
 */
@Data
public class BookCrawlerPreviewChapterVO {

    private Integer chapterIndex;

    private String chapterTitle;

    private String chapterUrl;
}
