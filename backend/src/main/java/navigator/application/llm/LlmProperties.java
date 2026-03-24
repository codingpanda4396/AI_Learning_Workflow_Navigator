package navigator.application.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "navigator.llm")
public class LlmProperties {
    /**
     * Whether to try real provider first.
     * Default is false to keep dev/test zero-dependency.
     */
    private boolean enabled = false;

    /**
     * Provider base URL, e.g. https://api.openai.com or an OpenAI-compatible gateway.
     */
    private String baseUrl = "https://api.openai.com";

    /**
     * Provider API key. Recommended to pass via env var.
     */
    private String apiKey;

    /**
     * Model name for chat completions.
     */
    private String model = "gpt-4.1-mini";

    /**
     * Max tokens for regular tutor chat to keep replies concise and fast.
     */
    private Integer chatMaxTokens = 220;

    /**
     * Max tokens for streaming tutor chat.
     */
    private Integer streamMaxTokens = 220;

    /**
     * Max tokens for structured tutor feedback.
     */
    private Integer feedbackMaxTokens = 180;

    /** HTTP connect timeout in milliseconds. */
    private int connectTimeoutMs = 3000;

    /** HTTP read timeout in milliseconds. */
    private int timeoutMs = 60000;

    /** Shorter HTTP read timeout for structured feedback calls. */
    private int feedbackTimeoutMs = 10000;
}
