package com.pandanav.learning.infrastructure.external.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallException;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmProfileConfig;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.TraceContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final RestClient.Builder restClientBuilder;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier failureClassifier;

    public OpenAiCompatibleLlmGateway(
        RestClient.Builder restClientBuilder,
        LlmProperties properties,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier failureClassifier
    ) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.llmCallLogger = llmCallLogger;
        this.failureClassifier = failureClassifier;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LlmTextResult generate(LlmStage stage, LlmPrompt prompt) {
        Instant start = Instant.now();
        LlmProfileConfig profile = properties.resolveProfile(prompt.invocationProfile(), prompt.promptKey());
        String model = (prompt.modelHint() == null || prompt.modelHint().isBlank())
            ? profile.model()
            : prompt.modelHint().trim();
        LlmCallContext context = new LlmCallContext(TraceContext.traceId(), TraceContext.requestId(), stage, model);
        if (properties.getObservability().isEnabled()) {
            llmCallLogger.logStart(context);
        }

        if (properties.isForceFallback()) {
            int latencyMs = elapsedMs(start);
            if (properties.getObservability().isEnabled()) {
                llmCallLogger.logFallback(context, com.pandanav.learning.domain.llm.model.LlmFallbackReason.FORCE_FALLBACK, latencyMs);
            }
            throw new LlmCallException(LlmFailureType.UNKNOWN_ERROR, "LLM force fallback is enabled.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
            Map.of("role", "system", "content", prompt.systemPrompt()),
            Map.of("role", "user", "content", prompt.userPrompt())
        ));
        payload.put("temperature", profile.temperature());
        if (profile.jsonResponse()) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        Integer maxOutputTokens = prompt.maxOutputTokens() != null ? prompt.maxOutputTokens() : profile.maxTokens();
        if (maxOutputTokens != null && maxOutputTokens > 0) {
            payload.put("max_tokens", maxOutputTokens);
        }
        if (profile.extraParams() != null && !profile.extraParams().isEmpty()) {
            payload.putAll(profile.extraParams());
        }

        JsonNode requestPayload = objectMapper.valueToTree(payload);
        try {
            JsonNode response = invokeWithRetry(requestPayload, profile.timeoutMs());
            JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
                throw new LlmCallException(LlmFailureType.EMPTY_RESPONSE, "LLM provider response has no content.");
            }

            LlmUsage usage = extractUsage(response, elapsedMs(start));
            LlmTextResult result = new LlmTextResult(
                contentNode.asText(),
                profile.provider(),
                model,
                prompt.invocationProfile(),
                usage,
                requestPayload,
                response
            );
            if (properties.getObservability().isEnabled()) {
                llmCallLogger.logSuccess(
                    context,
                    LlmCallMetrics.from(usage),
                    usage == null ? null : usage.finishReason(),
                    usage != null && usage.truncated()
                );
            }
            return result;
        } catch (RuntimeException ex) {
            int latencyMs = elapsedMs(start);
            if (properties.getObservability().isEnabled()) {
                llmCallLogger.logFailure(context, failureClassifier.classifyFailure(ex), ex.getMessage(), latencyMs);
            }
            throw ex;
        }
    }

    private JsonNode invokeWithRetry(JsonNode requestPayload, Integer timeoutMs) {
        int attempts = properties.getMaxRetries() + 1;
        Exception lastError = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                ResponseEntity<byte[]> entity = callProvider(requestPayload, timeoutMs);
                return parseResponse(entity);
            } catch (Exception ex) {
                lastError = ex;
                boolean shouldRetry = i < attempts && isRetryable(ex);
                if (!shouldRetry) {
                    break;
                }
                sleepBackoff();
            }
        }

        if (lastError instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new LlmCallException(LlmFailureType.API_ERROR, "LLM provider call failed: " + (lastError == null ? "unknown" : lastError.getMessage()));
    }

    private ResponseEntity<byte[]> callProvider(JsonNode requestPayload, Integer timeoutMs) {
        RestClient restClient = buildClient(timeoutMs);
        try {
            return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(requestPayload)
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        throw new LlmCallException(
                            LlmFailureType.API_ERROR,
                            "LLM provider returned " + status + ": " + readBodyAsString(res.getBody()));
                    }
                    byte[] body;
                    try (InputStream in = res.getBody()) {
                        body = in.readAllBytes();
                    }
                    return ResponseEntity.status(status).headers(res.getHeaders()).body(body);
                });
        } catch (LlmCallException e) {
            throw e;
        } catch (ResourceAccessException ex) {
            throw new LlmCallException(LlmFailureType.TIMEOUT, "LLM provider call timed out: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new LlmCallException(LlmFailureType.API_ERROR, "LLM provider call failed: " + ex.getMessage(), ex);
        }
    }

    private RestClient buildClient(Integer timeoutMs) {
        int resolvedTimeout = timeoutMs == null || timeoutMs <= 0 ? properties.getTimeoutMs() : timeoutMs;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(resolvedTimeout);
        requestFactory.setReadTimeout(resolvedTimeout);
        return restClientBuilder.baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
    }

    private JsonNode parseResponse(ResponseEntity<byte[]> entity) {
        byte[] body = entity.getBody();
        if (body == null || body.length == 0) {
            throw new LlmCallException(LlmFailureType.EMPTY_RESPONSE, "LLM provider returned empty response.");
        }

        String text = new String(body, StandardCharsets.UTF_8);
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            String contentType = entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String preview = text.length() > 240 ? text.substring(0, 240) : text;
            throw new LlmCallException(
                LlmFailureType.JSON_PARSE_ERROR,
                "LLM provider response is not valid JSON. contentType=" + contentType + ", preview=" + preview
            );
        }
    }

    private LlmUsage extractUsage(JsonNode response, int latencyMs) {
        JsonNode usageNode = response.path("usage");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        String finishReason = response.path("choices").path(0).path("finish_reason").asText("");
        Integer promptTokens = usageNode.path("prompt_tokens").isNumber() ? usageNode.path("prompt_tokens").asInt() : -1;
        Integer completionTokens = usageNode.path("completion_tokens").isNumber() ? usageNode.path("completion_tokens").asInt() : -1;
        Integer totalTokens = usageNode.path("total_tokens").isNumber() ? usageNode.path("total_tokens").asInt() : -1;
        Integer reasoningTokens = completionDetails.path("reasoning_tokens").isNumber()
            ? completionDetails.path("reasoning_tokens").asInt()
            : -1;
        boolean truncated = "length".equalsIgnoreCase(finishReason);
        return new LlmUsage(promptTokens, completionTokens, totalTokens, reasoningTokens, latencyMs, finishReason, false, truncated);
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            String msg = ex.getMessage();
            return isRetryableMessage(msg);
        }
        String msg = ex.getMessage();
        return isRetryableMessage(msg);
    }

    private boolean isRetryableMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String normalized = msg.toLowerCase();
        return normalized.contains("timed out")
            || normalized.contains("connection reset")
            || normalized.contains("i/o error")
            || normalized.contains(" 408 ")
            || normalized.contains(" 429 ")
            || normalized.contains(" 500 ")
            || normalized.contains(" 502 ")
            || normalized.contains(" 503 ")
            || normalized.contains(" 504 ");
    }

    private void sleepBackoff() {
        if (properties.getRetryBackoffMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(properties.getRetryBackoffMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String readBodyAsString(InputStream in) {
        if (in == null) {
            return "";
        }
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(unable to read body: " + e.getMessage() + ")";
        }
    }

    private int elapsedMs(Instant start) {
        return (int) Duration.between(start, Instant.now()).toMillis();
    }
}
