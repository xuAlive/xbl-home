package com.xu.home.config.blog;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class QwenLangChain4jFactory {

    private final QwenProperties properties;

    public QwenLangChain4jFactory(QwenProperties properties) {
        this.properties = properties;
    }

    /**
     * 按需创建千问 ChatModel，避免在未配置密钥时阻塞应用启动。
     */
    public ChatModel createChatModel() {
        return createChatModel(properties.resolveLangChainModelName());
    }

    /**
     * 按指定模型创建千问 ChatModel，供页面选择不同模型时使用。
     */
    public ChatModel createChatModel(String modelName) {
        if (!StringUtils.hasText(properties.getApi().getKey())) {
            throw new IllegalArgumentException("未配置 QWEN_API_KEY，请先在 xbl-home/.env 或启动参数中设置");
        }
        return OpenAiChatModel.builder()
                .baseUrl(properties.resolveLangChainBaseUrl())
                .apiKey(properties.getApi().getKey())
                .modelName(StringUtils.hasText(modelName) ? modelName : properties.resolveLangChainModelName())
                .timeout(Duration.ofSeconds(Math.max(properties.getLangchain4j().getTimeoutSeconds(), 60)))
                .maxRetries(1)
                .logRequests(properties.getLangchain4j().isLogRequests())
                .logResponses(properties.getLangchain4j().isLogResponses())
                .build();
    }
}
