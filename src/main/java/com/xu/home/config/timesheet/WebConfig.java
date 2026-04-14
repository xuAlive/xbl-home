package com.xu.home.config.timesheet;

import com.xu.home.config.common.BaseTokenWebConfig;
import com.xu.home.Interceptor.common.CommonTokenHandlerInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;

/**
 * Web MVC 配置
 */
@Configuration("timesheetWebConfig")
public class WebConfig extends BaseTokenWebConfig {

    public WebConfig(CommonTokenHandlerInterceptor tokenHandlerInterceptor) {
        super(tokenHandlerInterceptor);
    }

    @Override
    protected String[] getIncludePatterns() {
        return new String[]{"/timesheet/**"};
    }

    @Override
    protected List<String> getExcludePatterns() {
        return List.of("/timesheet/health");
    }
}
