package com.pandanav.learning.application.service.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmProfileConfig;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PracticeGenerationContext;
import com.pandanav.learning.domain.model.LlmCallLog;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
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
    private final LlmCallLogRepository llmCallLogRepository;
    private final ObjectMapper objectMapper;

    public LlmPracticeGenerator(
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator,
        LlmProperties llmProperties,
        LlmCallLogRepository llmCallLogRepository,
        ObjectMapper objectMapper
    ) {
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
        this.llmProperties = llmProperties;
        this.llmCallLogRepository = llmCallLogRepository;
        this.objectMapper = objectMapper;
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
        LlmProfileConfig profile = llmProperties.resolveProfile(prompt.invocationProfile(), prompt.promptKey());
        String model = resolveModel(prompt, profile);

        LlmTextResult result = null;
        try {
            result = llmGateway.generate(LlmStage.PRACTICE_GENERATION, prompt);
            Integer completionTokens = result.usage() == null ? null : result.usage().tokenOutput();
            Integer threshold = llmProperties.resolveProfile(LlmInvocationProfile.LIGHT_JSON_TASK, prompt.promptKey())
                .completionWarningThreshold();
            if (completionTokens != null && threshold != null && completionTokens > threshold) {
                throw new AiGenerationException("PRACTICE_GENERATION", "OUTPUT_TOKEN_LIMIT_EXCEEDED");
            }
            JsonNode parsed = llmJsonParser.parse(result.text());
            List<String> errors = promptOutputValidator.validatePracticeGeneration(parsed);
            if (!errors.isEmpty()) {
                saveAudit("FAILED", result, prompt, parsed, false, false);
                throw new AiGenerationException("PRACTICE_GENERATION", "JSON_SCHEMA_MISMATCH");
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
            validate(items);

            saveAudit("SUCCEEDED", result, prompt, parsed, true, true);
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
        } catch (AiGenerationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                throw new AiGenerationException("PRACTICE_GENERATION", "TIMEOUT");
            }
            if (result == null) {
                llmCallLogRepository.save(new LlmCallLog(
                    null,
                    "PRACTICE_GENERATION",
                    prompt.invocationProfile().name(),
                    profile.provider(),
                    model,
                    prompt.promptKey(),
                    prompt.promptVersion(),
                    null,
                    null,
                    null,
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    llmProperties.isFallbackToRule(),
                    false,
                    false,
                    false
                ));
            }
            throw new AiGenerationException("PRACTICE_GENERATION", "UNKNOWN_ERROR");
        }
    }

    private void saveAudit(String status, LlmTextResult result, LlmPrompt prompt, JsonNode parsed, boolean parseSuccess, boolean schemaValid) {
        llmCallLogRepository.save(new LlmCallLog(
            null,
            "PRACTICE_GENERATION",
            prompt.invocationProfile().name(),
            result.provider(),
            result.model(),
            prompt.promptKey(),
            prompt.promptVersion(),
            toJson(result.requestPayload()),
            toJson(result.responsePayload()),
            parsed == null ? null : toJson(parsed),
            status,
            result.usage() == null ? null : result.usage().latencyMs(),
            result.usage() == null ? null : result.usage().tokenInput(),
            result.usage() == null ? null : result.usage().tokenOutput(),
            result.usage() == null ? null : result.usage().reasoningTokens(),
            result.usage() == null ? null : result.usage().finishReason(),
            result.usage() != null && result.usage().timeout(),
            false,
            parseSuccess,
            schemaValid,
            result.usage() != null && result.usage().truncated()
        ));
    }

    private String resolveModel(LlmPrompt prompt, LlmProfileConfig profile) {
        if (prompt.modelHint() != null && !prompt.modelHint().isBlank()) {
            return prompt.modelHint().trim();
        }
        return profile.model();
    }

    private PracticeQuestionType mapType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiGenerationException("PRACTICE_GENERATION", "MISSING_REQUIRED_FIELDS");
        }
        return PracticeQuestionType.valueOf(raw.trim().toUpperCase());
    }

    private void validate(List<PracticeDraftItem> items) {
        if (items == null || items.isEmpty()) {
            throw new AiGenerationException("PRACTICE_GENERATION", "EMPTY_CONTENT");
        }
        for (PracticeDraftItem item : items) {
            if (item == null
                || item.stem() == null || item.stem().isBlank()
                || item.standardAnswer() == null || item.standardAnswer().isBlank()
                || item.explanation() == null || item.explanation().isBlank()) {
                throw new AiGenerationException("PRACTICE_GENERATION", "MISSING_REQUIRED_FIELDS");
            }
        }
    }

    private boolean isTimeout(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return message.contains("timeout")
            || message.contains("timed out")
            || ex.getClass().getSimpleName().toLowerCase().contains("timeout");
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return null;
        }
    }
}
