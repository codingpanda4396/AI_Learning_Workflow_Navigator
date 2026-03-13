package com.pandanav.learning.infrastructure.observability;

import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class LlmCallLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmCallLogger.class);

    private final MeterRegistry meterRegistry;

    public LlmCallLogger(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    public void logStart(LlmCallContext context) {
        log.info("LLM_CALL_START stage={} model={}", context.stage(), safe(context.model()));
        increment("llm.call.total", context, List.of(Tag.of("result", "start")));
    }

    public void logSuccess(LlmCallContext context, LlmCallMetrics metrics) {
        log.info(
            "LLM_CALL_SUCCESS stage={} model={} latencyMs={} promptTokens={} completionTokens={} totalTokens={}",
            context.stage(),
            safe(context.model()),
            metrics.latencyMs(),
            metrics.promptTokens(),
            metrics.completionTokens(),
            metrics.totalTokens()
        );
        increment("llm.call.success", context, List.of(Tag.of("result", "success")));
        recordLatency(context, metrics.latencyMs(), "success", null);
    }

    public void logFallback(LlmCallContext context, LlmFallbackReason reason, int latencyMs) {
        log.warn(
            "LLM_CALL_FALLBACK stage={} model={} reason={} latencyMs={}",
            context.stage(),
            safe(context.model()),
            reason,
            latencyMs
        );
        increment("llm.call.fallback", context, List.of(Tag.of("result", "fallback"), Tag.of("reason", reason.name())));
        recordLatency(context, latencyMs, "fallback", reason.name());
    }

    public void logFailure(LlmCallContext context, LlmFailureType failureType, String message, int latencyMs) {
        log.error(
            "LLM_CALL_FAILURE stage={} model={} errorType={} latencyMs={} message={}",
            context.stage(),
            safe(context.model()),
            failureType,
            latencyMs,
            abbreviate(message)
        );
        increment("llm.call.failure", context, List.of(Tag.of("result", "failure"), Tag.of("reason", failureType.name())));
        recordLatency(context, latencyMs, "failure", failureType.name());
    }

    private void increment(String name, LlmCallContext context, List<Tag> extraTags) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(name, tags(context, extraTags)).increment();
    }

    private void recordLatency(LlmCallContext context, int latencyMs, String result, String reason) {
        if (meterRegistry == null || latencyMs < 0) {
            return;
        }
        Tags tags = tags(context, reason == null
            ? List.of(Tag.of("result", result))
            : List.of(Tag.of("result", result), Tag.of("reason", reason)));
        meterRegistry.timer("llm.call.latency", tags).record(latencyMs, TimeUnit.MILLISECONDS);
    }

    private Tags tags(LlmCallContext context, List<Tag> extraTags) {
        Tags tags = Tags.of(
            Tag.of("stage", context.stage().name()),
            Tag.of("model", safe(context.model()))
        );
        return tags.and(extraTags);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        String normalized = message.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
