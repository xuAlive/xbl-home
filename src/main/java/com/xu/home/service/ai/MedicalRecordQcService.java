package com.xu.home.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xu.home.param.blog.vo.ai.MedicalRecordChiefComplaintQcVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MedicalRecordQcService {

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+\\s*(分钟|小时|天|日|周|星期|月|年)|[一二三四五六七八九十半两]+\\s*(分钟|小时|天|日|周|星期|月|年)|今晨|今日|昨天|昨晚|近日|数日|数周|数月|半年)"
    );

    private final DeepSeekAssistant deepSeekAssistant;

    public MedicalRecordQcService(DeepSeekAssistant deepSeekAssistant) {
        this.deepSeekAssistant = deepSeekAssistant;
    }

    public MedicalRecordChiefComplaintQcVO qcChiefComplaint(String chiefComplaint) {
        if (!StringUtils.hasText(chiefComplaint)) {
            throw new IllegalArgumentException("主诉不能为空");
        }

        MedicalRecordChiefComplaintQcVO result = buildRuleBasedResult(chiefComplaint);
        long startTime = System.currentTimeMillis();
        String modelResult = deepSeekAssistant.chat(buildPrompt(chiefComplaint));
        long costMs = System.currentTimeMillis() - startTime;
        log.info("病例主诉质控模型调用耗时: {} ms, chiefComplaint={}", costMs, chiefComplaint);
        result.setRawModelResult(modelResult);

        MedicalRecordChiefComplaintQcVO modelVo = parseModelResult(modelResult);
        mergeModelResult(result, modelVo);
        finalizeResult(result);
        return result;
    }

    private String buildPrompt(String chiefComplaint) {
        return """
                你是电子病历内涵质控助手，请对“主诉”做结构化质控。
                质控重点：
                1. 是否包含主要症状。
                2. 是否包含持续时间。
                3. 如果缺项，请明确指出。

                识别要求：
                - 主要症状示例：发热、咳嗽、腹痛、胸闷、头晕。
                - 持续时间示例：3天、2周、1月、半年、今晨。
                - 只返回 JSON，不要输出 markdown，不要输出解释性文字。

                返回格式：
                {
                  "mainSymptom": "",
                  "duration": "",
                  "qualified": true,
                  "problems": [],
                  "suggestion": ""
                }

                主诉内容：
                %s
                """.formatted(chiefComplaint);
    }

    private MedicalRecordChiefComplaintQcVO buildRuleBasedResult(String chiefComplaint) {
        MedicalRecordChiefComplaintQcVO result = new MedicalRecordChiefComplaintQcVO();
        result.setChiefComplaint(chiefComplaint.trim());

        Matcher matcher = DURATION_PATTERN.matcher(chiefComplaint);
        if (matcher.find()) {
            String duration = matcher.group();
            result.setDuration(duration);
            String symptom = chiefComplaint.replace(duration, "")
                    .replace("，", "")
                    .replace(",", "")
                    .trim();
            if (StringUtils.hasText(symptom)) {
                result.setMainSymptom(symptom);
            }
        } else {
            result.getProblems().add("主诉缺少持续时间");
        }

        if (!StringUtils.hasText(result.getMainSymptom())) {
            result.getProblems().add("主诉缺少主要症状");
        }
        return result;
    }

    private MedicalRecordChiefComplaintQcVO parseModelResult(String modelResult) {
        if (!StringUtils.hasText(modelResult)) {
            return null;
        }
        Matcher matcher = JSON_PATTERN.matcher(modelResult);
        if (!matcher.find()) {
            return null;
        }
        String jsonText = matcher.group();
        try {
            JSONObject jsonObject = JSON.parseObject(jsonText);
            MedicalRecordChiefComplaintQcVO result = new MedicalRecordChiefComplaintQcVO();
            result.setMainSymptom(jsonObject.getString("mainSymptom"));
            result.setDuration(jsonObject.getString("duration"));
            result.setQualified(jsonObject.getBoolean("qualified"));
            result.setSuggestion(jsonObject.getString("suggestion"));
            if (jsonObject.containsKey("problems")) {
                result.setProblems(jsonObject.getList("problems", String.class));
            }
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    private void mergeModelResult(MedicalRecordChiefComplaintQcVO result, MedicalRecordChiefComplaintQcVO modelVo) {
        if (modelVo == null) {
            return;
        }
        if (!StringUtils.hasText(result.getMainSymptom()) && StringUtils.hasText(modelVo.getMainSymptom())) {
            result.setMainSymptom(modelVo.getMainSymptom());
        }
        if (!StringUtils.hasText(result.getDuration()) && StringUtils.hasText(modelVo.getDuration())) {
            result.setDuration(modelVo.getDuration());
        }
        if (!StringUtils.hasText(result.getSuggestion()) && StringUtils.hasText(modelVo.getSuggestion())) {
            result.setSuggestion(modelVo.getSuggestion());
        }

        Set<String> mergedProblems = new LinkedHashSet<>(result.getProblems());
        if (modelVo.getProblems() != null) {
            mergedProblems.addAll(modelVo.getProblems());
        }

        if (!StringUtils.hasText(result.getMainSymptom())) {
            mergedProblems.add("主诉缺少主要症状");
        }
        if (!StringUtils.hasText(result.getDuration())) {
            mergedProblems.add("主诉缺少持续时间");
        }

        result.setProblems(new java.util.ArrayList<>(mergedProblems));
    }

    private void finalizeResult(MedicalRecordChiefComplaintQcVO result) {
        boolean qualified = StringUtils.hasText(result.getMainSymptom())
                && StringUtils.hasText(result.getDuration())
                && (result.getProblems() == null || result.getProblems().isEmpty());
        result.setQualified(qualified);

        if (!StringUtils.hasText(result.getSuggestion())) {
            if (qualified) {
                result.setSuggestion("主诉基本合格，可继续结合现病史和诊断做进一步内涵质控");
            } else if (!StringUtils.hasText(result.getMainSymptom()) && !StringUtils.hasText(result.getDuration())) {
                result.setSuggestion("建议按“主要症状+持续时间”的格式补充主诉，例如“咳嗽3天”");
            } else if (!StringUtils.hasText(result.getMainSymptom())) {
                result.setSuggestion("建议补充明确的主要症状，例如发热、腹痛、胸闷等");
            } else {
                result.setSuggestion("建议补充明确持续时间，例如3天、2周、1月等");
            }
        }
    }
}
