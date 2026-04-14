package com.xu.home.config.blog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信公众号配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    /**
     * 公众号AppID
     */
    private String appId;

    /**
     * 公众号AppSecret
     */
    private String appSecret;

    /**
     * 授权回调域名
     */
    private String redirectDomain;

    /**
     * 获取网页授权链接
     * @param redirectPath 授权后跳转的前端路径
     * @param scope 授权范围 snsapi_base(静默) 或 snsapi_userinfo(需用户确认)
     * @return 授权链接
     */
    public String getAuthUrl(String redirectPath, String scope) {
        String redirectUri = redirectDomain + "/blog/wechat/callback?target=" + redirectPath;
        return String.format(
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=STATE#wechat_redirect",
            appId,
            java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8),
            scope
        );
    }
}
