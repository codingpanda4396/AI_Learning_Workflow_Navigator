package navigator.application.llm;

import navigator.infrastructure.llm.LlmClient;
import org.springframework.stereotype.Component;

/**
 * Minimal OpenAI-compatible implementation.
 * Delegates to {@link LlmClient}（长超时）以保持与 R0003 技术出口一致。
 */
@Component
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final LlmClient llmClient;

    public OpenAiCompatibleLlmGateway(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String generateReply(String systemHint, String userContent) {
        return llmClient.chat(systemHint, userContent);
    }
}
