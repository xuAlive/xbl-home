package com.xu.home.param.blog.deepseek;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;


@Data
public class CompletionResponse {
    private String id;
    private String object;
    private int created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @JSONField(name = "system_fingerprint")
    private String systemFingerprint;

    @Data
    public static class Choice {
        private int index;
        private Completion message;

        @JSONField(name = "finish_reason")
        private String finishReason;

        private String logprobs;
    }

    @Data
    public static class Usage {
        @JSONField(name = "prompt_tokens")
        private int promptTokens;

        @JSONField(name = "completion_tokens")
        private int completionTokens;

        @JSONField(name = "total_tokens")
        private int totalTokens;

        @JSONField(name = "prompt_cache_hit_tokens")
        private int promptCacheHitTokens;

        @JSONField(name = "prompt_cache_miss_tokens")
        private int promptCacheMissTokens;

        @JSONField(name = "prompt_tokens_details")
        private PromptTokensDetail promptTokensDetails;

        @Data
        public static class PromptTokensDetail {
            @JSONField(name = "cached_tokens")
            private int cachedTokens;
        }
    }
}
