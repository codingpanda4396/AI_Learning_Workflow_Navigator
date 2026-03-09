package com.pandanav.learning.application.service.tutor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.TutorMessageRole;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;
import com.pandanav.learning.domain.model.TutorMessage;
import com.pandanav.learning.infrastructure.config.TutorLlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RealTutorProvider implements TutorProvider {

    private static final String FALLBACK_MESSAGE = "当前 AI 导师暂时不可用，请先说出你卡住的步骤，我会继续引导你。";
    private static final Logger log = LoggerFactory.getLogger(RealTutorProvider.class);

    private final RestClient restClient;
    private final TutorLlmProperties properties;
    private final PromptTemplateProvider promptTemplateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealTutorProvider(RestClient restClient, TutorLlmProperties properties, PromptTemplateProvider promptTemplateProvider) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptTemplateProvider = promptTemplateProvider;
    }

    @Override
    public TutorProviderReply generateReply(TutorProviderRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.getModel());
            payload.put("messages", buildMessages(request));
            payload.put("temperature", 0.2);

            JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(payload)
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        String errorBody = readBodyAsString(res.getBody());
                        throw new IllegalStateException(
                            "Tutor LLM HTTP error " + status.value() + ", body=" + truncate(errorBody, 300)
                        );
                    }
                    try (InputStream in = res.getBody()) {
                        if (in == null) {
                            return null;
                        }
                        return objectMapper.readTree(in);
                    }
                });

            String content = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.warn("Tutor LLM returned empty content. provider={}, model={}", properties.getProvider(), properties.getModel());
                return new TutorProviderReply(FALLBACK_MESSAGE, properties.getProvider(), properties.getModel());
            }

            // 防止单次回复过长导致教学节奏失控
            String normalized = truncate(content.trim(), 220);
            return new TutorProviderReply(normalized, properties.getProvider(), properties.getModel());
        } catch (Exception ex) {
            log.warn(
                "Tutor LLM call failed. provider={}, model={}, baseUrl={}, reason={}",
                properties.getProvider(),
                properties.getModel(),
                properties.getBaseUrl(),
                ex.getMessage()
            );
            return new TutorProviderReply(FALLBACK_MESSAGE, properties.getProvider(), properties.getModel());
        }
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
}
