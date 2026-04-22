package com.xu.home.param.medical.vo;

import com.xu.home.domain.medical.MedicalKnowledgeItem;
import com.xu.home.domain.medical.MedicalKnowledgeSource;
import com.xu.home.domain.medical.MedicalKnowledgeTask;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalKnowledgeSourceDetailVO {

    private MedicalKnowledgeSource source;

    private List<MedicalKnowledgeTask> tasks = new ArrayList<>();

    private List<MedicalKnowledgeItem> latestItems = new ArrayList<>();
}
