package com.xu.home.param.blog.po.ai;

import lombok.Data;

@Data
public class MedicalRecordQcPromptTemplatePO {

    private Long id;

    private String templateName;

    private String sceneCode;

    private String modelProvider;

    private String modelName;

    private String systemMessage;

    private String promptTemplate;

    private Integer defaultFlag;

    private Integer enabled;

    private Integer sortOrder;
}
