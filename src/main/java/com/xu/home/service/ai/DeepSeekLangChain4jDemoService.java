package com.xu.home.service.ai;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author xu
 * @date 2026/4/1 17:02
 * @desc langchain4j + deepseek demo
 */
@Service
public class DeepSeekLangChain4jDemoService {

    private final DeepSeekAssistant deepSeekAssistant;

    public DeepSeekLangChain4jDemoService(DeepSeekAssistant deepSeekAssistant) {
        this.deepSeekAssistant = deepSeekAssistant;
    }

    public String chat(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        return deepSeekAssistant.chat(message);
    }
}
