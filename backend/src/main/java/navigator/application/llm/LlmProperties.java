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

    /** HTTP 读超时（等完整响应）；连接超时在网关内 capped 为 min(本值, 15s)。 */
    private int timeoutMs = 60000;
}

