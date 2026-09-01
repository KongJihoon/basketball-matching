package com.example.basketballmatching.global.monitoring.query;

public final class QueryRequestContextHolder {

    private static final ThreadLocal<QueryRequestContext> CONTEXT = new ThreadLocal<>();

    private QueryRequestContextHolder() {}

    public static void initialize(QueryRequestContext context) {

        CONTEXT.remove();
        CONTEXT.set(context);
    }

    public static QueryRequestContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }


}
