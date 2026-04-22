package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_knowledge_task")
public class MedicalKnowledgeTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceId;

    private Integer taskType;

    private Integer taskStatus;

    private String modelName;

    private String promptVersion;

    private String resultMessage;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
