package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
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
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());

        List<String> errors = promptOutputValidator.validateStage(context.stage(), parsed);
        if (!errors.isEmpty()) {
            throw new InternalServerException("Invalid stage prompt output: " + String.join("; ", errors));
        }

        return new StageContent(
            context.stage(),
            parsed,
            "LLM",
            prompt.promptKey(),
            prompt.promptVersion(),
            result.provider(),
            result.model(),
            result.usage(),
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
