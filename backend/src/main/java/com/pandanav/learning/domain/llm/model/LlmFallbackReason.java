package com.pandanav.learning.domain.llm.model;

public enum LlmFallbackReason {
    LLM_TIMEOUT,
    LLM_API_ERROR,
    JSON_PARSE_ERROR,
    EMPTY_RESPONSE,
    MISSING_REQUIRED_FIELDS,
    FORCE_FALLBACK,
    UNKNOWN_ERROR
}
