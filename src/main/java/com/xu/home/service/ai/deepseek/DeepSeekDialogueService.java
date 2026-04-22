package com.xu.home.service.ai.deepseek;

import com.alibaba.fastjson2.JSONObject;
import com.xu.home.param.blog.po.deepseek.DialogueInfoPO;
import com.xu.home.param.blog.vo.ds.CompletionVO;
import com.xu.home.service.blog.DeepseekDialogueInfoService;
import com.xu.home.utils.DeepSeekAPIUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DeepSeekDialogueService {

    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();
    private static final long STREAM_CHAR_DELAY_MS = 12L;

    private final DeepseekDialogueInfoService deepseekDialogueInfoService;
    private final DeepSeekAPIUtil deepSeekAPIUtil;

    public DeepSeekDialogueService(DeepseekDialogueInfoService deepseekDialogueInfoService, DeepSeekAPIUtil deepSeekAPIUtil) {
        this.deepseekDialogueInfoService = deepseekDialogueInfoService;
        this.deepSeekAPIUtil = deepSeekAPIUtil;
    }

    public SseEmitter streamCompletion(DialogueInfoPO po) {
        validateRequest(po);
        if (Objects.isNull(po.getDialogueId())) {
            po.setDialogueId(System.currentTimeMillis());
        }
        po.setRole("user");
        deepseekDialogueInfoService.saveDialogueInfo(po);
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> doStreamCompletion(po, emitter), STREAM_EXECUTOR);
        return emitter;
    }

    private void doStreamCompletion(DialogueInfoPO po, SseEmitter emitter) {
        StringBuilder answerBuilder = new StringBuilder();
        try {
            sendEvent(emitter, "meta", buildMeta(po));
            JSONObject body = buildMessageBody(po);
            log.warn("DeepSeek流式请求体: {}", body.toJSONString());

            deepSeekAPIUtil.streamChatCompletions(body.toJSONString(), chunk -> {
                if (StringUtils.isNotBlank(chunk.getContent())) {
                    answerBuilder.append(chunk.getContent());
                    sendDeltaCharacters(emitter, po.getDialogueId(), chunk.getContent());
                }
            });

            if (StringUtils.isBlank(answerBuilder.toString())) {
                throw new RuntimeException("DeepSeek未返回有效内容");
            }

            DialogueInfoPO resultPo = new DialogueInfoPO();
            resultPo.setDialogueId(po.getDialogueId());
            resultPo.setRole("assistant");
            resultPo.setContent(answerBuilder.toString());
            resultPo.setAccount(po.getAccount());
            deepseekDialogueInfoService.saveDialogueInfo(resultPo);

            sendEvent(emitter, "done", buildDonePayload(po.getDialogueId(), answerBuilder.toString()));
            emitter.complete();
        } catch (Exception e) {
            log.error("DeepSeek流式对话失败, dialogueId={}", po.getDialogueId(), e);
            try {
                sendEvent(emitter, "error", Map.of(
                        "dialogueId", po.getDialogueId(),
                        "message", StringUtils.defaultIfBlank(e.getMessage(), "发送消息失败")
                ));
            } catch (Exception ignored) {
                log.warn("DeepSeek错误事件发送失败, dialogueId={}", po.getDialogueId());
            }
            emitter.completeWithError(e);
        }
    }

    private JSONObject buildMessageBody(DialogueInfoPO po) {
        List<CompletionVO> dialogueInfo = deepseekDialogueInfoService.getDialogueInfo(po.getDialogueId(), po.getAccount());
        if (CollectionUtils.isEmpty(dialogueInfo)) {
            dialogueInfo = new ArrayList<>();
        }
        List<Map<String, String>> mapList = new ArrayList<>();
        for (CompletionVO completionVO : dialogueInfo) {
            Map<String, String> map = new HashMap<>();
            map.put("role", completionVO.getRole());
            map.put("content", completionVO.getContent());
            mapList.add(map);
        }
        JSONObject body = new JSONObject();
        body.put("model", deepSeekAPIUtil.getChatModel());
        body.put("stream", true);
        body.put("messages", mapList);
        return body;
    }

    private Map<String, Object> buildMeta(DialogueInfoPO po) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("dialogueId", po.getDialogueId());
        meta.put("model", deepSeekAPIUtil.getChatModel());
        meta.put("role", "assistant");
        meta.put("startedAt", new Date());
        return meta;
    }

    private Map<String, Object> buildDeltaPayload(Long dialogueId, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dialogueId", dialogueId);
        payload.put("role", "assistant");
        payload.put("content", content);
        return payload;
    }

    private void sendDeltaCharacters(SseEmitter emitter, Long dialogueId, String content) {
        for (int i = 0; i < content.length(); i++) {
            sendEvent(emitter, "delta", buildDeltaPayload(dialogueId, String.valueOf(content.charAt(i))));
            sleepQuietly();
        }
    }

    private void sleepQuietly() {
        try {
            TimeUnit.MILLISECONDS.sleep(STREAM_CHAR_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DeepSeek流式推送被中断", e);
        }
    }

    private Map<String, Object> buildDonePayload(Long dialogueId, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dialogueId", dialogueId);
        payload.put("role", "assistant");
        payload.put("model", deepSeekAPIUtil.getChatModel());
        payload.put("content", content);
        payload.put("completedAt", new Date());
        return payload;
    }

    private void validateRequest(DialogueInfoPO po) {
        if (Objects.isNull(po) || StringUtils.isBlank(po.getContent())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (StringUtils.isBlank(po.getAccount())) {
            throw new IllegalArgumentException("用户信息缺失");
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(JSONObject.toJSONString(payload)));
        } catch (IOException e) {
            throw new RuntimeException("推送DeepSeek流事件失败", e);
        }
    }
}
