package com.xu.home.param.blog.vo.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalRecordChiefComplaintQcVO {

    /**
     * 主诉原文
     */
    private String chiefComplaint;

    /**
     * 主要症状
     */
    private String mainSymptom;

    /**
     * 持续时间
     */
    private String duration;

    /**
     * 是否合格
     */
    private Boolean qualified;

    /**
     * 发现的问题
     */
    private List<String> problems = new ArrayList<>();

    /**
     * 质控建议
     */
    private String suggestion;

    /**
     * 模型原始结果，便于调试
     */
    private String rawModelResult;
}
