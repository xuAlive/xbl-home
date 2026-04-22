package com.xu.home.param.medical.po;

import lombok.Data;

@Data
public class MedicalKnowledgeItemQueryPO {

    private Long sourceId;

    private String keyword;

    private String itemType;

    private String department;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
