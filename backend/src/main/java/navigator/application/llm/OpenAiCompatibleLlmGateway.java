package navigator.application.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Minimal OpenAI-compatible implementation.
 * It uses /v1/chat/completions with {model, messages}.
 */
@Component
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final LlmProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmGateway(LlmProperties props, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(Math.max(1000, props.getTimeoutMs())))
                .setReadTimeout(Duration.ofMillis(Math.max(1000, props.getTimeoutMs())))
                .build();
    }

    @Override
    public String generateReply(String systemHint, String userContent) {
        if (!props.isEnabled()) {
            throw new IllegalStateException("LLM provider disabled");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("LLM apiKey is empty");
        }

        String base = props.getBaseUrl() != null ? props.getBaseUrl().trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/v1/chat/completions";

        ChatCompletionRequest req = new ChatCompletionRequest(
                props.getModel(),
                List.of(
                        new Message("system", systemHint != null ? systemHint : ""),
                        new Message("user", userContent != null ? userContent : "")
                ),
                0.2
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("LLM provider non-2xx: " + resp.getStatusCode());
            }
            ChatCompletionResponse parsed = objectMapper.readValue(resp.getBody(), ChatCompletionResponse.class);
            if (parsed == null || parsed.choices == null || parsed.choices.isEmpty()
                    || parsed.choices.get(0).message == null || parsed.choices.get(0).message.content == null) {
                throw new IllegalStateException("LLM provider empty reply");
            }
            return parsed.choices.get(0).message.content.trim();
        } catch (RestClientException ex) {
            throw new IllegalStateException("LLM provider request failed", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("LLM provider parse failed", ex);
        }
    }

    public record ChatCompletionRequest(
            String model,
            List<Message> messages,
            @JsonProperty("temperature") Double temperature
    ) {
    }

    public record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatCompletionResponse {
        public List<Choice> choices;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            public OutMessage message;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OutMessage {
            public String content;
        }
    }
}

