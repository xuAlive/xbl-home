package com.xu.home.param.medical.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalKnowledgeImportResultVO {

    private Integer totalFiles;

    private List<Long> sourceIds = new ArrayList<>();

    private List<String> sourceNames = new ArrayList<>();
}
