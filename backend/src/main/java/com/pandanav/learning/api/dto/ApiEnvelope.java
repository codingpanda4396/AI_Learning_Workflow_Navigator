package com.pandanav.learning.api.dto;

public record ApiEnvelope<T>(String code, String message, T data, ResponseMetadata metadata) {

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>("OK", "success", data, ResponseMetadata.now());
    }

    public static <T> ApiEnvelope<T> ok(T data, String strategy) {
        return new ApiEnvelope<>("OK", "success", data, ResponseMetadata.now(strategy));
    }
}
