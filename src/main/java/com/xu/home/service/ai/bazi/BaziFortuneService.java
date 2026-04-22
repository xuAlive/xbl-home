package com.xu.home.service.ai.bazi;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xu.home.domain.blog.BaziMarriageRecord;
import com.xu.home.domain.blog.BaziFortuneRecord;
import com.xu.home.param.blog.po.bazi.BaziFortunePO;
import com.xu.home.param.blog.po.bazi.BaziMarriagePO;
import com.xu.home.param.blog.po.bazi.BaziMatchPersonPO;
import com.xu.home.param.blog.vo.bazi.BaziFortuneHistoryVO;
import com.xu.home.param.blog.vo.bazi.BaziFortuneRecordVO;
import com.xu.home.param.blog.vo.bazi.BaziMarriageHistoryVO;
import com.xu.home.param.blog.vo.bazi.BaziMarriagePersonVO;
import com.xu.home.param.blog.vo.bazi.BaziMarriageRecordVO;
import com.xu.home.service.blog.BaziMarriageRecordService;
import com.xu.home.service.blog.BaziFortuneRecordService;
import com.xu.home.utils.BaZiUtil;
import com.xu.home.utils.DeepSeekAPIUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class BaziFortuneService {

    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final BaziFortuneRecordService baziFortuneRecordService;
    private final BaziMarriageRecordService baziMarriageRecordService;
    private final DeepSeekAPIUtil deepSeekAPIUtil;

    public BaziFortuneService(
            BaziFortuneRecordService baziFortuneRecordService,
            BaziMarriageRecordService baziMarriageRecordService,
            DeepSeekAPIUtil deepSeekAPIUtil
    ) {
        this.baziFortuneRecordService = baziFortuneRecordService;
        this.baziMarriageRecordService = baziMarriageRecordService;
        this.deepSeekAPIUtil = deepSeekAPIUtil;
    }

    public SseEmitter streamFortune(BaziFortunePO po, String account) {
        ParsedInput input = parseInput(po);
        Map<String, Object> baziResult = calculateSingleBazi(input);
        String prompt = buildPrompt(input, baziResult);
        BaziFortuneRecord record = createProcessingRecord(account, input, baziResult, prompt);
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> doStream(record, baziResult, prompt, emitter), STREAM_EXECUTOR);
        return emitter;
    }

    public SseEmitter streamMarriage(BaziMarriagePO po, String account) {
        PersonInput personA = parseMatchPerson(po == null ? null : po.getPersonA(), "甲方");
        PersonInput personB = parseMatchPerson(po == null ? null : po.getPersonB(), "乙方");
        String question = po != null && StringUtils.hasText(po.getQuestion()) ? po.getQuestion().trim() : null;

        Map<String, Object> personABazi = calculateBazi(personA);
        Map<String, Object> personBBazi = calculateBazi(personB);
        String prompt = buildMarriagePrompt(personA, personABazi, personB, personBBazi, question);
        BaziMarriageRecord record = createMarriageProcessingRecord(account, personA, personABazi, personB, personBBazi, question, prompt);

        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> doMarriageStream(record, personA, personABazi, personB, personBBazi, prompt, emitter), STREAM_EXECUTOR);
        return emitter;
    }

    public List<BaziFortuneHistoryVO> getHistory(String account) {
        List<BaziFortuneRecord> records = baziFortuneRecordService.getRecentRecords(account, 20);
        List<BaziFortuneHistoryVO> result = new ArrayList<>();
        for (BaziFortuneRecord record : records) {
            result.add(toHistoryVO(record));
        }
        return result;
    }

    public BaziFortuneRecordVO getDetail(Long id, String account) {
        BaziFortuneRecord record = baziFortuneRecordService.getOwnedRecord(id, account);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        return toDetailVO(record);
    }

    public List<BaziMarriageHistoryVO> getMarriageHistory(String account) {
        List<BaziMarriageRecord> records = baziMarriageRecordService.getRecentRecords(account, 20);
        List<BaziMarriageHistoryVO> result = new ArrayList<>();
        for (BaziMarriageRecord record : records) {
            result.add(toMarriageHistoryVO(record));
        }
        return result;
    }

    public BaziMarriageRecordVO getMarriageDetail(Long id, String account) {
        BaziMarriageRecord record = baziMarriageRecordService.getOwnedRecord(id, account);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        return toMarriageDetailVO(record);
    }

    private void doStream(BaziFortuneRecord record, Map<String, Object> baziResult, String prompt, SseEmitter emitter) {
        StringBuilder answer = new StringBuilder();
        try {
            sendEvent(emitter, "meta", buildMeta(record.getId(), baziResult));

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", buildFortuneSystemPrompt());

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            JSONObject body = new JSONObject();
            body.put("model", deepSeekAPIUtil.getChatModel());
            body.put("stream", true);
            body.put("messages", List.of(systemMessage, userMessage));

            deepSeekAPIUtil.streamChatCompletions(body.toJSONString(), chunk -> {
                if (!StringUtils.hasText(chunk.getContent())) {
                    return;
                }
                answer.append(chunk.getContent());
                sendEvent(emitter, "delta", Map.of("content", chunk.getContent()));
            });

            record.setFortuneContent(answer.toString());
            record.setStatus("SUCCESS");
            record.setErrorMessage(null);
            baziFortuneRecordService.updateById(record);
            sendEvent(emitter, "done", Map.of("recordId", record.getId()));
            emitter.complete();
        } catch (Exception e) {
            log.error("八字运势流式输出失败, recordId={}", record.getId(), e);
            record.setFortuneContent(answer.toString());
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            baziFortuneRecordService.updateById(record);
            try {
                sendEvent(emitter, "error", Map.of("message", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "生成失败"));
            } catch (Exception ignored) {
                log.warn("发送错误事件失败, recordId={}", record.getId());
            }
            emitter.completeWithError(e);
        }
    }

    private void doMarriageStream(
            BaziMarriageRecord record,
            PersonInput personA,
            Map<String, Object> personABazi,
            PersonInput personB,
            Map<String, Object> personBBazi,
            String prompt,
            SseEmitter emitter
    ) {
        StringBuilder answer = new StringBuilder();
        try {
            sendEvent(emitter, "meta", buildMarriageMeta(record.getId(), personA, personABazi, personB, personBBazi));

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", buildMarriageSystemPrompt());

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            JSONObject body = new JSONObject();
            body.put("model", deepSeekAPIUtil.getChatModel());
            body.put("stream", true);
            body.put("messages", List.of(systemMessage, userMessage));

            deepSeekAPIUtil.streamChatCompletions(body.toJSONString(), chunk -> {
                if (!StringUtils.hasText(chunk.getContent())) {
                    return;
                }
                answer.append(chunk.getContent());
                sendEvent(emitter, "delta", Map.of("content", chunk.getContent()));
            });

            record.setFortuneContent(answer.toString());
            record.setStatus("SUCCESS");
            record.setErrorMessage(null);
            baziMarriageRecordService.updateById(record);
            sendEvent(emitter, "done", Map.of("recordId", record.getId(), "content", answer.toString()));
            emitter.complete();
        } catch (Exception e) {
            log.error("合八字姻缘流式输出失败", e);
            record.setFortuneContent(answer.toString());
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            baziMarriageRecordService.updateById(record);
            try {
                sendEvent(emitter, "error", Map.of("message", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "生成失败"));
            } catch (Exception ignored) {
                log.warn("发送合八字错误事件失败");
            }
            emitter.completeWithError(e);
        }
    }

    private ParsedInput parseInput(BaziFortunePO po) {
        if (po == null || !StringUtils.hasText(po.getBirthTime())) {
            throw new IllegalArgumentException("出生时间不能为空");
        }
        if (!StringUtils.hasText(po.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        LocalTime birthTime;
        try {
            birthTime = LocalTime.parse(po.getBirthTime(), TIME_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("出生时间格式错误");
        }

        Integer birthYear = po.getBirthYear();
        Integer birthMonth = po.getBirthMonth();
        Integer birthDay = po.getBirthDay();

        if (birthYear == null || birthMonth == null || birthDay == null) {
            throw new IllegalArgumentException("农历出生年月日不能为空");
        }
        if (birthYear < 1900 || birthYear > 2100) {
            throw new IllegalArgumentException("当前仅支持 1900-2100 年之间的日期");
        }
        String gender = po.getGender().trim();
        if (!"男".equals(gender) && !"女".equals(gender)) {
            throw new IllegalArgumentException("性别仅支持“男”或“女”");
        }
        String calendarType = resolveCalendarType(po.getCalendarType());
        validateBirthDate(calendarType, birthYear, birthMonth, birthDay, "出生日期");
        String question = StringUtils.hasText(po.getQuestion()) ? po.getQuestion().trim() : null;
        String name = StringUtils.hasText(po.getName()) ? po.getName().trim() : null;
        return new ParsedInput(
                name,
                birthYear,
                birthMonth,
                birthDay,
                Boolean.TRUE.equals(po.getLeapMonth()),
                calendarType,
                LocalDateTime.of(LocalDate.of(2000, 1, 1), birthTime),
                gender,
                question
        );
    }

    private PersonInput parseMatchPerson(BaziMatchPersonPO po, String fallbackName) {
        if (po == null) {
            throw new IllegalArgumentException(fallbackName + "信息不能为空");
        }
        if (!StringUtils.hasText(po.getBirthTime())) {
            throw new IllegalArgumentException(fallbackName + "出生时间不能为空");
        }
        if (!StringUtils.hasText(po.getGender())) {
            throw new IllegalArgumentException(fallbackName + "性别不能为空");
        }

        LocalTime birthTime;
        try {
            birthTime = LocalTime.parse(po.getBirthTime(), TIME_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException(fallbackName + "出生时间格式错误");
        }

        Integer birthYear = po.getBirthYear();
        Integer birthMonth = po.getBirthMonth();
        Integer birthDay = po.getBirthDay();
        if (birthYear == null || birthMonth == null || birthDay == null) {
            throw new IllegalArgumentException(fallbackName + "农历出生年月日不能为空");
        }
        if (birthYear < 1900 || birthYear > 2100) {
            throw new IllegalArgumentException(fallbackName + "当前仅支持 1900-2100 年之间的日期");
        }

        String gender = po.getGender().trim();
        if (!"男".equals(gender) && !"女".equals(gender)) {
            throw new IllegalArgumentException(fallbackName + "性别仅支持“男”或“女”");
        }

        String name = StringUtils.hasText(po.getName()) ? po.getName().trim() : fallbackName;
        String calendarType = resolveCalendarType(po.getCalendarType());
        validateBirthDate(calendarType, birthYear, birthMonth, birthDay, fallbackName + "出生日期");
        return new PersonInput(
                name,
                birthYear,
                birthMonth,
                birthDay,
                Boolean.TRUE.equals(po.getLeapMonth()),
                calendarType,
                LocalDateTime.of(LocalDate.of(2000, 1, 1), birthTime),
                gender
        );
    }

    private Map<String, Object> calculateBazi(PersonInput input) {
        if ("solar".equals(input.calendarType())) {
            return BaZiUtil.calculate(
                    input.birthYear(),
                    input.birthMonth(),
                    input.birthDay(),
                    input.birthDateTime().getHour(),
                    input.birthDateTime().getMinute()
            );
        }
        return BaZiUtil.calculateFromLunar(
                input.birthYear(),
                input.birthMonth(),
                input.birthDay(),
                input.leapMonth(),
                input.birthDateTime().getHour(),
                input.birthDateTime().getMinute()
        );
    }

    private BaziFortuneRecord createProcessingRecord(String account, ParsedInput input, Map<String, Object> baziResult, String prompt) {
        BaziFortuneRecord record = new BaziFortuneRecord();
        record.setAccount(account);
        record.setGender(input.gender());
        LocalDate solarDate = LocalDate.parse((String) baziResult.get("solarDate"), DATE_FORMATTER);
        LocalTime time = input.birthDateTime().toLocalTime();
        LocalDateTime solarDateTime = LocalDateTime.of(solarDate, time);
        record.setBirthDatetime(Date.from(solarDateTime.atZone(ZONE_ID).toInstant()));
        record.setInputBirthDate(formatInputBirthDate(input.calendarType(), input.birthYear(), input.birthMonth(), input.birthDay()));
        record.setInputBirthTime(time.format(TIME_FORMATTER));
        record.setIsLeapMonth("lunar".equals(input.calendarType()) && input.leapMonth() ? 1 : 0);
        record.setQuestion(input.question());
        record.setYearPillar((String) baziResult.get("yearPillar"));
        record.setMonthPillar((String) baziResult.get("monthPillar"));
        record.setDayPillar((String) baziResult.get("dayPillar"));
        record.setHourPillar((String) baziResult.get("hourPillar"));
        record.setBaZi((String) baziResult.get("baZi"));
        record.setZodiac((String) baziResult.get("zodiac"));
        record.setShiChen((String) baziResult.get("shiChen"));
        record.setLunarText((String) baziResult.get("lunarText"));
        record.setPromptText(prompt);
        record.setModelName(deepSeekAPIUtil.getChatModel());
        record.setStatus("PROCESSING");
        baziFortuneRecordService.save(record);
        return record;
    }

    private BaziMarriageRecord createMarriageProcessingRecord(
            String account,
            PersonInput personA,
            Map<String, Object> personABazi,
            PersonInput personB,
            Map<String, Object> personBBazi,
            String question,
            String prompt
    ) {
        BaziMarriageRecord record = new BaziMarriageRecord();
        record.setAccount(account);
        fillMarriagePersonRecord(record, true, personA, personABazi);
        fillMarriagePersonRecord(record, false, personB, personBBazi);
        record.setQuestion(question);
        record.setPromptText(prompt);
        record.setModelName(deepSeekAPIUtil.getChatModel());
        record.setStatus("PROCESSING");
        baziMarriageRecordService.save(record);
        return record;
    }

    private String buildPrompt(ParsedInput input, Map<String, Object> baziResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下生辰八字信息做命理分析\n");
        if (StringUtils.hasText(input.name())) {
            prompt.append("姓名：").append(input.name()).append("\n");
        }
        prompt.append("性别：").append(input.gender()).append("\n");
        prompt.append("输入日期类型：").append(calendarTypeLabel(input.calendarType())).append("\n");
        prompt.append(calendarTypeLabel(input.calendarType())).append("出生时间：")
                .append(formatRawBirthDate(input.birthYear(), input.birthMonth(), input.birthDay()))
                .append("lunar".equals(input.calendarType()) && input.leapMonth() ? "（闰月）" : "")
                .append(" ")
                .append(input.birthDateTime().toLocalTime().format(TIME_FORMATTER))
                .append("\n");
        prompt.append("换算公历时间：").append(valueOrFallback((String) baziResult.get("solarDate"), "未知"))
                .append(" ")
                .append(input.birthDateTime().toLocalTime().format(TIME_FORMATTER))
                .append("\n");
        prompt.append("农历信息：").append(valueOrFallback((String) baziResult.get("inputLunarText"), "未知")).append("\n");
        prompt.append("生肖：").append(valueOrFallback((String) baziResult.get("zodiac"), "未知")).append("\n");
        prompt.append("八字：").append(valueOrFallback((String) baziResult.get("baZi"), "未知")).append("\n");
        prompt.append("年柱：").append(valueOrFallback((String) baziResult.get("yearPillar"), "未知")).append("\n");
        prompt.append("月柱：").append(valueOrFallback((String) baziResult.get("monthPillar"), "未知")).append("\n");
        prompt.append("日柱：").append(valueOrFallback((String) baziResult.get("dayPillar"), "未知")).append("\n");
        prompt.append("时柱：").append(valueOrFallback((String) baziResult.get("hourPillar"), "未知")).append("\n");
        prompt.append("时辰：").append(valueOrFallback((String) baziResult.get("shiChen"), "未知")).append("\n");
        if (StringUtils.hasText((String) baziResult.get("solarTerm"))) {
            prompt.append("节气：").append(baziResult.get("solarTerm")).append("\n");
        }
        if (StringUtils.hasText(input.question())) {
            prompt.append("用户特别关注：").append(input.question()).append("\n");
        } else {
            prompt.append("用户特别关注：希望得到整体运势与方向建议\n");
        }
        return prompt.toString();
    }

    private String buildMarriagePrompt(
            PersonInput personA,
            Map<String, Object> personABazi,
            PersonInput personB,
            Map<String, Object> personBBazi,
            String question
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下双方生辰八字信息做合八字姻缘分析\n");
        appendMarriagePerson(prompt, "甲方", personA, personABazi);
        appendMarriagePerson(prompt, "乙方", personB, personBBazi);
        if (StringUtils.hasText(question)) {
            prompt.append("双方特别关注：").append(question).append("\n");
        } else {
            prompt.append("双方特别关注：两人的缘分契合度、相处模式、感情风险与婚恋建议\n");
        }
        prompt.append("请重点结合双方五行互补、日主关系、情感互动与长期相处稳定性进行分析。\n");
        return prompt.toString();
    }

    private Map<String, Object> buildMeta(Long recordId, Map<String, Object> baziResult) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("recordId", recordId);
        meta.put("calendarType", baziResult.get("calendarType"));
        meta.put("yearPillar", baziResult.get("yearPillar"));
        meta.put("monthPillar", baziResult.get("monthPillar"));
        meta.put("dayPillar", baziResult.get("dayPillar"));
        meta.put("hourPillar", baziResult.get("hourPillar"));
        meta.put("baZi", baziResult.get("baZi"));
        meta.put("zodiac", baziResult.get("zodiac"));
        meta.put("shiChen", baziResult.get("shiChen"));
        meta.put("lunarText", baziResult.get("inputLunarText"));
        meta.put("solarTerm", baziResult.get("solarTerm"));
        meta.put("solarDate", baziResult.get("solarDate"));
        return meta;
    }

    private Map<String, Object> buildMarriageMeta(
            Long recordId,
            PersonInput personA,
            Map<String, Object> personABazi,
            PersonInput personB,
            Map<String, Object> personBBazi
    ) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("recordId", recordId);
        meta.put("personA", buildMarriagePersonMeta(personA, personABazi));
        meta.put("personB", buildMarriagePersonMeta(personB, personBBazi));
        return meta;
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(JSON.toJSONString(data)));
        } catch (IOException e) {
            throw new RuntimeException("推送流式事件失败", e);
        }
    }

    private BaziFortuneHistoryVO toHistoryVO(BaziFortuneRecord record) {
        BaziFortuneHistoryVO vo = new BaziFortuneHistoryVO();
        vo.setId(record.getId());
        vo.setGender(record.getGender());
        vo.setBirthDate(record.getInputBirthDate());
        vo.setBirthTime(record.getInputBirthTime());
        vo.setLeapMonth(record.getIsLeapMonth() != null && record.getIsLeapMonth() == 1);
        vo.setBaZi(record.getBaZi());
        vo.setZodiac(record.getZodiac());
        vo.setQuestion(record.getQuestion());
        vo.setStatus(record.getStatus());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private BaziFortuneRecordVO toDetailVO(BaziFortuneRecord record) {
        BaziFortuneRecordVO vo = new BaziFortuneRecordVO();
        vo.setId(record.getId());
        vo.setGender(record.getGender());
        vo.setBirthDate(record.getInputBirthDate());
        vo.setBirthTime(record.getInputBirthTime());
        vo.setLeapMonth(record.getIsLeapMonth() != null && record.getIsLeapMonth() == 1);
        vo.setSolarDate(formatDate(record.getBirthDatetime()));
        vo.setSolarTime(formatTime(record.getBirthDatetime()));
        vo.setQuestion(record.getQuestion());
        vo.setYearPillar(record.getYearPillar());
        vo.setMonthPillar(record.getMonthPillar());
        vo.setDayPillar(record.getDayPillar());
        vo.setHourPillar(record.getHourPillar());
        vo.setBaZi(record.getBaZi());
        vo.setZodiac(record.getZodiac());
        vo.setShiChen(record.getShiChen());
        vo.setLunarText(record.getLunarText());
        vo.setFortuneContent(record.getFortuneContent());
        vo.setStatus(record.getStatus());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private BaziMarriageHistoryVO toMarriageHistoryVO(BaziMarriageRecord record) {
        BaziMarriageHistoryVO vo = new BaziMarriageHistoryVO();
        vo.setId(record.getId());
        vo.setPersonAName(record.getPersonAName());
        vo.setPersonBName(record.getPersonBName());
        vo.setPersonABaZi(record.getPersonABaZi());
        vo.setPersonBBaZi(record.getPersonBBaZi());
        vo.setQuestion(record.getQuestion());
        vo.setStatus(record.getStatus());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private BaziMarriageRecordVO toMarriageDetailVO(BaziMarriageRecord record) {
        BaziMarriageRecordVO vo = new BaziMarriageRecordVO();
        vo.setId(record.getId());
        vo.setPersonA(toMarriagePersonVO(
                record.getPersonAName(),
                record.getPersonAGender(),
                record.getPersonACalendarType(),
                record.getPersonAInputBirthDate(),
                record.getPersonAInputBirthTime(),
                record.getPersonAIsLeapMonth(),
                record.getPersonABirthDatetime(),
                record.getPersonAYearPillar(),
                record.getPersonAMonthPillar(),
                record.getPersonADayPillar(),
                record.getPersonAHourPillar(),
                record.getPersonABaZi(),
                record.getPersonAZodiac(),
                record.getPersonAShiChen(),
                record.getPersonALunarText()
        ));
        vo.setPersonB(toMarriagePersonVO(
                record.getPersonBName(),
                record.getPersonBGender(),
                record.getPersonBCalendarType(),
                record.getPersonBInputBirthDate(),
                record.getPersonBInputBirthTime(),
                record.getPersonBIsLeapMonth(),
                record.getPersonBBirthDatetime(),
                record.getPersonBYearPillar(),
                record.getPersonBMonthPillar(),
                record.getPersonBDayPillar(),
                record.getPersonBHourPillar(),
                record.getPersonBBaZi(),
                record.getPersonBZodiac(),
                record.getPersonBShiChen(),
                record.getPersonBLunarText()
        ));
        vo.setQuestion(record.getQuestion());
        vo.setFortuneContent(record.getFortuneContent());
        vo.setStatus(record.getStatus());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private BaziMarriagePersonVO toMarriagePersonVO(
            String name,
            String gender,
            String calendarType,
            String birthDate,
            String birthTime,
            Integer leapMonth,
            Date birthDatetime,
            String yearPillar,
            String monthPillar,
            String dayPillar,
            String hourPillar,
            String baZi,
            String zodiac,
            String shiChen,
            String lunarText
    ) {
        BaziMarriagePersonVO vo = new BaziMarriagePersonVO();
        vo.setName(name);
        vo.setGender(gender);
        vo.setCalendarType(calendarType);
        vo.setBirthDate(stripCalendarPrefix(birthDate));
        vo.setBirthTime(birthTime);
        vo.setLeapMonth(leapMonth != null && leapMonth == 1);
        vo.setSolarDate(formatDate(birthDatetime));
        vo.setSolarTime(formatTime(birthDatetime));
        vo.setYearPillar(yearPillar);
        vo.setMonthPillar(monthPillar);
        vo.setDayPillar(dayPillar);
        vo.setHourPillar(hourPillar);
        vo.setBaZi(baZi);
        vo.setZodiac(zodiac);
        vo.setShiChen(shiChen);
        vo.setLunarText(lunarText);
        return vo;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant().atZone(ZONE_ID).toLocalDate().format(DATE_FORMATTER);
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant().atZone(ZONE_ID).toLocalTime().format(TIME_FORMATTER);
    }

    private String stripCalendarPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int index = value.indexOf(':');
        return index >= 0 ? value.substring(index + 1) : value;
    }

    private String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Map<String, Object> calculateSingleBazi(ParsedInput input) {
        if ("solar".equals(input.calendarType())) {
            return BaZiUtil.calculate(
                    input.birthYear(),
                    input.birthMonth(),
                    input.birthDay(),
                    input.birthDateTime().getHour(),
                    input.birthDateTime().getMinute()
            );
        }
        return BaZiUtil.calculateFromLunar(
                input.birthYear(),
                input.birthMonth(),
                input.birthDay(),
                input.leapMonth(),
                input.birthDateTime().getHour(),
                input.birthDateTime().getMinute()
        );
    }

    private String resolveCalendarType(String calendarType) {
        return "solar".equalsIgnoreCase(calendarType) ? "solar" : "lunar";
    }

    private String calendarTypeLabel(String calendarType) {
        return "solar".equals(calendarType) ? "阳历" : "农历";
    }

    private void validateBirthDate(String calendarType, int year, int month, int day, String fieldLabel) {
        if (month < 1 || month > 12 || day < 1) {
            throw new IllegalArgumentException(fieldLabel + "不合法");
        }
        if ("solar".equals(calendarType)) {
            try {
                LocalDate.of(year, month, day);
            } catch (Exception e) {
                throw new IllegalArgumentException(fieldLabel + "不合法");
            }
            return;
        }
        if (day > 30) {
            throw new IllegalArgumentException(fieldLabel + "不合法");
        }
    }

    private String formatRawBirthDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private String formatLunarDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private String formatInputBirthDate(String calendarType, int year, int month, int day) {
        return calendarTypeLabel(calendarType) + ":" + formatRawBirthDate(year, month, day);
    }

    private void appendMarriagePerson(StringBuilder prompt, String roleName, PersonInput input, Map<String, Object> baziResult) {
        prompt.append(roleName).append("姓名：").append(input.name()).append("\n");
        prompt.append(roleName).append("性别：").append(input.gender()).append("\n");
        prompt.append(roleName).append("输入日期类型：").append(calendarTypeLabel(input.calendarType())).append("\n");
        prompt.append(roleName).append(calendarTypeLabel(input.calendarType())).append("出生时间：")
                .append(formatRawBirthDate(input.birthYear(), input.birthMonth(), input.birthDay()))
                .append("lunar".equals(input.calendarType()) && input.leapMonth() ? "（闰月）" : "")
                .append(" ")
                .append(input.birthDateTime().toLocalTime().format(TIME_FORMATTER))
                .append("\n");
        prompt.append(roleName).append("换算公历时间：")
                .append(valueOrFallback((String) baziResult.get("solarDate"), "未知"))
                .append(" ")
                .append(input.birthDateTime().toLocalTime().format(TIME_FORMATTER))
                .append("\n");
        prompt.append(roleName).append("农历信息：").append(valueOrFallback((String) baziResult.get("inputLunarText"), "未知")).append("\n");
        prompt.append(roleName).append("生肖：").append(valueOrFallback((String) baziResult.get("zodiac"), "未知")).append("\n");
        prompt.append(roleName).append("八字：").append(valueOrFallback((String) baziResult.get("baZi"), "未知")).append("\n");
        prompt.append(roleName).append("年柱：").append(valueOrFallback((String) baziResult.get("yearPillar"), "未知")).append("\n");
        prompt.append(roleName).append("月柱：").append(valueOrFallback((String) baziResult.get("monthPillar"), "未知")).append("\n");
        prompt.append(roleName).append("日柱：").append(valueOrFallback((String) baziResult.get("dayPillar"), "未知")).append("\n");
        prompt.append(roleName).append("时柱：").append(valueOrFallback((String) baziResult.get("hourPillar"), "未知")).append("\n");
        prompt.append(roleName).append("时辰：").append(valueOrFallback((String) baziResult.get("shiChen"), "未知")).append("\n");
        if (StringUtils.hasText((String) baziResult.get("solarTerm"))) {
            prompt.append(roleName).append("节气：").append(baziResult.get("solarTerm")).append("\n");
        }
    }

    private Map<String, Object> buildMarriagePersonMeta(PersonInput input, Map<String, Object> baziResult) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", input.name());
        meta.put("gender", input.gender());
        meta.put("calendarType", input.calendarType());
        meta.put("birthDate", formatRawBirthDate(input.birthYear(), input.birthMonth(), input.birthDay()));
        meta.put("birthTime", input.birthDateTime().toLocalTime().format(TIME_FORMATTER));
        meta.put("leapMonth", input.leapMonth());
        meta.put("solarDate", baziResult.get("solarDate"));
        meta.put("lunarText", baziResult.get("inputLunarText"));
        meta.put("zodiac", baziResult.get("zodiac"));
        meta.put("baZi", baziResult.get("baZi"));
        meta.put("yearPillar", baziResult.get("yearPillar"));
        meta.put("monthPillar", baziResult.get("monthPillar"));
        meta.put("dayPillar", baziResult.get("dayPillar"));
        meta.put("hourPillar", baziResult.get("hourPillar"));
        meta.put("shiChen", baziResult.get("shiChen"));
        meta.put("solarTerm", baziResult.get("solarTerm"));
        return meta;
    }

    private void fillMarriagePersonRecord(BaziMarriageRecord record, boolean first, PersonInput input, Map<String, Object> baziResult) {
        LocalDate solarDate = LocalDate.parse((String) baziResult.get("solarDate"), DATE_FORMATTER);
        LocalTime time = input.birthDateTime().toLocalTime();
        LocalDateTime solarDateTime = LocalDateTime.of(solarDate, time);
        Date birthDatetime = Date.from(solarDateTime.atZone(ZONE_ID).toInstant());
        String inputBirthDate = formatInputBirthDate(input.calendarType(), input.birthYear(), input.birthMonth(), input.birthDay());
        Integer leapMonth = "lunar".equals(input.calendarType()) && input.leapMonth() ? 1 : 0;

        if (first) {
            record.setPersonAName(input.name());
            record.setPersonAGender(input.gender());
            record.setPersonACalendarType(input.calendarType());
            record.setPersonABirthDatetime(birthDatetime);
            record.setPersonAInputBirthDate(inputBirthDate);
            record.setPersonAInputBirthTime(time.format(TIME_FORMATTER));
            record.setPersonAIsLeapMonth(leapMonth);
            record.setPersonAYearPillar((String) baziResult.get("yearPillar"));
            record.setPersonAMonthPillar((String) baziResult.get("monthPillar"));
            record.setPersonADayPillar((String) baziResult.get("dayPillar"));
            record.setPersonAHourPillar((String) baziResult.get("hourPillar"));
            record.setPersonABaZi((String) baziResult.get("baZi"));
            record.setPersonAZodiac((String) baziResult.get("zodiac"));
            record.setPersonAShiChen((String) baziResult.get("shiChen"));
            record.setPersonALunarText((String) baziResult.get("inputLunarText"));
            return;
        }

        record.setPersonBName(input.name());
        record.setPersonBGender(input.gender());
        record.setPersonBCalendarType(input.calendarType());
        record.setPersonBBirthDatetime(birthDatetime);
        record.setPersonBInputBirthDate(inputBirthDate);
        record.setPersonBInputBirthTime(time.format(TIME_FORMATTER));
        record.setPersonBIsLeapMonth(leapMonth);
        record.setPersonBYearPillar((String) baziResult.get("yearPillar"));
        record.setPersonBMonthPillar((String) baziResult.get("monthPillar"));
        record.setPersonBDayPillar((String) baziResult.get("dayPillar"));
        record.setPersonBHourPillar((String) baziResult.get("hourPillar"));
        record.setPersonBBaZi((String) baziResult.get("baZi"));
        record.setPersonBZodiac((String) baziResult.get("zodiac"));
        record.setPersonBShiChen((String) baziResult.get("shiChen"));
        record.setPersonBLunarText((String) baziResult.get("inputLunarText"));
    }

    private String buildFortuneSystemPrompt() {
        return """
                你是一位中文命理分析助手。请基于用户提供的八字信息，输出温和、克制、可读性强的分析。
                要求：
                1. 先概括命盘特点，再从性格、事业、财运、感情、健康、近期建议六个方面分析。
                2. 使用中文，避免绝对化、宿命论措辞，可以使用“倾向于”“更适合”“建议”。
                3. 不要声称自己具备超自然能力，不要输出违法、医疗诊断或投资保证。
                4. 整体必须使用 Markdown 输出。
                5. 顶部给出 `## 总览`，后续每一部分都用 `### 性格分析` 这种格式的小节标题，`#` 后面必须有空格。
                6. 每个小节标题和正文之间必须空一行。
                7. 如果使用列表，必须写成 `- 内容` 或 `1. 内容`，符号后面必须有空格。
                8. 不要把标题和正文写在同一行，不要输出形如 `##总览`、`###性格分析*` 这样的内容。
                9. 结尾补一段 `### 行动建议`，给出 3-5 条简短建议。
                10. 必须严格按下面这个标题顺序输出，标题文字不要改动：
                `## 总览`
                `### 性格分析`
                `### 事业分析`
                `### 财运分析`
                `### 感情分析`
                `### 健康分析`
                `### 行动建议`
                """;
    }

    private String buildMarriageSystemPrompt() {
        return """
                你是一位中文命理合盘分析助手。请基于双方提供的八字信息，输出温和、克制、可读性强的姻缘分析。
                要求：
                1. 重点分析双方缘分契合度、相处模式、情感互补、潜在摩擦点与婚恋建议。
                2. 使用中文，避免绝对化、宿命论措辞，可以使用“更容易”“倾向于”“建议”。
                3. 不要声称自己具备超自然能力，不要输出违法、医疗诊断或投资保证。
                4. 整体必须使用 Markdown 输出。
                5. 标题必须严格按下面顺序输出，且 `#` 后必须有空格：
                `## 合盘总览`
                `### 缘分契合`
                `### 性格互动`
                `### 感情风险`
                `### 婚恋建议`
                6. 每个小节标题和正文之间必须空一行。
                7. 如果使用列表，必须写成 `- 内容` 或 `1. 内容`，符号后面必须有空格。
                8. 不要把标题和正文写在同一行，不要输出形如 `##合盘总览`、`###缘分契合*` 这样的内容。
                9. 分析时要兼顾双方各自八字与彼此互动关系，不要只分析单方。
                """;
    }

    private record ParsedInput(
            String name,
            int birthYear,
            int birthMonth,
            int birthDay,
            boolean leapMonth,
            String calendarType,
            LocalDateTime birthDateTime,
            String gender,
            String question
    ) {
    }

    private record PersonInput(
            String name,
            int birthYear,
            int birthMonth,
            int birthDay,
            boolean leapMonth,
            String calendarType,
            LocalDateTime birthDateTime,
            String gender
    ) {
    }
}
