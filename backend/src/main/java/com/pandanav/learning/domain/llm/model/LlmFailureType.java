package com.pandanav.learning.domain.llm.model;

public enum LlmFailureType {
    LLM_API_ERROR,
    LLM_TIMEOUT,
    LLM_JSON_PARSE_ERROR,
    LLM_TRUNCATED,
    API_ERROR,
    TIMEOUT,
    JSON_PARSE_ERROR,
    EMPTY_RESPONSE,
    VALIDATION_ERROR,
    UNKNOWN_ERROR
}
