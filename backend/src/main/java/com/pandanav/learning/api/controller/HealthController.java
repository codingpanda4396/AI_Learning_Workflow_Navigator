package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.ok();
    }
}
