package com.xu.home.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xu.home.config.blog.QwenLangChain4jFactory;
import com.xu.home.domain.medical.MedicalRecordQcPromptTemplate;
import com.xu.home.param.blog.po.ai.MedicalRecordCaseQcPO;
import com.xu.home.param.blog.vo.ai.MedicalRecordCaseQcVO;
import com.xu.home.param.blog.vo.ai.MedicalRecordQcKnowledgeReferenceVO;
import com.xu.home.param.blog.vo.ai.MedicalRecordQcSectionVO;
import com.xu.home.service.ai.qwen.MedicalKnowledgeQcToolFactory;
import com.xu.home.service.ai.qwen.QwenMedicalCaseQcAssistant;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MedicalRecordCaseQcService {

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    private final QwenLangChain4jFactory qwenLangChain4jFactory;
    private final MedicalKnowledgeQcToolFactory medicalKnowledgeQcToolFactory;
    private final MedicalRecordQcPromptTemplateService promptTemplateService;

    public MedicalRecordCaseQcService(QwenLangChain4jFactory qwenLangChain4jFactory,
                                      MedicalKnowledgeQcToolFactory medicalKnowledgeQcToolFactory,
                                      MedicalRecordQcPromptTemplateService promptTemplateService) {
        this.qwenLangChain4jFactory = qwenLangChain4jFactory;
        this.medicalKnowledgeQcToolFactory = medicalKnowledgeQcToolFactory;
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 基于千问工具调用能力完成病例质控，并引用医疗知识库作为依据。
     */
    public MedicalRecordCaseQcVO qcCase(String account, MedicalRecordCaseQcPO po) {
        validate(po);

        MedicalRecordQcPromptTemplate template = promptTemplateService.getTemplateForQc(account, po.getPromptTemplateId());
        String modelName = StringUtils.defaultIfBlank(StringUtils.trim(po.getModelName()), template.getModelName());
        MedicalRecordCaseQcVO fallback = buildFallbackResult(po);
        fallback.setModelName(modelName);
        fallback.setPromptTemplateId(template.getId());
        fallback.setPromptTemplateName(template.getTemplateName());
        String prompt = promptTemplateService.renderPrompt(template, po);
        log.info("病例问题质控开始, account={}, templateId={}, templateName={}, modelName={}, casePreview={}, department={}",
                account, template.getId(), template.getTemplateName(), modelName,
                abbreviate(resolveCasePreview(po), 100), StringUtils.defaultIfBlank(po.getDepartment(), "-"));
        long startTime = System.currentTimeMillis();
        String modelResult = buildAssistant(account, template.getSystemMessage(), modelName).chat(prompt);
        long costMs = System.currentTimeMillis() - startTime;
        log.info("病例问题质控完成, account={}, templateId={}, modelName={}, costMs={}",
                account, template.getId(), modelName, costMs);

        fallback.setRawModelResult(modelResult);
        MedicalRecordCaseQcVO modelVo = parseModelResult(modelResult);
        mergeModelResult(fallback, modelVo);
        finalizeResult(fallback, po);
        return fallback;
    }

    /**
     * 为当前账号创建带医疗知识检索工具的千问 Assistant。
     */
    private QwenMedicalCaseQcAssistant buildAssistant(String account, String systemMessage, String modelName) {
        return AiServices.builder(QwenMedicalCaseQcAssistant.class)
                .chatModel(qwenLangChain4jFactory.createChatModel(modelName))
                .tools(medicalKnowledgeQcToolFactory.create(account))
                .systemMessage(systemMessage)
                .build();
    }

    /**
     * 在模型不可用前先做基础兜底，至少给出可解释的本地质控结论。
     */
    private MedicalRecordCaseQcVO buildFallbackResult(MedicalRecordCaseQcPO po) {
        MedicalRecordCaseQcVO result = new MedicalRecordCaseQcVO();
        result.setFullMedicalRecord(StringUtils.trimToEmpty(po.getFullMedicalRecord()));
        result.setChiefComplaint(StringUtils.trimToEmpty(po.getChiefComplaint()));
        result.setPresentIllness(StringUtils.trimToEmpty(po.getPresentIllness()));
        result.setPreliminaryDiagnosis(StringUtils.trimToEmpty(po.getPreliminaryDiagnosis()));
        result.setDepartment(StringUtils.trimToEmpty(po.getDepartment()));

        if (!containsDuration(po.getChiefComplaint())) {
            result.getProblems().add("主诉可能缺少持续时间");
        }
        if (StringUtils.isBlank(po.getPresentIllness())) {
            result.getProblems().add("现病史为空，无法支撑主诉与诊断");
        }
        if (StringUtils.isBlank(po.getPreliminaryDiagnosis())) {
            result.getProblems().add("未提供诊断信息，无法判断诊断一致性");
        }
        if (StringUtils.isBlank(po.getDepartment())) {
            result.getSuggestions().add("建议补充科室信息，便于结合专科知识进行质控");
        }

        result.setSummary("已完成基础规则质控，等待模型结合医疗知识库给出更完整结论");
        result.setSections(buildFallbackSections(po));
        return result;
    }

    /**
     * 从模型输出中提取 JSON 并解析为结构化结果。
     */
    private MedicalRecordCaseQcVO parseModelResult(String modelResult) {
        if (StringUtils.isBlank(modelResult)) {
            return null;
        }
        Matcher matcher = JSON_PATTERN.matcher(modelResult);
        if (!matcher.find()) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(matcher.group());
            MedicalRecordCaseQcVO result = new MedicalRecordCaseQcVO();
            result.setQualified(jsonObject.getBoolean("qualified"));
            result.setRiskLevel(jsonObject.getString("riskLevel"));
            result.setSummary(jsonObject.getString("summary"));
            if (jsonObject.containsKey("sections")) {
                result.setSections(parseSections(jsonObject.getJSONArray("sections")));
            }
            if (jsonObject.containsKey("problems")) {
                result.setProblems(jsonObject.getList("problems", String.class));
            }
            if (jsonObject.containsKey("suggestions")) {
                result.setSuggestions(jsonObject.getList("suggestions", String.class));
            }
            if (jsonObject.containsKey("knowledgeReferences")) {
                result.setKnowledgeReferences(parseKnowledgeReferences(jsonObject.getJSONArray("knowledgeReferences")));
            }
            return result;
        } catch (Exception ex) {
            log.warn("病例问题质控结果解析失败, raw={}", abbreviate(modelResult, 500), ex);
            return null;
        }
    }

    /**
     * 解析知识引用列表，保留页面展示和审计需要的关键字段。
     */
    private java.util.List<MedicalRecordQcKnowledgeReferenceVO> parseKnowledgeReferences(JSONArray references) {
        java.util.List<MedicalRecordQcKnowledgeReferenceVO> result = new java.util.ArrayList<>();
        if (references == null) {
            return result;
        }
        for (int i = 0; i < references.size(); i++) {
            JSONObject item = references.getJSONObject(i);
            if (item == null) {
                continue;
            }
            MedicalRecordQcKnowledgeReferenceVO reference = new MedicalRecordQcKnowledgeReferenceVO();
            reference.setItemId(item.getLong("itemId"));
            reference.setTitle(item.getString("title"));
            reference.setSourceName(item.getString("sourceName"));
            reference.setChapterTitle(item.getString("chapterTitle"));
            reference.setQuoteText(item.getString("quoteText"));
            result.add(reference);
        }
        return result;
    }

    /**
     * 解析模型返回的分项质控结构。
     */
    private java.util.List<MedicalRecordQcSectionVO> parseSections(JSONArray sections) {
        java.util.List<MedicalRecordQcSectionVO> result = new java.util.ArrayList<>();
        if (sections == null) {
            return result;
        }
        for (int i = 0; i < sections.size(); i++) {
            JSONObject item = sections.getJSONObject(i);
            if (item == null) {
                continue;
            }
            MedicalRecordQcSectionVO section = new MedicalRecordQcSectionVO();
            section.setSectionCode(item.getString("sectionCode"));
            section.setSectionName(item.getString("sectionName"));
            section.setPassed(item.getBoolean("passed"));
            section.setConclusion(item.getString("conclusion"));
            if (item.containsKey("problems")) {
                section.setProblems(item.getList("problems", String.class));
            }
            if (item.containsKey("suggestions")) {
                section.setSuggestions(item.getList("suggestions", String.class));
            }
            result.add(section);
        }
        return result;
    }

    /**
     * 合并模型结果与规则兜底结果，优先保留更完整的信息。
     */
    private void mergeModelResult(MedicalRecordCaseQcVO fallback, MedicalRecordCaseQcVO modelVo) {
        if (modelVo == null) {
            return;
        }
        if (StringUtils.isNotBlank(modelVo.getModelName())) {
            fallback.setModelName(modelVo.getModelName());
        }
        if (modelVo.getQualified() != null) {
            fallback.setQualified(modelVo.getQualified());
        }
        if (StringUtils.isNotBlank(modelVo.getRiskLevel())) {
            fallback.setRiskLevel(modelVo.getRiskLevel().trim().toLowerCase());
        }
        if (StringUtils.isNotBlank(modelVo.getSummary())) {
            fallback.setSummary(modelVo.getSummary().trim());
        }

        Set<String> mergedProblems = new LinkedHashSet<>(fallback.getProblems());
        if (modelVo.getProblems() != null) {
            mergedProblems.addAll(modelVo.getProblems());
        }
        fallback.setProblems(new java.util.ArrayList<>(mergedProblems));

        Set<String> mergedSuggestions = new LinkedHashSet<>(fallback.getSuggestions());
        if (modelVo.getSuggestions() != null) {
            mergedSuggestions.addAll(modelVo.getSuggestions());
        }
        fallback.setSuggestions(new java.util.ArrayList<>(mergedSuggestions));

        if (modelVo.getKnowledgeReferences() != null && !modelVo.getKnowledgeReferences().isEmpty()) {
            fallback.setKnowledgeReferences(modelVo.getKnowledgeReferences());
        }
        if (modelVo.getSections() != null && !modelVo.getSections().isEmpty()) {
            fallback.setSections(modelVo.getSections());
        }
    }

    /**
     * 补齐最终状态字段，保证接口返回稳定。
     */
    private void finalizeResult(MedicalRecordCaseQcVO result, MedicalRecordCaseQcPO po) {
        if (StringUtils.isBlank(result.getRiskLevel())) {
            result.setRiskLevel(result.getProblems().isEmpty() ? "low" : result.getProblems().size() > 2 ? "high" : "medium");
        }
        if (result.getQualified() == null) {
            result.setQualified(result.getProblems().isEmpty());
        }
        if (StringUtils.isBlank(result.getSummary())) {
            result.setSummary(result.getQualified() ? "病例基础结构基本完整，可继续进行进一步专科质控" : "病例存在结构或内容问题，建议根据问题列表补充修正");
        }
        if (result.getSuggestions().isEmpty()) {
            result.getSuggestions().add(result.getQualified()
                    ? "建议继续结合检验检查、治疗经过和出院诊断做全流程内涵质控"
                    : "建议结合知识引用逐项修正病例描述，确保主诉、现病史和诊断相互一致");
        }
        if (result.getSections() == null || result.getSections().isEmpty()) {
            result.setSections(buildFallbackSections(po));
        }

        result.setQualifiedText(resolveQualifiedText(result.getQualified()));
        result.setRiskLevelText(resolveRiskLevelText(result.getRiskLevel()));
        for (MedicalRecordQcSectionVO section : result.getSections()) {
            if (section == null) {
                continue;
            }
            section.setPassedText(resolveSectionPassedText(section.getPassed()));
        }
    }

    /**
     * 基于基础规则生成面向医生查看的分项质控结果。
     */
    private java.util.List<MedicalRecordQcSectionVO> buildFallbackSections(MedicalRecordCaseQcPO po) {
        java.util.List<MedicalRecordQcSectionVO> sections = new java.util.ArrayList<>();

        MedicalRecordQcSectionVO chiefComplaintSection = new MedicalRecordQcSectionVO();
        chiefComplaintSection.setSectionCode("chiefComplaint");
        chiefComplaintSection.setSectionName("主诉质控");
        boolean chiefComplaintPassed = StringUtils.isNotBlank(po.getChiefComplaint()) && containsDuration(po.getChiefComplaint());
        chiefComplaintSection.setPassed(chiefComplaintPassed);
        chiefComplaintSection.setConclusion(chiefComplaintPassed ? "主诉基本包含主要症状与持续时间表达" : "主诉结构可能不完整，需补充症状或持续时间");
        if (!chiefComplaintPassed) {
            chiefComplaintSection.getProblems().add("主诉建议按“主要症状+持续时间”格式描述");
            chiefComplaintSection.getSuggestions().add("例如可写为“胸闷3天”“腹痛6小时”");
        }
        sections.add(chiefComplaintSection);

        MedicalRecordQcSectionVO presentIllnessSection = new MedicalRecordQcSectionVO();
        presentIllnessSection.setSectionCode("presentIllness");
        presentIllnessSection.setSectionName("现病史质控");
        boolean presentIllnessPassed = StringUtils.isNotBlank(po.getPresentIllness());
        presentIllnessSection.setPassed(presentIllnessPassed);
        presentIllnessSection.setConclusion(presentIllnessPassed ? "已提供现病史，可继续结合医学知识做一致性判断" : "未提供现病史，无法完成完整病例质控");
        if (!presentIllnessPassed) {
            presentIllnessSection.getProblems().add("现病史缺失");
            presentIllnessSection.getSuggestions().add("建议补充起病时间、演变过程、伴随症状及诊疗经过");
        }
        sections.add(presentIllnessSection);

        MedicalRecordQcSectionVO diagnosisSection = new MedicalRecordQcSectionVO();
        diagnosisSection.setSectionCode("diagnosis");
        diagnosisSection.setSectionName("诊断一致性质控");
        boolean diagnosisPassed = StringUtils.isNotBlank(po.getPreliminaryDiagnosis());
        diagnosisSection.setPassed(diagnosisPassed);
        diagnosisSection.setConclusion(diagnosisPassed ? "已提供诊断信息，可结合病史和知识库判断支撑性" : "未提供诊断，无法判断病例与诊断是否一致");
        if (!diagnosisPassed) {
            diagnosisSection.getProblems().add("诊断信息缺失");
            diagnosisSection.getSuggestions().add("建议填写初步诊断或主要诊断后再进行病例一致性质控");
        }
        sections.add(diagnosisSection);

        MedicalRecordQcSectionVO knowledgeSection = new MedicalRecordQcSectionVO();
        knowledgeSection.setSectionCode("knowledge");
        knowledgeSection.setSectionName("知识依据与整改建议");
        knowledgeSection.setPassed(true);
        knowledgeSection.setConclusion("建议结合医疗知识库检索结果确认病例表述是否符合专科规范");
        knowledgeSection.getSuggestions().add("优先关注与当前科室、主诉和诊断相关的知识条目");
        sections.add(knowledgeSection);

        return sections;
    }

    /**
     * 将布尔型质控结果转换为医生易读的中文状态。
     */
    private String resolveQualifiedText(Boolean qualified) {
        return Boolean.TRUE.equals(qualified) ? "通过质控" : "需补充修正";
    }

    /**
     * 将风险等级代码转换为医生易读的中文等级。
     */
    private String resolveRiskLevelText(String riskLevel) {
        if ("high".equalsIgnoreCase(riskLevel)) {
            return "高风险";
        }
        if ("medium".equalsIgnoreCase(riskLevel)) {
            return "中风险";
        }
        return "低风险";
    }

    /**
     * 将分项通过状态转换为医生查看时的短文案。
     */
    private String resolveSectionPassedText(Boolean passed) {
        return Boolean.TRUE.equals(passed) ? "通过" : "需修正";
    }

    /**
     * 校验病例输入，避免无效请求进入模型。
     */
    private void validate(MedicalRecordCaseQcPO po) {
        if (po == null) {
            throw new IllegalArgumentException("病例信息不能为空");
        }
        if (StringUtils.isBlank(po.getFullMedicalRecord()) && StringUtils.isBlank(po.getChiefComplaint())) {
            throw new IllegalArgumentException("完整病例或主诉不能为空");
        }
    }

    private String resolveCasePreview(MedicalRecordCaseQcPO po) {
        return StringUtils.defaultIfBlank(po.getFullMedicalRecord(), po.getChiefComplaint());
    }

    /**
     * 简单判断主诉中是否含有持续时间表达。
     */
    private boolean containsDuration(String chiefComplaint) {
        if (StringUtils.isBlank(chiefComplaint)) {
            return false;
        }
        return chiefComplaint.matches(".*(分钟|小时|天|日|周|星期|月|年|今晨|今日|昨天|昨晚|近日|半年).*");
    }

    /**
     * 截断长日志内容，避免病例原文和模型原文刷屏。
     */
    private String abbreviate(String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String normalized = value.replace("\n", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
