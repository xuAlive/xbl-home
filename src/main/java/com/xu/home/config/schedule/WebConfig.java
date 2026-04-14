package com.xu.home.config.schedule;

import com.xu.home.config.common.BaseTokenWebConfig;
import com.xu.home.Interceptor.common.CommonTokenHandlerInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;

/**
 * Web 配置类
 * 配置拦截器
 */
@Configuration("scheduleWebConfig")
public class WebConfig extends BaseTokenWebConfig {

    public WebConfig(CommonTokenHandlerInterceptor tokenHandlerInterceptor) {
        super(tokenHandlerInterceptor);
    }

    @Override
    protected String[] getIncludePatterns() {
        return new String[]{"/schedule/**"};
    }

    @Override
    protected List<String> getExcludePatterns() {
        return List.of("/schedule/health");
    }
}
