package com.pandanav.learning.infrastructure.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LlmStage;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LlmCallLoggerTest {

    @Test
    void shouldWriteStructuredSuccessAndFailureFields() {
        Logger logger = (Logger) LoggerFactory.getLogger(LlmCallLogger.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            LlmCallLogger llmCallLogger = new LlmCallLogger(mock(ObjectProvider.class));
            LlmCallContext context = new LlmCallContext("trace-1", "req-1", LlmStage.CAPABILITY_SUMMARY, "qwen3.5-plus");

            llmCallLogger.logSuccess(context, new LlmCallMetrics(842, 231, 112, 343));
            llmCallLogger.logFallback(context, LlmFallbackReason.LLM_TIMEOUT, 900);
            llmCallLogger.logFailure(context, LlmFailureType.JSON_PARSE_ERROR, "invalid json", 901);
            llmCallLogger.logStructuredOutputFailure(context, "JSON_SCHEMA_MISMATCH", "$.headline is required");

            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
            assertTrue(logs.contains("LLM_CALL_SUCCESS stage=CAPABILITY_SUMMARY model=qwen3.5-plus traceId=trace-1 requestId=req-1 latencyMs=842 promptTokens=231 completionTokens=112 totalTokens=343"));
            assertTrue(logs.contains("LLM_CALL_FALLBACK stage=CAPABILITY_SUMMARY model=qwen3.5-plus traceId=trace-1 requestId=req-1 reason=LLM_TIMEOUT"));
            assertTrue(logs.contains("LLM_CALL_FAILURE stage=CAPABILITY_SUMMARY model=qwen3.5-plus traceId=trace-1 requestId=req-1 errorType=JSON_PARSE_ERROR"));
            assertTrue(logs.contains("LLM_STRUCTURED_OUTPUT_FAILURE stage=CAPABILITY_SUMMARY model=qwen3.5-plus traceId=trace-1 requestId=req-1 reason=JSON_SCHEMA_MISMATCH"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
