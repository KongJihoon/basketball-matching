package com.example.basketballmatching.global.monitoring.query;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.monitoring.query-count",
        name = "enabled",
        havingValue = "true"
)
public class QueryCountInterceptor implements AsyncHandlerInterceptor {


    private static final String UNKNOWN_PATH = "UNKNOWN_PATH";

    private static final String METRIC_NAME =
            "app.query.per_request";

    private final MeterRegistry meterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String path = resolvePath(request);

        QueryRequestContext context = new QueryRequestContext(request.getMethod(), path);

        QueryRequestContextHolder.initialize(context);

        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        recordAndClear();
    }

    @Override
    public void afterConcurrentHandlingStarted(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        recordAndClear();
    }


    private String resolvePath(HttpServletRequest request) {

        Object pathAttribute = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        if (pathAttribute instanceof String path) {
            return path;
        }

        return UNKNOWN_PATH;

    }

    private void recordAndClear() {
        QueryRequestContext context = QueryRequestContextHolder.getContext();

        try {
            if (context == null) {
                return;
            }

            context.getQueryTypeCountByType()
                    .forEach((queryType, count) ->
                            record(context, queryType, count));
        } finally {
            QueryRequestContextHolder.clear();
        }

    }

    private void record(QueryRequestContext context, QueryType queryType, Integer count) {

        DistributionSummary summary =
                DistributionSummary.builder(METRIC_NAME)
                        .description(
                                "Number of SQL queries per HTTP request"
                        )
                        .baseUnit("queries")
                        .tag(
                                "path",
                                context.getPath()
                        )
                        .tag(
                                "http_method",
                                context.getHttpMethod()
                        )
                        .tag(
                                "query_type",
                                queryType.name()
                        )
                        .publishPercentiles(
                                0.5,
                                0.95
                        )
                        .publishPercentileHistogram()
                        .minimumExpectedValue(1.0)
                        .maximumExpectedValue(500.0)
                        .register(meterRegistry);

        summary.record(count);
    }
}
