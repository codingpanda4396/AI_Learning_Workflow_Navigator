package com.pandanav.learning.infrastructure.exception;

import com.pandanav.learning.api.dto.AiFailureResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<AiFailureResponse> handle(AiGenerationException ex) {
        return ResponseEntity.ok(
            new AiFailureResponse(
                false,
                "AI_GENERATION_FAILED",
                "个性化内容生成失败，请重试",
                true,
                MDC.get("traceId")
            )
        );
    }
}
