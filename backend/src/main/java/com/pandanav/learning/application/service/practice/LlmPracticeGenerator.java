package com.pandanav.learning.application.service.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.PracticeGenerationContext;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmPracticeGenerator implements PracticeGenerator {

    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;
    private final PromptOutputValidator promptOutputValidator;
    private final LlmProperties llmProperties;

    public LlmPracticeGenerator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator,
        LlmProperties llmProperties
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
        this.llmProperties = llmProperties;
    }

    @Override
    public PracticeGeneratorResult generate(PracticeGeneratorRequest request) {
        LlmPrompt prompt = promptTemplateProvider.buildPracticeGenerationPrompt(new PracticeGenerationContext(
            request.taskId(),
            request.sessionId(),
            request.nodeId(),
            request.nodeTitle(),
            request.taskObjective(),
            request.stageContentJson()
        ));

        LlmTextResult result = llmGateway.generate(prompt);
        Integer completionTokens = result.usage() == null ? null : result.usage().tokenOutput();
        Integer threshold = llmProperties.resolveProfile(LlmInvocationProfile.LIGHT_JSON_TASK, prompt.promptKey())
            .completionWarningThreshold();
        if (completionTokens != null && threshold != null && completionTokens > threshold) {
            throw new InternalServerException("practice generation completion tokens exceed threshold");
        }
        JsonNode parsed = llmJsonParser.parse(result.text());

        List<String> errors = promptOutputValidator.validatePracticeGeneration(parsed);
        if (!errors.isEmpty()) {
            throw new InternalServerException("Invalid practice generation output: " + String.join("; ", errors));
        }

        List<PracticeDraftItem> items = new ArrayList<>();
        for (JsonNode item : parsed.path("items")) {
            List<String> options = new ArrayList<>();
            for (JsonNode option : item.path("options")) {
                options.add(option.asText());
            }
            items.add(new PracticeDraftItem(
                mapType(item.path("question_type").asText()),
                item.path("stem").asText(),
                options,
                item.path("standard_answer").asText(),
                item.path("explanation").asText(),
                item.path("difficulty").asText()
            ));
        }

        return new PracticeGeneratorResult(
            items,
            "LLM",
            false,
            true,
            prompt.promptVersion(),
            result.provider(),
            result.model(),
            result.usage() == null ? null : result.usage().tokenInput(),
            result.usage() == null ? null : result.usage().tokenOutput(),
            result.usage() == null ? null : result.usage().latencyMs()
        );
    }

    private PracticeQuestionType mapType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InternalServerException("question_type is missing.");
        }
        return PracticeQuestionType.valueOf(raw.trim().toUpperCase());
    }
}
