package com.xu.home.param.blog.vo.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalRecordCaseQcVO {

    /**
     * 完整病例文本
     */
    private String fullMedicalRecord;

    /**
     * 主诉
     */
    private String chiefComplaint;

    /**
     * 现病史
     */
    private String presentIllness;

    /**
     * 初步诊断或主要诊断
     */
    private String preliminaryDiagnosis;

    /**
     * 科室
     */
    private String department;

    /**
     * 本次调用的模型名称
     */
    private String modelName;

    /**
     * 本次使用的 Prompt 模板 ID
     */
    private Long promptTemplateId;

    /**
     * 本次使用的 Prompt 模板名称
     */
    private String promptTemplateName;

    /**
     * 是否通过质控
     */
    private Boolean qualified;

    /**
     * 是否通过质控的中文文案
     */
    private String qualifiedText;

    /**
     * 风险等级：low / medium / high
     */
    private String riskLevel;

    /**
     * 风险等级中文文案
     */
    private String riskLevelText;

    /**
     * 总结
     */
    private String summary;

    /**
     * 发现的问题
     */
    private List<String> problems = new ArrayList<>();

    /**
     * 整改建议
     */
    private List<String> suggestions = new ArrayList<>();

    /**
     * 知识引用
     */
    private List<MedicalRecordQcKnowledgeReferenceVO> knowledgeReferences = new ArrayList<>();

    /**
     * 分项质控结果
     */
    private List<MedicalRecordQcSectionVO> sections = new ArrayList<>();

    /**
     * 模型原始输出
     */
    private String rawModelResult;
}
