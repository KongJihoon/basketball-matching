package com.example.basketballmatching.global.monitoring.query;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class QueryCountInspector implements StatementInspector {
    @Override
    public String inspect(String sql) {
        QueryRequestContext context = QueryRequestContextHolder.getContext();

        if (context != null) {
            context.incrementQueryCount(sql);
        }

        return sql;
    }
}
