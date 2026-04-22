package com.xu.home.param.medical.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalKnowledgePageVO<T> {

    private Long total = 0L;

    private List<T> list = new ArrayList<>();
}
