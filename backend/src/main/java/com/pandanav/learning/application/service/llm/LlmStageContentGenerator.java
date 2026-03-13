package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageContent;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmStageContentGenerator implements StageContentGenerator {

    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;
    private final PromptOutputValidator promptOutputValidator;

    public LlmStageContentGenerator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
    }

    @Override
    public StageContent generate(StageGenerationContext context) {
        PromptTemplateKey key = mapKey(context.stage());
        LlmPrompt prompt = promptTemplateProvider.buildStagePrompt(key, context);
        LlmTextResult result = llmGateway.generate(LlmStage.TASK_RUN, prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());
        PromptOutputValidator.SanitizedStageOutput sanitized = promptOutputValidator.sanitizeStage(context.stage(), parsed);

        List<String> errors = sanitized.errors();
        if (!errors.isEmpty()) {
            throw new InternalServerException("Invalid stage prompt output: " + String.join("; ", errors));
        }

        return new StageContent(
            context.stage(),
            sanitized.node(),
            "LLM",
            prompt.promptKey(),
            prompt.promptVersion(),
            prompt.invocationProfile(),
            result.provider(),
            result.model(),
            result.usage(),
            true,
            true,
            sanitized.truncated() || (result.usage() != null && result.usage().truncated()),
            result.requestPayload(),
            result.responsePayload()
        );
    }

    private PromptTemplateKey mapKey(Stage stage) {
        return switch (stage) {
            case STRUCTURE -> PromptTemplateKey.STRUCTURE_V1;
            case UNDERSTANDING -> PromptTemplateKey.UNDERSTANDING_V1;
            case TRAINING -> PromptTemplateKey.TRAINING_V1;
            case REFLECTION -> PromptTemplateKey.REFLECTION_V1;
        };
    }
}
