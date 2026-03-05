package com.pandanav.learning.api.dto;

public record ApiResponse(String status) {

    public static ApiResponse ok() {
        return new ApiResponse("ok");
    }
}
