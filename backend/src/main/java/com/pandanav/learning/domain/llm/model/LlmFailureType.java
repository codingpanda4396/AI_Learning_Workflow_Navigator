package com.pandanav.learning.domain.llm.model;

public enum LlmFailureType {
    API_ERROR,
    TIMEOUT,
    JSON_PARSE_ERROR,
    EMPTY_RESPONSE,
    VALIDATION_ERROR,
    UNKNOWN_ERROR
}
