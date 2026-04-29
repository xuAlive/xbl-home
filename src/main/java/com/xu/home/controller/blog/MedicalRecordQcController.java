package com.xu.home.controller.blog;

import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.param.blog.po.ai.MedicalRecordQcPromptTemplatePO;
import com.xu.home.param.blog.po.ai.MedicalRecordCaseQcPO;
import com.xu.home.param.blog.po.ai.MedicalRecordChiefComplaintQcPO;
import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.ai.MedicalRecordCaseQcService;
import com.xu.home.service.ai.MedicalRecordQcPromptTemplateService;
import com.xu.home.service.ai.MedicalRecordQcService;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blog/ai/medical-record")
public class MedicalRecordQcController {

    private final MedicalRecordQcService medicalRecordQcService;
    private final MedicalRecordCaseQcService medicalRecordCaseQcService;
    private final MedicalRecordQcPromptTemplateService promptTemplateService;

    public MedicalRecordQcController(MedicalRecordQcService medicalRecordQcService,
                                     MedicalRecordCaseQcService medicalRecordCaseQcService,
                                     MedicalRecordQcPromptTemplateService promptTemplateService) {
        this.medicalRecordQcService = medicalRecordQcService;
        this.medicalRecordCaseQcService = medicalRecordCaseQcService;
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 对病历主诉进行基础结构质控。
     */
    @PostMapping("/chief-complaint/qc")
    @RequirePermission("deepseek:chat")
    public Response chiefComplaintQc(@RequestBody MedicalRecordChiefComplaintQcPO po) {
        return Response.success(medicalRecordQcService.qcChiefComplaint(po.getChiefComplaint()));
    }

    /**
     * 基于千问工具调用和医疗知识库，对病例进行结构化问题质控。
     */
    @PostMapping("/case/qc")
    @RequirePermission("deepseek:chat")
    public Response caseQc(@RequestBody MedicalRecordCaseQcPO po) {
        return Response.success(medicalRecordCaseQcService.qcCase(getCurrentAccount(), po));
    }

    /**
     * 查询当前账号可用的病例质控 Prompt 模板。
     */
    @GetMapping("/case/qc/template/list")
    @RequirePermission("deepseek:chat")
    public Response templateList() {
        return Response.success(promptTemplateService.listAvailableTemplates(getCurrentAccount()));
    }

    /**
     * 保存当前账号的病例质控 Prompt 模板。
     */
    @PostMapping("/case/qc/template/save")
    @RequirePermission("deepseek:chat")
    public Response saveTemplate(@RequestBody MedicalRecordQcPromptTemplatePO po) {
        return Response.success(promptTemplateService.saveTemplate(getCurrentAccount(), po));
    }

    /**
     * 物理业务上停用当前账号自建模板。
     */
    @PostMapping("/case/qc/template/delete")
    @RequirePermission("deepseek:chat")
    public Response deleteTemplate(@RequestBody IdPO po) {
        return Response.checkResult(promptTemplateService.deleteTemplate(getCurrentAccount(), po.getId()));
    }

    /**
     * 查询页面可选择的模型列表。
     */
    @GetMapping("/case/qc/model/options")
    @RequirePermission("deepseek:chat")
    public Response modelOptions() {
        return Response.success(promptTemplateService.getModelOptions());
    }

    /**
     * 获取当前登录账号，未登录时直接中止请求。
     */
    private String getCurrentAccount() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            throw new RuntimeException("未登录");
        }
        return account;
    }
}
