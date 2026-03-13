package com.pandanav.learning.application.service.tutor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.TutorMessageRole;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;
import com.pandanav.learning.domain.model.TutorMessage;
import com.pandanav.learning.infrastructure.config.TutorLlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RealTutorProvider implements TutorProvider {

    private static final String FALLBACK_MESSAGE = "当前 AI 导师暂时不可用，请先描述你卡住的步骤，我会继续帮助你。";
    private static final Logger log = LoggerFactory.getLogger(RealTutorProvider.class);

    private final RestClient restClient;
    private final TutorLlmProperties properties;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier llmFailureClassifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealTutorProvider(
        RestClient restClient,
        TutorLlmProperties properties,
        PromptTemplateProvider promptTemplateProvider,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmCallLogger = llmCallLogger;
        this.llmFailureClassifier = llmFailureClassifier;
    }

    @Override
    public TutorProviderReply generateReply(TutorProviderRequest request) {
        Instant start = Instant.now();
        llmCallLogger.logStart(LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()));
        try {
            JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(buildPayload(request, false))
                .exchange((req, res) -> readJsonResponse(res.getStatusCode(), res.getBody()));

            String content = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                llmCallLogger.logFallback(
                    LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                    com.pandanav.learning.domain.llm.model.LlmFallbackReason.EMPTY_RESPONSE,
                    LlmObservabilityHelper.elapsedMs(start)
                );
                return fallbackReply();
            }
            llmCallLogger.logSuccess(
                LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                buildMetrics(response, start)
            );
            return new TutorProviderReply(truncate(content.trim(), 220), properties.getProvider(), properties.getModel());
        } catch (Exception ex) {
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                llmFailureClassifier.classifyFailure(ex),
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                llmFailureClassifier.classifyFallback(ex),
                LlmObservabilityHelper.elapsedMs(start)
            );
            log.warn("Tutor LLM call failed. provider={}, model={}, reason={}", properties.getProvider(), properties.getModel(), ex.getMessage());
            return fallbackReply();
        }
    }

    @Override
    public TutorProviderReply streamReply(TutorProviderRequest request, Consumer<String> onDelta) {
        if (!properties.isStreamEnabled()) {
            return TutorProvider.super.streamReply(request, onDelta);
        }
        StringBuilder fullContent = new StringBuilder();
        Instant start = Instant.now();
        llmCallLogger.logStart(LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()));
        try {
            restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(buildPayload(request, true))
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        throw new IllegalStateException("Tutor LLM HTTP error " + status.value());
                    }
                    readStreamBody(res.getBody(), fullContent, onDelta);
                    return null;
                });
            if (fullContent.length() == 0) {
                return fallbackReply();
            }
            llmCallLogger.logSuccess(
                LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                new LlmCallMetrics(LlmObservabilityHelper.elapsedMs(start), -1, -1, -1)
            );
            return new TutorProviderReply(truncate(fullContent.toString().trim(), 220), properties.getProvider(), properties.getModel());
        } catch (Exception ex) {
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.TUTOR_REPLY, properties.getModel()),
                llmFailureClassifier.classifyFailure(ex),
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            log.warn("Tutor LLM stream failed, fallback to sync. provider={}, model={}, reason={}", properties.getProvider(), properties.getModel(), ex.getMessage());
            TutorProviderReply fallback = generateReply(request);
            if (onDelta != null && fallback != null && fallback.content() != null && !fallback.content().isBlank() && fullContent.length() == 0) {
                onDelta.accept(fallback.content());
            }
            return fallback;
        }
    }

    private Map<String, Object> buildPayload(TutorProviderRequest request, boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", buildMessages(request));
        payload.put("temperature", 0.2);
        payload.put("max_tokens", properties.getMaxOutputTokens());
        payload.put("stream", stream);
        return payload;
    }

    private List<Map<String, String>> buildMessages(TutorProviderRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(request)));
        List<TutorMessage> history = request.history();
        if (history != null && !history.isEmpty()) {
            for (TutorMessage item : history) {
                if (item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                String role = item.getRole() == TutorMessageRole.ASSISTANT ? "assistant" : "user";
                messages.add(Map.of("role", role, "content", item.getContent()));
            }
        } else if (request.userMessage() != null && !request.userMessage().isBlank()) {
            messages.add(Map.of("role", "user", "content", request.userMessage()));
        }
        return messages;
    }

    private String buildSystemPrompt(TutorProviderRequest request) {
        return promptTemplateProvider.buildTutorSystemPrompt(new TutorPromptContext(
            safe(request.taskStage()),
            safe(request.taskObjective()),
            safe(request.nodeName()),
            safe(request.sessionGoal()),
            request.hintMode(),
            request.directAnswerMode()
        ));
    }

    private JsonNode readJsonResponse(HttpStatusCode status, InputStream body) {
        if (status.is4xxClientError() || status.is5xxServerError()) {
            throw new IllegalStateException("Tutor LLM HTTP error " + status.value() + ", body=" + readBodyAsString(body));
        }
        try (InputStream in = body) {
            return in == null ? null : objectMapper.readTree(in);
        } catch (Exception ex) {
            throw new IllegalStateException("Tutor LLM response parse failed: " + ex.getMessage());
        }
    }

    private void readStreamBody(InputStream body, StringBuilder fullContent, Consumer<String> onDelta) {
        if (body == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isBlank() || "[DONE]".equals(payload)) {
                    continue;
                }
                JsonNode json = objectMapper.readTree(payload);
                String chunk = json.path("choices").path(0).path("delta").path("content").asText("");
                if (chunk.isBlank()) {
                    continue;
                }
                fullContent.append(chunk);
                if (onDelta != null) {
                    onDelta.accept(chunk);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Tutor stream parse failed: " + ex.getMessage(), ex);
        }
    }

    private TutorProviderReply fallbackReply() {
        return new TutorProviderReply(FALLBACK_MESSAGE, properties.getProvider(), properties.getModel());
    }

    private String safe(String input) {
        return input == null || input.isBlank() ? "(unknown)" : input.trim();
    }

    private static String readBodyAsString(InputStream in) {
        if (in == null) {
            return "";
        }
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "(unable to read error body: " + ex.getMessage() + ")";
        }
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private LlmCallMetrics buildMetrics(JsonNode response, Instant start) {
        JsonNode usage = response == null ? null : response.path("usage");
        int promptTokens = usage != null && usage.path("prompt_tokens").isNumber() ? usage.path("prompt_tokens").asInt() : -1;
        int completionTokens = usage != null && usage.path("completion_tokens").isNumber() ? usage.path("completion_tokens").asInt() : -1;
        int totalTokens = usage != null && usage.path("total_tokens").isNumber() ? usage.path("total_tokens").asInt() : -1;
        return new LlmCallMetrics(LlmObservabilityHelper.elapsedMs(start), promptTokens, completionTokens, totalTokens);
    }
}
