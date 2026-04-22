package com.xu.home.param.medical.vo;

import lombok.Data;

@Data
public class MedicalKnowledgeEvidenceVO {

    private Long refId;

    private Long chunkId;

    private Integer chapterNo;

    private String chapterTitle;

    private Integer pageFrom;

    private Integer pageTo;

    private String quoteText;

    private String cleanContent;

    private Integer sortOrder;
}
