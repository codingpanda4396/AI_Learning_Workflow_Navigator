package com.pandanav.learning.api.dto;

public record ApiErrorResponse(
    String error,
    String message
) {
}
