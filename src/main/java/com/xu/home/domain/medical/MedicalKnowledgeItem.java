package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medical_knowledge_item")
public class MedicalKnowledgeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceId;

    private String itemType;

    private String title;

    private String keywords;

    private String department;

    private String summary;

    private String content;

    private String structuredData;

    private BigDecimal confidenceScore;

    private String dedupKey;

    private Integer reviewStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
