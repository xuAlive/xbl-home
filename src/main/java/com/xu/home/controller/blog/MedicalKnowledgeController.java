package com.xu.home.controller.blog;

import com.xu.home.param.common.IdPO;
import com.xu.home.param.common.response.Response;
import com.xu.home.param.medical.po.MedicalKnowledgeItemQueryPO;
import com.xu.home.param.medical.po.MedicalKnowledgeLocalImportPO;
import com.xu.home.service.medical.MedicalKnowledgeService;
import com.xu.home.utils.common.SessionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/blog/medical/knowledge")
public class MedicalKnowledgeController {

    private final MedicalKnowledgeService medicalKnowledgeService;

    public MedicalKnowledgeController(MedicalKnowledgeService medicalKnowledgeService) {
        this.medicalKnowledgeService = medicalKnowledgeService;
    }

    @PostMapping("/source/local/import")
    public Response<?> importLocal(@RequestBody MedicalKnowledgeLocalImportPO po) {
        return Response.success(medicalKnowledgeService.importFromLocal(getCurrentAccount(), po.getLocalPath()));
    }

    @PostMapping("/source/upload")
    public Response<?> importUpload(@RequestParam("file") MultipartFile file) {
        return Response.success(medicalKnowledgeService.importFromUpload(getCurrentAccount(), file));
    }

    @PostMapping("/source/reextract")
    public Response<?> reextract(@RequestBody IdPO po) {
        medicalKnowledgeService.reextract(getCurrentAccount(), po.getId());
        return Response.success();
    }

    @GetMapping("/source/list")
    public Response<?> getSourceList() {
        return Response.success(medicalKnowledgeService.getSourceList(getCurrentAccount()));
    }

    @GetMapping("/source/detail/{id}")
    public Response<?> getSourceDetail(@PathVariable("id") Long id) {
        return Response.success(medicalKnowledgeService.getSourceDetail(getCurrentAccount(), id));
    }

    @GetMapping("/item/list")
    public Response<?> getItemList(MedicalKnowledgeItemQueryPO po) {
        return Response.success(medicalKnowledgeService.getItemPage(getCurrentAccount(), po));
    }

    /**
     * 查询当前账号下可用于筛选的知识类型列表。
     */
    @GetMapping("/item/types")
    public Response<?> getItemTypes(@RequestParam(value = "sourceId", required = false) Long sourceId) {
        return Response.success(medicalKnowledgeService.getItemTypes(getCurrentAccount(), sourceId));
    }

    @GetMapping("/item/detail/{id}")
    public Response<?> getItemDetail(@PathVariable("id") Long id) {
        return Response.success(medicalKnowledgeService.getItemDetail(getCurrentAccount(), id));
    }

    @PostMapping("/item/delete")
    public Response<?> deleteItem(@RequestBody IdPO po) {
        return Response.checkResult(medicalKnowledgeService.deleteItem(getCurrentAccount(), po.getId()));
    }

    /**
     * 获取当前登录账号，未登录时直接终止后续业务处理。
     */
    private String getCurrentAccount() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null) {
            throw new RuntimeException("未登录");
        }
        return account;
    }
}
