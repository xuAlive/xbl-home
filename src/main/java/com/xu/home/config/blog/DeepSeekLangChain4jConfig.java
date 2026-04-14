package com.xu.home.config.blog;

import com.xu.home.service.ai.DeepSeekAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DeepSeekLangChain4jConfig {

    @Bean
    public ChatModel deepSeekChatModel(DeepSeekProperties properties) {
        if (!StringUtils.hasText(properties.getApi().getKey())) {
            throw new IllegalArgumentException("未配置 DeepSeek API Key，请先设置环境变量 DEEPSEEK_API_KEY");
        }
        return OpenAiChatModel.builder()
                .baseUrl(properties.resolveLangChainBaseUrl())
                .apiKey(properties.getApi().getKey())
                .modelName(properties.resolveLangChainModelName())
                .logRequests(properties.getLangchain4j().isLogRequests())
                .logResponses(properties.getLangchain4j().isLogResponses())
                .build();
    }

    @Bean
    public DeepSeekAssistant deepSeekAssistant(ChatModel deepSeekChatModel) {
        return AiServices.builder(DeepSeekAssistant.class)
                .chatModel(deepSeekChatModel)
                .systemMessage("你是一个简洁、专业的中文 AI 助手，请优先用中文回答。")
                .build();
    }
}
