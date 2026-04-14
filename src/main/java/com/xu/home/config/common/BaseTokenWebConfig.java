package com.xu.home.config.common;

import com.xu.home.Interceptor.common.CommonTokenHandlerInterceptor;
import java.util.List;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 提供统一的 Token 拦截器注册逻辑，子项目只需要声明路径。
 */
public abstract class BaseTokenWebConfig implements WebMvcConfigurer {

    private final CommonTokenHandlerInterceptor tokenHandlerInterceptor;

    protected BaseTokenWebConfig(CommonTokenHandlerInterceptor tokenHandlerInterceptor) {
        this.tokenHandlerInterceptor = tokenHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenHandlerInterceptor)
                .addPathPatterns(getIncludePatterns())
                .excludePathPatterns(getExcludePatterns());
    }

    protected abstract String[] getIncludePatterns();

    protected List<String> getExcludePatterns() {
        return List.of();
    }
}
