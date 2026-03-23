package navigator.application.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.infrastructure.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI 兼容 Chat Completions；长超时与 2s 反馈超时各用独立 {@link RestTemplate}。
 */
@Component
public class OpenAiCompatibleLlmClientAdapter implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClientAdapter.class);

    public static final int FEEDBACK_READ_TIMEOUT_MS = 2_000;

    private final LlmProperties props;
    private final RestTemplate restTemplateLong;
    private final RestTemplate restTemplateFeedback;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClientAdapter(LlmProperties props,
                                            RestTemplateBuilder restTemplateBuilder,
                                            ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        int readMs = Math.max(1000, props.getTimeoutMs());
        int connectLong = Math.min(readMs, 15_000);
        this.restTemplateLong = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectLong))
                .setReadTimeout(Duration.ofMillis(readMs))
                .build();
        int fbRead = Math.max(500, FEEDBACK_READ_TIMEOUT_MS);
        int connectFb = Math.min(fbRead, 2_000);
        this.restTemplateFeedback = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectFb))
                .setReadTimeout(Duration.ofMillis(fbRead))
                .build();
    }

    @Override
    public boolean isLiveProviderReady() {
        return props.isEnabled()
                && props.getApiKey() != null
                && !props.getApiKey().isBlank();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return exchange(restTemplateLong, systemPrompt, userPrompt, props.getTimeoutMs());
    }

    @Override
    public String chatForFeedback(String systemPrompt, String userPrompt) {
        return exchange(restTemplateFeedback, systemPrompt, userPrompt, FEEDBACK_READ_TIMEOUT_MS);
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onDelta)
            throws IOException, InterruptedException {
        if (!isLiveProviderReady()) {
            throw new IllegalStateException("LLM provider disabled or apiKey empty");
        }
        String base = props.getBaseUrl() != null ? props.getBaseUrl().trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/v1/chat/completions";
        int readMs = Math.max(1000, props.getTimeoutMs());
        int connectMs = Math.min(readMs, 15_000);

        ChatCompletionStreamRequest req = new ChatCompletionStreamRequest(
                props.getModel(),
                List.of(
                        new Message("system", systemPrompt != null ? systemPrompt : ""),
                        new Message("user", userPrompt != null ? userPrompt : "")
                ),
                0.2,
                true
        );
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .build();
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(readMs))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        log.debug("LLM HTTP stream POST {} model={} readTimeoutMs={}", url, props.getModel(), readMs);

        HttpResponse<InputStream> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            String errBody = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
            log.warn("LLM stream non-success: status={} bodyPreview={}",
                    resp.statusCode(), truncateForLog(errBody, 800));
            throw new IllegalStateException("LLM provider non-2xx: " + resp.statusCode());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }
                if (payload.isEmpty()) {
                    continue;
                }
                String piece = extractStreamDeltaContent(payload);
                if (piece != null && !piece.isEmpty()) {
                    onDelta.accept(piece);
                }
            }
        }
    }

    private String extractStreamDeltaContent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.hasNonNull("error")) {
                log.warn("LLM stream error field: {}", truncateForLog(payload, 500));
                return null;
            }
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                return null;
            }
            JsonNode content = delta.get("content");
            if (content == null || !content.isTextual()) {
                return null;
            }
            return content.asText();
        } catch (Exception e) {
            log.debug("LLM stream line parse skip: {}", e.getMessage());
            return null;
        }
    }

    private String exchange(RestTemplate restTemplate, String systemHint, String userContent, int readTimeoutMs) {
        if (!isLiveProviderReady()) {
            throw new IllegalStateException("LLM provider disabled or apiKey empty");
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

        int sysLen = systemHint != null ? systemHint.length() : 0;
        int userLen = userContent != null ? userContent.length() : 0;
        log.debug("LLM HTTP POST {} model={} readTimeoutMs={} systemChars={} userChars={}",
                url, props.getModel(), readTimeoutMs, sysLen, userLen);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("LLM HTTP non-success: status={} bodyPreview={}",
                        resp.getStatusCode(), truncateForLog(resp.getBody(), 800));
                throw new IllegalStateException("LLM provider non-2xx: " + resp.getStatusCode());
            }
            ChatCompletionResponse parsed = objectMapper.readValue(resp.getBody(), ChatCompletionResponse.class);
            if (parsed == null || parsed.choices == null || parsed.choices.isEmpty()
                    || parsed.choices.get(0).message == null || parsed.choices.get(0).message.content == null) {
                log.warn("LLM HTTP 2xx but empty assistant content; bodyPreview={}",
                        truncateForLog(resp.getBody(), 800));
                throw new IllegalStateException("LLM provider empty reply");
            }
            String reply = parsed.choices.get(0).message.content.trim();
            log.debug("LLM HTTP 2xx: assistantChars={}", reply.length());
            return reply;
        } catch (RestClientException ex) {
            log.warn("LLM HTTP request failed: {}", ex.getMessage());
            log.debug("LLM HTTP request failed detail", ex);
            throw new IllegalStateException("LLM provider request failed", ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("LLM response parse failed: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
            log.debug("LLM response parse failed detail", ex);
            throw new IllegalStateException("LLM provider parse failed", ex);
        }
    }

    private static String truncateForLog(String body, int maxChars) {
        if (body == null) {
            return "";
        }
        String t = body.replace("\r\n", "\n").trim();
        return t.length() <= maxChars ? t : t.substring(0, maxChars) + "…(truncated)";
    }

    public record ChatCompletionRequest(
            String model,
            List<Message> messages,
            @JsonProperty("temperature") Double temperature
    ) {
    }

    private record ChatCompletionStreamRequest(
            String model,
            List<Message> messages,
            @JsonProperty("temperature") double temperature,
            @JsonProperty("stream") boolean stream
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
