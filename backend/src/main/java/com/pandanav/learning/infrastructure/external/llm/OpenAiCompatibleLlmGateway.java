package com.pandanav.learning.infrastructure.external.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmProfileConfig;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmGateway.class);

    private final RestClient.Builder restClientBuilder;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmGateway(RestClient.Builder restClientBuilder, LlmProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LlmTextResult generate(LlmPrompt prompt) {
        Instant start = Instant.now();
        LlmProfileConfig profile = properties.resolveProfile(prompt.invocationProfile(), prompt.promptKey());
        String model = (prompt.modelHint() == null || prompt.modelHint().isBlank())
            ? profile.model()
            : prompt.modelHint().trim();

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
        if (properties.isLogRequest()) {
            log.info(
                "LLM request: profile={}, promptKey={}, model={}, timeoutMs={}, maxTokens={}, payload={}",
                prompt.invocationProfile(),
                prompt.promptKey(),
                model,
                profile.timeoutMs(),
                maxOutputTokens,
                toCompactJson(requestPayload)
            );
        }

        boolean timeout = false;
        JsonNode response;
        try {
            response = invokeWithRetry(requestPayload, profile.timeoutMs());
        } catch (InternalServerException ex) {
            timeout = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timed out");
            throw ex;
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
            throw new InternalServerException("LLM provider response has no content.");
        }

        int latency = (int) Duration.between(start, Instant.now()).toMillis();
        JsonNode usageNode = response.path("usage");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        String finishReason = response.path("choices").path(0).path("finish_reason").asText("");
        Integer promptTokens = usageNode.path("prompt_tokens").isNumber() ? usageNode.path("prompt_tokens").asInt() : null;
        Integer completionTokens = usageNode.path("completion_tokens").isNumber() ? usageNode.path("completion_tokens").asInt() : null;
        Integer reasoningTokens = completionDetails.path("reasoning_tokens").isNumber()
            ? completionDetails.path("reasoning_tokens").asInt()
            : null;
        boolean truncated = "length".equalsIgnoreCase(finishReason);
        if (properties.isLogResponse()) {
            log.info(
                "LLM response: profile={}, promptKey={}, finishReason={}, promptTokens={}, completionTokens={}, reasoningTokens={}, latencyMs={}",
                prompt.invocationProfile(),
                prompt.promptKey(),
                finishReason,
                promptTokens,
                completionTokens,
                reasoningTokens,
                latency
            );
        }

        return new LlmTextResult(
            contentNode.asText(),
            profile.provider(),
            model,
            prompt.invocationProfile(),
            new LlmUsage(promptTokens, completionTokens, reasoningTokens, latency, finishReason, timeout, truncated),
            requestPayload,
            response
        );
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

        if (lastError instanceof InternalServerException internalServerException) {
            throw internalServerException;
        }
        throw new InternalServerException("LLM provider call failed: " + (lastError == null ? "unknown" : lastError.getMessage()));
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
                        throw new InternalServerException(
                            "LLM provider returned " + status + ": " + readBodyAsString(res.getBody()));
                    }
                    byte[] body;
                    try (InputStream in = res.getBody()) {
                        body = in.readAllBytes();
                    }
                    return ResponseEntity.status(status).headers(res.getHeaders()).body(body);
                });
        } catch (InternalServerException e) {
            throw e;
        } catch (ResourceAccessException ex) {
            throw new InternalServerException("LLM provider call timed out: " + ex.getMessage());
        } catch (Exception ex) {
            throw new InternalServerException("LLM provider call failed: " + ex.getMessage());
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
            throw new InternalServerException("LLM provider returned empty response.");
        }

        String text = new String(body, StandardCharsets.UTF_8);
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            String contentType = entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String preview = text.length() > 240 ? text.substring(0, 240) : text;
            throw new InternalServerException(
                "LLM provider response is not valid JSON. contentType=" + contentType + ", preview=" + preview
            );
        }
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            String msg = ex.getMessage();
            return msg != null && (msg.contains("Read timed out") || msg.contains("Connection reset") || msg.contains("I/O error"));
        }
        String msg = ex.getMessage();
        return msg != null && (msg.contains(" 429 ") || msg.contains(" 500 ") || msg.contains(" 502 ") || msg.contains(" 503 "));
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

    private String toCompactJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "{\"error\":\"failed to serialize request payload\"}";
        }
    }
}
