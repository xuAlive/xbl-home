package com.xu.home.param.blog.po.ai;

import lombok.Data;

@Data
public class MedicalRecordCaseQcPO {

    /**
     * 完整病例文本，优先作为模型解析与质控输入。
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
     * 本次质控选择的 Prompt 模板 ID。
     */
    private Long promptTemplateId;

    /**
     * 本次质控选择的模型名称，为空时使用模板或配置默认模型。
     */
    private String modelName;
}
