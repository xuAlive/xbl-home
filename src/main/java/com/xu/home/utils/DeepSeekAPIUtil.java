package com.xu.home.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xu.home.config.blog.DeepSeekProperties;
import com.xu.home.param.blog.deepseek.BalanceInfo;
import com.xu.home.param.blog.deepseek.CompletionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class DeepSeekAPIUtil {

    private final DeepSeekProperties deepSeekProperties;

    public DeepSeekAPIUtil(DeepSeekProperties deepSeekProperties) {
        this.deepSeekProperties = deepSeekProperties;
    }

    public BalanceInfo balance() {
        try {
            var requestUrl = deepSeekProperties.resolveApiUrl() + "/user/balance";
            var headers = createHeaders();
            var balance = HttpUtil.sendGet(requestUrl, headers);
            return JSON.parseObject(balance, BalanceInfo.class);
        } catch (Exception e) {
            log.error("获取余额失败", e);
        }
        return null;
    }

    public List<CompletionResponse.Choice> completions(String message) {
        try {
            var requestUrl = deepSeekProperties.resolveApiUrl() + "/chat/completions";
            log.debug("DeepSeek API URL: {}", requestUrl);
            var headers = createHeaders();
            var completion = HttpUtil.sendPost(requestUrl, message, headers);
            var completionResponse = JSON.parseObject(completion, CompletionResponse.class);
            return completionResponse.getChoices();
        } catch (Exception e) {
            log.error("对话请求失败", e);
        }
        return null;
    }

    public void streamCompletions(String message, Consumer<String> deltaConsumer) {
        HttpURLConnection connection = null;
        try {
            String requestUrl = deepSeekProperties.resolveApiUrl() + "/chat/completions";
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(300000);

            Map<String, String> headers = createHeaders();
            headers.put("Accept", "text/event-stream");
            for (var entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            try (var os = connection.getOutputStream()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("DeepSeek流式请求失败: " + responseCode + " " + readStream(connection.getErrorStream()));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (!StringUtils.hasText(data)) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JSONObject payload = JSON.parseObject(data);
                    JSONArray choices = payload.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) {
                        continue;
                    }
                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    if (delta == null) {
                        continue;
                    }
                    String content = delta.getString("content");
                    if (StringUtils.hasText(content)) {
                        deltaConsumer.accept(content);
                    }
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek流式请求失败", e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String getChatModel() {
        return deepSeekProperties.resolveChatModel();
    }

    public String getReasonerModel() {
        return deepSeekProperties.resolveReasonerModel();
    }

    private Map<String, String> createHeaders() {
        String key = deepSeekProperties.getApi().getKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("未配置 DeepSeek API Key，请先设置环境变量 DEEPSEEK_API_KEY");
        }
        var headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + key);
        return headers;
    }

    private String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
