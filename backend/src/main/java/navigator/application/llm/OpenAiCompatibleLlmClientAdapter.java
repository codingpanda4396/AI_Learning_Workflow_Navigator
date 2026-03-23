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

import java.time.Duration;
import java.util.List;

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
