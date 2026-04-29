package com.xu.home.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.config.blog.QwenProperties;
import com.xu.home.domain.medical.MedicalRecordQcPromptTemplate;
import com.xu.home.mapper.medical.MedicalRecordQcPromptTemplateMapper;
import com.xu.home.param.blog.po.ai.MedicalRecordCaseQcPO;
import com.xu.home.param.blog.po.ai.MedicalRecordQcPromptTemplatePO;
import com.xu.home.param.blog.vo.ai.MedicalRecordQcModelOptionVO;
import com.xu.home.service.ai.MedicalRecordQcPromptTemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicalRecordQcPromptTemplateServiceImpl
        extends ServiceImpl<MedicalRecordQcPromptTemplateMapper, MedicalRecordQcPromptTemplate>
        implements MedicalRecordQcPromptTemplateService {

    private static final String GLOBAL_ACCOUNT = "";
    private static final String DEFAULT_SCENE_CODE = "medical_case_qc";
    private static final String DEFAULT_PROVIDER = "qwen";

    private final QwenProperties qwenProperties;

    public MedicalRecordQcPromptTemplateServiceImpl(QwenProperties qwenProperties) {
        this.qwenProperties = qwenProperties;
    }

    @Override
    public List<MedicalRecordQcPromptTemplate> listAvailableTemplates(String account) {
        return lambdaQuery()
                .eq(MedicalRecordQcPromptTemplate::getIsDelete, 0)
                .and(w -> w.eq(MedicalRecordQcPromptTemplate::getAccount, normalizeAccount(account))
                        .or().eq(MedicalRecordQcPromptTemplate::getAccount, GLOBAL_ACCOUNT))
                .orderByDesc(MedicalRecordQcPromptTemplate::getDefaultFlag)
                .orderByAsc(MedicalRecordQcPromptTemplate::getSortOrder)
                .orderByDesc(MedicalRecordQcPromptTemplate::getUpdateTime)
                .list();
    }

    @Override
    public MedicalRecordQcPromptTemplate getTemplateForQc(String account, Long templateId) {
        if (templateId != null) {
            MedicalRecordQcPromptTemplate template = getById(templateId);
            if (isVisible(account, template) && isEnabled(template)) {
                return template;
            }
            throw new IllegalArgumentException("质控模板不存在或不可用");
        }
        MedicalRecordQcPromptTemplate defaultTemplate = lambdaQuery()
                .eq(MedicalRecordQcPromptTemplate::getIsDelete, 0)
                .eq(MedicalRecordQcPromptTemplate::getEnabled, 1)
                .and(w -> w.eq(MedicalRecordQcPromptTemplate::getAccount, normalizeAccount(account))
                        .or().eq(MedicalRecordQcPromptTemplate::getAccount, GLOBAL_ACCOUNT))
                .orderByDesc(MedicalRecordQcPromptTemplate::getDefaultFlag)
                .orderByAsc(MedicalRecordQcPromptTemplate::getSortOrder)
                .orderByDesc(MedicalRecordQcPromptTemplate::getUpdateTime)
                .last("limit 1")
                .one();
        if (defaultTemplate == null) {
            throw new IllegalArgumentException("未配置可用的病例质控模板");
        }
        return defaultTemplate;
    }

    @Override
    public MedicalRecordQcPromptTemplate saveTemplate(String account, MedicalRecordQcPromptTemplatePO po) {
        validate(po);
        String owner = normalizeAccount(account);
        MedicalRecordQcPromptTemplate template;
        if (po.getId() == null) {
            template = new MedicalRecordQcPromptTemplate();
            template.setAccount(owner);
            template.setCreateTime(LocalDateTime.now());
            template.setIsDelete(0);
        } else {
            template = getById(po.getId());
            if (template == null || template.getIsDelete() != null && template.getIsDelete() == 1) {
                throw new IllegalArgumentException("质控模板不存在");
            }
            if (!StringUtils.equals(template.getAccount(), owner)) {
                throw new IllegalArgumentException("不能修改系统模板或其他账号模板");
            }
        }

        template.setTemplateName(StringUtils.trim(po.getTemplateName()));
        template.setSceneCode(StringUtils.defaultIfBlank(StringUtils.trim(po.getSceneCode()), DEFAULT_SCENE_CODE));
        template.setModelProvider(StringUtils.defaultIfBlank(StringUtils.trim(po.getModelProvider()), DEFAULT_PROVIDER));
        template.setModelName(StringUtils.defaultIfBlank(StringUtils.trim(po.getModelName()), qwenProperties.resolveLangChainModelName()));
        template.setSystemMessage(StringUtils.trim(po.getSystemMessage()));
        template.setPromptTemplate(StringUtils.trim(po.getPromptTemplate()));
        template.setDefaultFlag(po.getDefaultFlag() == null ? 0 : po.getDefaultFlag());
        template.setEnabled(po.getEnabled() == null ? 1 : po.getEnabled());
        template.setSortOrder(po.getSortOrder() == null ? 100 : po.getSortOrder());
        template.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(template);
        return template;
    }

    @Override
    public boolean deleteTemplate(String account, Long id) {
        MedicalRecordQcPromptTemplate template = getById(id);
        if (template == null || template.getIsDelete() != null && template.getIsDelete() == 1) {
            return false;
        }
        if (!StringUtils.equals(template.getAccount(), normalizeAccount(account))) {
            throw new IllegalArgumentException("不能删除系统模板或其他账号模板");
        }
        template.setIsDelete(1);
        template.setUpdateTime(LocalDateTime.now());
        return updateById(template);
    }

    @Override
    public List<MedicalRecordQcModelOptionVO> getModelOptions() {
        String configured = qwenProperties.resolveLangChainModelName();
        Map<String, MedicalRecordQcModelOptionVO> options = new LinkedHashMap<>();
        addModelOption(options, configured, configured + "（当前配置）");
        addModelOption(options, "qwen-plus", "通义千问 Plus");
        addModelOption(options, "qwen-max", "通义千问 Max");
        addModelOption(options, "qwen-turbo", "通义千问 Turbo");
        return List.copyOf(options.values());
    }

    @Override
    public String renderPrompt(MedicalRecordQcPromptTemplate template, MedicalRecordCaseQcPO po) {
        String prompt = template.getPromptTemplate();
        Map<String, String> variables = Map.of(
                "medicalRecord", buildMedicalRecordText(po),
                "chiefComplaint", StringUtils.defaultIfBlank(po.getChiefComplaint(), "未提供"),
                "presentIllness", StringUtils.defaultIfBlank(po.getPresentIllness(), "未提供"),
                "preliminaryDiagnosis", StringUtils.defaultIfBlank(po.getPreliminaryDiagnosis(), "未提供"),
                "department", StringUtils.defaultIfBlank(po.getDepartment(), "未提供")
        );
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return prompt;
    }

    private void validate(MedicalRecordQcPromptTemplatePO po) {
        if (po == null) {
            throw new IllegalArgumentException("质控模板不能为空");
        }
        if (StringUtils.isBlank(po.getTemplateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (StringUtils.isBlank(po.getSystemMessage())) {
            throw new IllegalArgumentException("System Message 不能为空");
        }
        if (StringUtils.isBlank(po.getPromptTemplate())) {
            throw new IllegalArgumentException("Prompt 模板不能为空");
        }
    }

    private boolean isVisible(String account, MedicalRecordQcPromptTemplate template) {
        if (template == null || template.getIsDelete() != null && template.getIsDelete() == 1) {
            return false;
        }
        return StringUtils.equals(template.getAccount(), GLOBAL_ACCOUNT)
                || StringUtils.equals(template.getAccount(), normalizeAccount(account));
    }

    private boolean isEnabled(MedicalRecordQcPromptTemplate template) {
        return template.getEnabled() == null || template.getEnabled() == 1;
    }

    private void addModelOption(Map<String, MedicalRecordQcModelOptionVO> options, String value, String label) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        options.putIfAbsent(value, new MedicalRecordQcModelOptionVO(label, value, DEFAULT_PROVIDER));
    }

    private String buildMedicalRecordText(MedicalRecordCaseQcPO po) {
        if (StringUtils.isNotBlank(po.getFullMedicalRecord())) {
            return po.getFullMedicalRecord().trim();
        }
        return """
                主诉：%s
                现病史：%s
                初步诊断：%s
                科室：%s
                """.formatted(
                StringUtils.defaultIfBlank(po.getChiefComplaint(), "未提供"),
                StringUtils.defaultIfBlank(po.getPresentIllness(), "未提供"),
                StringUtils.defaultIfBlank(po.getPreliminaryDiagnosis(), "未提供"),
                StringUtils.defaultIfBlank(po.getDepartment(), "未提供")
        ).trim();
    }

    private String normalizeAccount(String account) {
        return StringUtils.defaultString(account);
    }
}
