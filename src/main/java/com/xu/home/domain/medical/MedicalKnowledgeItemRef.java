package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_knowledge_item_ref")
public class MedicalKnowledgeItemRef {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeItemId;

    private Long chunkId;

    private String quoteText;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
