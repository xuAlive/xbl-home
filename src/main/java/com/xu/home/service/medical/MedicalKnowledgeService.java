package com.xu.home.service.medical;

import com.xu.home.domain.medical.MedicalKnowledgeSource;
import com.xu.home.param.medical.po.MedicalKnowledgeItemQueryPO;
import com.xu.home.param.medical.vo.MedicalKnowledgeImportResultVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeItemDetailVO;
import com.xu.home.param.medical.vo.MedicalKnowledgePageVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeSourceDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MedicalKnowledgeService {

    MedicalKnowledgeImportResultVO importFromLocal(String account, String localPath);

    MedicalKnowledgeImportResultVO importFromUpload(String account, MultipartFile file);

    void reextract(String account, Long sourceId);

    List<MedicalKnowledgeSource> getSourceList(String account);

    MedicalKnowledgeSourceDetailVO getSourceDetail(String account, Long sourceId);

    /**
     * 查询当前账号下已生成的知识类型列表，可按来源过滤。
     */
    List<String> getItemTypes(String account, Long sourceId);

    MedicalKnowledgePageVO<com.xu.home.domain.medical.MedicalKnowledgeItem> getItemPage(String account, MedicalKnowledgeItemQueryPO po);

    MedicalKnowledgeItemDetailVO getItemDetail(String account, Long itemId);

    boolean deleteItem(String account, Long itemId);
}
