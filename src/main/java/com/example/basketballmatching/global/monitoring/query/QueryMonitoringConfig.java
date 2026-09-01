package com.example.basketballmatching.global.monitoring.query;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.monitoring.query-count",
        name = "enabled",
        havingValue = "true"
)
public class QueryMonitoringConfig implements WebMvcConfigurer {

    private final QueryCountInterceptor queryCountInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {


        registry.addInterceptor(queryCountInterceptor)
                .addPathPatterns("/api/**");
    }
}
