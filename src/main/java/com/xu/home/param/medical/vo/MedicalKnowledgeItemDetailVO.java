package com.xu.home.param.medical.vo;

import com.xu.home.domain.medical.MedicalKnowledgeItem;
import com.xu.home.domain.medical.MedicalKnowledgeSource;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalKnowledgeItemDetailVO {

    private MedicalKnowledgeItem item;

    private MedicalKnowledgeSource source;

    private List<MedicalKnowledgeEvidenceVO> references = new ArrayList<>();
}
