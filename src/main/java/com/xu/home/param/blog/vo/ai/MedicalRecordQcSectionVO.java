package com.xu.home.param.blog.vo.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalRecordQcSectionVO {

    /**
     * 分项编码
     */
    private String sectionCode;

    /**
     * 分项名称
     */
    private String sectionName;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 分项状态中文文案
     */
    private String passedText;

    /**
     * 分项结论
     */
    private String conclusion;

    /**
     * 发现的问题
     */
    private List<String> problems = new ArrayList<>();

    /**
     * 处理建议
     */
    private List<String> suggestions = new ArrayList<>();
}
