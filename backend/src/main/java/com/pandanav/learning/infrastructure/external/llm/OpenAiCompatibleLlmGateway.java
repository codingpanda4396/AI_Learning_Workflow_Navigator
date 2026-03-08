package com.pandanav.learning.infrastructure.external.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final RestClient restClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmGateway(RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LlmTextResult generate(LlmPrompt prompt) {
        Instant start = Instant.now();
        JsonNode requestPayload = objectMapper.valueToTree(Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", prompt.systemPrompt()),
                Map.of("role", "user", "content", prompt.userPrompt())
            ),
            "temperature", 0.2,
            "response_format", Map.of("type", "json_object")
        ));

        JsonNode response;
        try {
            response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(requestPayload)
                .retrieve()
                .body(JsonNode.class);
        } catch (Exception ex) {
            throw new InternalServerException("LLM provider call failed: " + ex.getMessage());
        }

        if (response == null) {
            throw new InternalServerException("LLM provider returned empty response.");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
            throw new InternalServerException("LLM provider response has no content.");
        }

        int latency = (int) Duration.between(start, Instant.now()).toMillis();
        JsonNode usageNode = response.path("usage");
        LlmUsage usage = new LlmUsage(
            usageNode.path("prompt_tokens").isNumber() ? usageNode.path("prompt_tokens").asInt() : null,
            usageNode.path("completion_tokens").isNumber() ? usageNode.path("completion_tokens").asInt() : null,
            latency
        );

        return new LlmTextResult(
            contentNode.asText(),
            properties.getProvider(),
            properties.getModel(),
            usage,
            requestPayload,
            response
        );
    }
}

