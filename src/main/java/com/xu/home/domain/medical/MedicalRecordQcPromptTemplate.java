package com.xu.home.domain.medical;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_record_qc_prompt_template")
public class MedicalRecordQcPromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String templateName;

    private String sceneCode;

    private String modelProvider;

    private String modelName;

    private String systemMessage;

    private String promptTemplate;

    private Integer defaultFlag;

    private Integer enabled;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
