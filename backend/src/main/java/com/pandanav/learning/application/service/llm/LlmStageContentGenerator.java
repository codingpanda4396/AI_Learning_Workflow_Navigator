package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageContent;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LlmStageContentGenerator implements StageContentGenerator {

    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;

    public LlmStageContentGenerator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
    }

    @Override
    public StageContent generate(StageGenerationContext context) {
        PromptTemplateKey key = mapKey(context.stage());
        LlmPrompt prompt = promptTemplateProvider.buildStagePrompt(key, context);
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());
        validate(parsed, context.stage());
        return new StageContent(
            context.stage(),
            parsed,
            "LLM",
            prompt.promptVersion(),
            result.provider(),
            result.model(),
            result.usage()
        );
    }

    private PromptTemplateKey mapKey(Stage stage) {
        return switch (stage) {
            case STRUCTURE -> PromptTemplateKey.STRUCTURE_PROMPT_V1;
            case UNDERSTANDING -> PromptTemplateKey.UNDERSTANDING_PROMPT_V1;
            case TRAINING -> PromptTemplateKey.TRAINING_PROMPT_V1;
            case REFLECTION -> PromptTemplateKey.REFLECTION_PROMPT_V1;
        };
    }

    private void validate(JsonNode parsed, Stage stage) {
        Set<String> required = switch (stage) {
            case STRUCTURE -> Set.of("title", "summary", "key_points", "common_misconceptions", "suggested_sequence");
            case UNDERSTANDING -> Set.of("concept_explanation", "analogy", "step_by_step_reasoning", "common_errors", "check_questions");
            case TRAINING -> Set.of("questions");
            case REFLECTION -> Set.of("reflection_prompt", "review_checklist", "next_step_suggestion");
        };
        for (String field : required) {
            if (!parsed.has(field)) {
                throw new InternalServerException("LLM stage output missing field: " + field);
            }
        }
    }
}

