package com.pandanav.learning.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pandanav.learning.infrastructure.observability.TraceContext;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseMetadata(
    String schemaVersion,
    OffsetDateTime generatedAt,
    String traceId,
    String requestId,
    String strategy
) {

    private static final String SCHEMA_VERSION = "2026-03-14";

    public static ResponseMetadata now() {
        return now(null);
    }

    public static ResponseMetadata now(String strategy) {
        return new ResponseMetadata(
            SCHEMA_VERSION,
            OffsetDateTime.now(),
            TraceContext.traceId(),
            TraceContext.requestId(),
            strategy
        );
    }
}
