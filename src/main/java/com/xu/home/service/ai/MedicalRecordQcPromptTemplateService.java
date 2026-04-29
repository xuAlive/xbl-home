package com.xu.home.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.medical.MedicalRecordQcPromptTemplate;
import com.xu.home.param.blog.po.ai.MedicalRecordCaseQcPO;
import com.xu.home.param.blog.po.ai.MedicalRecordQcPromptTemplatePO;
import com.xu.home.param.blog.vo.ai.MedicalRecordQcModelOptionVO;

import java.util.List;

public interface MedicalRecordQcPromptTemplateService extends IService<MedicalRecordQcPromptTemplate> {

    List<MedicalRecordQcPromptTemplate> listAvailableTemplates(String account);

    MedicalRecordQcPromptTemplate getTemplateForQc(String account, Long templateId);

    MedicalRecordQcPromptTemplate saveTemplate(String account, MedicalRecordQcPromptTemplatePO po);

    boolean deleteTemplate(String account, Long id);

    List<MedicalRecordQcModelOptionVO> getModelOptions();

    String renderPrompt(MedicalRecordQcPromptTemplate template, MedicalRecordCaseQcPO po);
}
