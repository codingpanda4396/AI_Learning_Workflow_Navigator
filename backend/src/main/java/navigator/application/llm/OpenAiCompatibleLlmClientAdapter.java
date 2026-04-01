package navigator.application.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import navigator.infrastructure.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI 鍏煎 Chat Completions锛涢暱瓒呮椂涓?2s 鍙嶉瓒呮椂鍚勭敤鐙珛 RestTemplate锛涙祦寮忚矾寰勪娇鐢?WebClient + Flux銆?
 */
@Component
public class OpenAiCompatibleLlmClientAdapter implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClientAdapter.class);

    private final LlmProperties props;
    private final RestTemplate restTemplateLong;
    private final RestTemplate restTemplateFeedback;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClientAdapter(LlmProperties props,
                                            RestTemplateBuilder restTemplateBuilder,
                                            WebClient.Builder webClientBuilder,
                                            ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;

        int readMs = Math.max(1000, props.getTimeoutMs());
        int connectLong = normalizeConnectTimeout(props.getConnectTimeoutMs(), readMs, 15_000);
        this.restTemplateLong = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectLong))
                .setReadTimeout(Duration.ofMillis(readMs))
                .build();

        int fbRead = Math.max(1000, props.getFeedbackTimeoutMs());
        int connectFb = normalizeConnectTimeout(props.getConnectTimeoutMs(), fbRead, 5_000);
        this.restTemplateFeedback = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectFb))
                .setReadTimeout(Duration.ofMillis(fbRead))
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectLong)
                .responseTimeout(Duration.ofMillis(readMs));
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
        return exchange(
                restTemplateLong,
                systemPrompt,
                userPrompt,
                props.getTimeoutMs(),
                props.getChatMaxTokens());
    }

    @Override
    public String chatForFeedback(String systemPrompt, String userPrompt) {
        return exchange(
                restTemplateFeedback,
                systemPrompt,
                userPrompt,
                props.getFeedbackTimeoutMs(),
                props.getFeedbackMaxTokens());
    }

    @Override
    public String chatForScaffold(String systemPrompt, String userPrompt) {
        return exchange(
                restTemplateLong,
                systemPrompt,
                userPrompt,
                props.getTimeoutMs(),
                props.getScaffoldMaxTokens());
    }

    @Override
    public Flux<String> chatStream(String systemPrompt, String userPrompt) {
        if (!isLiveProviderReady()) {
            return Flux.error(new IllegalStateException("LLM provider disabled or apiKey empty"));
        }

        String url = buildChatCompletionsUrl();
        ChatCompletionStreamRequest req = new ChatCompletionStreamRequest(
                props.getModel(),
                List.of(
                        new Message("system", systemPrompt != null ? systemPrompt : ""),
                        new Message("user", userPrompt != null ? userPrompt : "")
                ),
                0.2,
                true,
                normalizeMaxTokens(props.getStreamMaxTokens())
        );

        log.debug("LLM HTTP stream POST {} model={} stream=true apiKeyFingerprint={} readTimeoutMs={}",
                url, props.getModel(), maskKey(props.getApiKey()), props.getTimeoutMs());

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .headers(headers -> headers.setBearerAuth(props.getApiKey()))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            log.warn("LLM stream non-success: status={} bodyPreview={}",
                                    response.statusCode().value(), truncateForLog(body, 800));
                            return new IllegalStateException("LLM provider non-2xx: " + response.statusCode().value());
                        }))
                .bodyToFlux(DataBuffer.class)
                .transform(this::decodeStreamChunks);
    }

    private Flux<String> decodeStreamChunks(Flux<DataBuffer> body) {
        return Flux.defer(() -> {
            StringBuilder carry = new StringBuilder();
            Flux<String> chunks = body.flatMapIterable(buffer -> {
                try {
                    carry.append(buffer.toString(StandardCharsets.UTF_8));
                    return drainCompleteLines(carry);
                } finally {
                    DataBufferUtils.release(buffer);
                }
            });
            return chunks.concatWith(Flux.defer(() -> flushTail(carry)));
        });
    }

    private List<String> drainCompleteLines(StringBuilder carry) {
        List<String> pieces = new java.util.ArrayList<>();
        int newline;
        while ((newline = carry.indexOf("\n")) >= 0) {
            String line = carry.substring(0, newline);
            carry.delete(0, newline + 1);
            String piece = extractDeltaFromLine(line);
            if (piece != null && !piece.isEmpty()) {
                pieces.add(piece);
            }
        }
        return pieces;
    }

    private Flux<String> flushTail(StringBuilder carry) {
        if (carry.length() == 0) {
            return Flux.empty();
        }
        String tail = carry.toString();
        carry.setLength(0);
        String piece = extractDeltaFromLine(tail);
        return piece == null || piece.isEmpty() ? Flux.empty() : Flux.just(piece);
    }

    private String extractDeltaFromLine(String rawLine) {
        String line = rawLine == null ? "" : rawLine.replace("\r", "").trim();
        if (line.isEmpty() || !line.startsWith("data:")) {
            return null;
        }
        String payload = line.substring(5).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            return null;
        }
        return extractStreamDeltaContent(payload);
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

    private String exchange(RestTemplate restTemplate,
                            String systemHint,
                            String userContent,
                            int readTimeoutMs,
                            Integer maxTokens) {
        if (!isLiveProviderReady()) {
            throw new IllegalStateException("LLM provider disabled or apiKey empty");
        }

        String url = buildChatCompletionsUrl();
        ChatCompletionRequest req = new ChatCompletionRequest(
                props.getModel(),
                List.of(
                        new Message("system", systemHint != null ? systemHint : ""),
                        new Message("user", userContent != null ? userContent : "")
                ),
                0.2,
                normalizeMaxTokens(maxTokens)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(req, headers);

        int sysLen = systemHint != null ? systemHint.length() : 0;
        int userLen = userContent != null ? userContent.length() : 0;
        log.debug("LLM HTTP POST {} model={} stream=false apiKeyFingerprint={} readTimeoutMs={} systemChars={} userChars={}",
                url, props.getModel(), maskKey(props.getApiKey()), readTimeoutMs, sysLen, userLen);

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
            log.warn("LLM response parse failed: {} 鈥?{}", ex.getClass().getSimpleName(), ex.getMessage());
            log.debug("LLM response parse failed detail", ex);
            throw new IllegalStateException("LLM provider parse failed", ex);
        }
    }

    private String sanitizeBaseUrl() {
        String base = props.getBaseUrl() != null ? props.getBaseUrl().trim() : "";
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String buildChatCompletionsUrl() {
        String base = sanitizeBaseUrl();
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }

    private static String truncateForLog(String body, int maxChars) {
        if (body == null) {
            return "";
        }
        String t = body.replace("\r\n", "\n").trim();
        return t.length() <= maxChars ? t : t.substring(0, maxChars) + "鈥?truncated)";
    }

    public record ChatCompletionRequest(
            String model,
            List<Message> messages,
            @JsonProperty("temperature") Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens
    ) {
    }

    private record ChatCompletionStreamRequest(
            String model,
            List<Message> messages,
            @JsonProperty("temperature") double temperature,
            @JsonProperty("stream") boolean stream,
            @JsonProperty("max_tokens") Integer maxTokens
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

    private static Integer normalizeMaxTokens(Integer maxTokens) {
        return maxTokens != null && maxTokens > 0 ? maxTokens : null;
    }

    private static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "EMPTY";
        }
        String trimmed = key.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    private static int normalizeConnectTimeout(int configuredConnectMs, int readMs, int capMs) {
        int normalized = Math.max(500, configuredConnectMs);
        return Math.min(Math.min(normalized, readMs), capMs);
    }
}
