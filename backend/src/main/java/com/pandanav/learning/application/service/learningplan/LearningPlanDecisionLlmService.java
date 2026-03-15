package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.AlternativeExplanation;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.service.LearningPlanDecisionPromptBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LearningPlanDecisionLlmService {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanDecisionLlmService.class);

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;
    private final LearningPlanDecisionPromptBuilder promptBuilder;
    private final LlmProperties llmProperties;

    public LearningPlanDecisionLlmService(
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LearningPlanDecisionPromptBuilder promptBuilder,
        LlmProperties llmProperties
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.promptBuilder = promptBuilder;
        this.llmProperties = llmProperties;
    }

    public Optional<LlmPlanDecisionResult> decide(LearnerState learnerState, PlanCandidateSet candidateSet) {
        if (learnerState == null || candidateSet == null) {
            return Optional.empty();
        }
        if (candidateSet.entries() == null || candidateSet.entries().isEmpty()) {
            return Optional.empty();
        }
        if (!llmProperties.isEnabled() || !llmProperties.isReady()) {
            if (isDecisionStrictMode()) {
                throw new AiGenerationException("LEARNING_PLAN_DECISION", "LLM_NOT_READY");
            }
            return Optional.empty();
        }
        try {
            LlmPrompt prompt = promptBuilder.build(learnerState, candidateSet);
            LlmTextResult raw = llmGateway.generate(LlmStage.LEARNING_PLAN, prompt);
            if (isTruncated(raw)) {
                return onDecisionFailure("OUTPUT_TRUNCATED", "Learning plan decision LLM output truncated, skip llm decision.");
            }
            JsonNode json = llmJsonParser.parse(raw.text());
            return Optional.of(parseDecision(json));
        } catch (Exception ex) {
            String reason = ex instanceof IllegalArgumentException ? "MISSING_REQUIRED_FIELDS" : "API_ERROR";
            return onDecisionFailure(reason, "Learning plan decision LLM call failed: " + ex.getMessage());
        }
    }

    private Optional<LlmPlanDecisionResult> onDecisionFailure(String reason, String logMessage) {
        log.warn(logMessage);
        if (isDecisionStrictMode()) {
            throw new AiGenerationException("LEARNING_PLAN_DECISION", reason);
        }
        return Optional.empty();
    }

    private LlmPlanDecisionResult parseDecision(JsonNode root) {
        String selectedConceptId = readRequiredText(root, "selectedConceptId");
        String selectedStrategyCode = readRequiredText(root, "selectedStrategyCode");
        String selectedIntensityCode = readRequiredText(root, "selectedIntensityCode");
        String heroReason = readRequiredText(root, "heroReason");
        String currentStateSummary = readRequiredText(root, "currentStateSummary");
        List<String> evidenceBullets = readStringArray(root.path("evidenceBullets"));
        List<AlternativeExplanation> alternatives = readAlternatives(root.path("alternativeExplanations"));
        List<String> nextActions = readStringArray(root.path("nextActions"));
        return new LlmPlanDecisionResult(
            selectedConceptId,
            selectedStrategyCode,
            selectedIntensityCode,
            heroReason,
            currentStateSummary,
            evidenceBullets,
            alternatives,
            nextActions
        );
    }

    private List<AlternativeExplanation> readAlternatives(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<AlternativeExplanation> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(new AlternativeExplanation(
                readRequiredText(item, "strategyCode"),
                readRequiredText(item, "label"),
                readRequiredText(item, "reason"),
                readRequiredText(item, "tradeoff")
            ));
        }
        return result;
    }

    private List<String> readStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        }
        return result;
    }

    private String readRequiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Field `" + field + "` is required.");
        }
        return value.asText().trim();
    }

    private boolean isTruncated(LlmTextResult result) {
        if (result == null || result.usage() == null) {
            return false;
        }
        return result.usage().truncated() || "length".equalsIgnoreCase(result.usage().finishReason());
    }

    private boolean isDecisionStrictMode() {
        return llmProperties.getFailurePolicy() != null
            && llmProperties.getFailurePolicy().isLearningPlanDecisionStrict();
    }
}
