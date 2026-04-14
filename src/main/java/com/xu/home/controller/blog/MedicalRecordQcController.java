package com.xu.home.controller.blog;

import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.param.blog.po.ai.MedicalRecordChiefComplaintQcPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.service.ai.MedicalRecordQcService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blog/ai/medical-record")
public class MedicalRecordQcController {

    private final MedicalRecordQcService medicalRecordQcService;

    public MedicalRecordQcController(MedicalRecordQcService medicalRecordQcService) {
        this.medicalRecordQcService = medicalRecordQcService;
    }

    @PostMapping("/chief-complaint/qc")
    @RequirePermission("deepseek:chat")
    public Response chiefComplaintQc(@RequestBody MedicalRecordChiefComplaintQcPO po) {
        return Response.success(medicalRecordQcService.qcChiefComplaint(po.getChiefComplaint()));
    }
}
