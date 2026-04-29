package com.xu.home.param.blog.vo.ai;

import lombok.Data;

@Data
public class MedicalRecordQcKnowledgeReferenceVO {

    /**
     * 知识条目 ID
     */
    private Long itemId;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 来源书籍
     */
    private String sourceName;

    /**
     * 章节标题
     */
    private String chapterTitle;

    /**
     * 引用片段
     */
    private String quoteText;
}
