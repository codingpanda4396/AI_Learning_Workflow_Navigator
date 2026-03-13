package com.pandanav.learning.domain.llm.model;

public enum LlmFallbackReason {
    LLM_TIMEOUT,
    LLM_API_ERROR,
    JSON_PARSE_ERROR,
    JSON_EXTRA_TEXT,
    JSON_SCHEMA_MISMATCH,
    JSON_EMPTY_RESPONSE,
    EMPTY_RESPONSE,
    MISSING_REQUIRED_FIELDS,
    FORCE_FALLBACK,
    UNKNOWN_ERROR
}
