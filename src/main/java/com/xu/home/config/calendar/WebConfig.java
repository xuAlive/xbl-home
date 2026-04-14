package com.xu.home.config.calendar;

import com.xu.home.config.common.BaseTokenWebConfig;
import com.xu.home.Interceptor.common.CommonTokenHandlerInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;

/**
 * Web 配置类
 * 配置拦截器
 */
@Configuration("calendarWebConfig")
public class WebConfig extends BaseTokenWebConfig {

    public WebConfig(CommonTokenHandlerInterceptor tokenHandlerInterceptor) {
        super(tokenHandlerInterceptor);
    }

    @Override
    protected String[] getIncludePatterns() {
        return new String[]{"/calendar/**"};
    }

    @Override
    protected List<String> getExcludePatterns() {
        return List.of("/calendar/health");
    }
}
