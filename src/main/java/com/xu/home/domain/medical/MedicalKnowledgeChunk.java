package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_knowledge_chunk")
public class MedicalKnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceId;

    private Integer chapterNo;

    private String chapterTitle;

    private Integer pageFrom;

    private Integer pageTo;

    private Integer chunkNo;

    private String rawContent;

    private String cleanContent;

    private String contentHash;

    private Integer tokenEstimate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
