package com.example.basketballmatching.global.monitoring.query;

import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class QueryRequestContext {

    private final String httpMethod;
    private final String path;

    private final Map<QueryType, Integer> queryTypeCountByType = new EnumMap<>(QueryType.class);

    public QueryRequestContext(String httpMethod, String path) {
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public void incrementQueryCount(String sql) {
        QueryType queryType = QueryType.from(sql);

        queryTypeCountByType.merge(queryType, 1, Integer::sum);
    }

}
