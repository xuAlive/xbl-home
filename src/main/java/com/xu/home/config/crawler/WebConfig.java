package com.xu.home.config.crawler;

import com.xu.home.Interceptor.common.CommonTokenHandlerInterceptor;
import com.xu.home.config.common.BaseTokenWebConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 书籍爬虫模块 Web 配置
 */
@Configuration("crawlerWebConfig")
public class WebConfig extends BaseTokenWebConfig {

    public WebConfig(CommonTokenHandlerInterceptor tokenHandlerInterceptor) {
        super(tokenHandlerInterceptor);
    }

    @Override
    protected String[] getIncludePatterns() {
        return new String[]{"/crawler/**"};
    }
}
