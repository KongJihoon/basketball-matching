package com.example.basketballmatching.global.monitoring.query;

import java.util.Locale;

public enum QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    OTHER;

    public static QueryType from(String sql) {

        if (sql == null || sql.isBlank()) {
            return OTHER;
        }

        String normalizedSql = removeLeadingComment(sql)
                .stripLeading().toUpperCase(Locale.ROOT);

        if (normalizedSql.startsWith("SELECT")) {
            return SELECT;
        }

        if (normalizedSql.startsWith("INSERT")) {
            return INSERT;
        }

        if (normalizedSql.startsWith("UPDATE")) {
            return UPDATE;
        }

        if (normalizedSql.startsWith("DELETE")) {
            return DELETE;
        }

        return OTHER;


    }

    private static String removeLeadingComment(String sql) {
        String result = sql;

        while (result.stripLeading().startsWith("/*")) {
            result = result.stripLeading();

            int commentEndIndex = result.indexOf("*/");

            if (commentEndIndex < 0) {
                return result;
            }

            result = result.substring(commentEndIndex + 2);
        }

        return result;
    }
}
