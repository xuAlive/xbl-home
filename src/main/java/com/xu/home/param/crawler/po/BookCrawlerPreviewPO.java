package com.xu.home.param.crawler.po;

import lombok.Data;

/**
 * 书籍爬虫预览参数
 */
@Data
public class BookCrawlerPreviewPO {

    private String catalogUrl;

    private String chapterLinkSelector;

    private String chapterTitleSelector;

    private String contentSelector;

    private String contentRemoveSelectors;
}
