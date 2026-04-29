package com.xu.home.config.blog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    private final Api api = new Api();
    private final Model model = new Model();
    private final Langchain4j langchain4j = new Langchain4j();

    public Api getApi() {
        return api;
    }

    public Model getModel() {
        return model;
    }

    public Langchain4j getLangchain4j() {
        return langchain4j;
    }

    public String resolveApiUrl() {
        String url = api.getUrl();
        if (!StringUtils.hasText(url)) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String resolveModelName() {
        return StringUtils.hasText(model.getChat()) ? model.getChat() : "qwen-plus";
    }

    public String resolveLangChainBaseUrl() {
        if (StringUtils.hasText(langchain4j.getBaseUrl())) {
            String baseUrl = langchain4j.getBaseUrl();
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
        return resolveApiUrl();
    }

    public String resolveLangChainModelName() {
        return StringUtils.hasText(langchain4j.getModelName()) ? langchain4j.getModelName() : resolveModelName();
    }

    public static class Api {
        private String url = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String key;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public static class Model {
        private String chat = "qwen-plus";

        public String getChat() {
            return chat;
        }

        public void setChat(String chat) {
            this.chat = chat;
        }
    }

    public static class Langchain4j {
        private String baseUrl;
        private String modelName;
        private boolean logRequests;
        private boolean logResponses;
        private long timeoutSeconds = 180;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public boolean isLogRequests() {
            return logRequests;
        }

        public void setLogRequests(boolean logRequests) {
            this.logRequests = logRequests;
        }

        public boolean isLogResponses() {
            return logResponses;
        }

        public void setLogResponses(boolean logResponses) {
            this.logResponses = logResponses;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
