package com.pandanav.learning.api.dto;

public record ApiEnvelope<T>(String code, String message, T data) {

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>("OK", "success", data);
    }
}
