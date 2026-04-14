package com.xu.home.config.common;

import com.xu.home.utils.common.JWTUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统一初始化 JWT 配置。
 */
@Configuration
@ConfigurationProperties(prefix = "blog.jwt")
public class CommonJwtConfig {

    private String secret;
    private Long expireMs;

    @PostConstruct
    public void init() {
        JWTUtil.configure(secret, expireMs);
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(Long expireMs) {
        this.expireMs = expireMs;
    }
}
