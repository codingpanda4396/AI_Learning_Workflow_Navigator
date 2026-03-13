package com.pandanav.learning.infrastructure.observability;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";

    private TraceContext() {
    }

    public static String traceId() {
        return valueOrGenerate(TRACE_ID);
    }

    public static String requestId() {
        return valueOrGenerate(REQUEST_ID);
    }

    public static void put(String traceId, String requestId) {
        MDC.put(TRACE_ID, blankToRandom(traceId));
        MDC.put(REQUEST_ID, blankToRandom(requestId));
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(REQUEST_ID);
    }

    private static String valueOrGenerate(String key) {
        String value = MDC.get(key);
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString().replace("-", "");
            MDC.put(key, value);
        }
        return value;
    }

    private static String blankToRandom(String value) {
        return (value == null || value.isBlank())
            ? UUID.randomUUID().toString().replace("-", "")
            : value.trim();
    }
}
