package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_knowledge_source")
public class MedicalKnowledgeSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String sourceName;

    private Integer sourceType;

    private String fileFormat;

    private Long fileSize;

    private String fileHash;

    private String localPath;

    private String storagePath;

    private Integer parseStatus;

    private Integer extractStatus;

    private Integer chapterCount;

    private Integer chunkCount;

    private Integer knowledgeCount;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
