package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisLlmSelectionResult;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM-based diagnosis strategy refinement and question selection.
 * LLM may only choose from candidate pool; output is validated by DiagnosisSelectionValidator.
 */
@Service
public class DiagnosisStrategySelectionLlmService {

    private static final String PROMPT_KEY = "DIAGNOSIS_LLM_SELECTION";

    private final LlmGateway llmGateway;
    private final com.pandanav.learning.application.service.llm.LlmJsonParser llmJsonParser;
    private final LlmCallLogger llmCallLogger;

    public DiagnosisStrategySelectionLlmService(
        LlmGateway llmGateway,
        com.pandanav.learning.application.service.llm.LlmJsonParser llmJsonParser,
        LlmCallLogger llmCallLogger
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogger = llmCallLogger;
    }

    /**
     * Call LLM to select and order questions from candidates. Caller must validate and fallback on failure.
     */
    public DiagnosisLlmSelectionResult select(
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision ruleStrategy,
        List<DiagnosisQuestionCandidate> candidates,
        String topic,
        String goalText,
        String chapter
    ) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidate pool is empty");
        }
        String candidateQuestionsJson = serializeCandidates(candidates);
        int targetCount = ruleStrategy != null && ruleStrategy.targetQuestionCount() > 0
            ? ruleStrategy.targetQuestionCount()
            : 5;
        List<String> mandatory = ruleStrategy != null && ruleStrategy.priorityDimensions() != null
            ? ruleStrategy.priorityDimensions()
            : List.of("FOUNDATION", "GOAL_STYLE", "TIME_BUDGET");
        List<String> optional = ruleStrategy != null && ruleStrategy.suppressedDimensions() != null
            ? ruleStrategy.suppressedDimensions()
            : List.of();
        String learnerProfile = formatLearnerProfile(profileSnapshot);
        String ruleStrategyText = formatRuleStrategy(ruleStrategy);

        String userPrompt = """
            You are an AI learning diagnosis planner.
            Your task is to select the most appropriate diagnostic questions for a learner.

            Constraints:
            1. You must ONLY select from the provided candidate questions.
            2. Do NOT invent new questions.
            3. Ensure the selected questions match the learner's context.
            4. Prefer questions that reveal the learner's starting point.
            5. Ensure coverage of mandatory dimensions.

            Learner Profile:
            %s

            Learning Context:
            Topic: %s
            Goal: %s
            Chapter: %s

            Rule Strategy Suggestion:
            %s

            Candidate Questions:
            %s

            Target Question Count: %d
            Mandatory dimensions (must cover): %s
            Optional dimensions: %s

            Return JSON only with structure:
            {
              "strategyCode": "...",
              "selectedQuestionIds": [],
              "questionOrder": { "questionId": order },
              "selectionReasons": { "questionId": "..." },
              "suppressedQuestionIds": [],
              "learnerSummary": "..."
            }
            """
            .formatted(
                learnerProfile,
                nullToEmpty(topic),
                nullToEmpty(goalText),
                nullToEmpty(chapter),
                ruleStrategyText,
                candidateQuestionsJson,
                targetCount,
                mandatory,
                optional
            );

        LlmPrompt prompt = new LlmPrompt(
            PromptTemplateKey.DIAGNOSIS_LLM_SELECTION_V1,
            PROMPT_KEY,
            "v1",
            LlmInvocationProfile.HEAVY_REASONING_TASK,
            "You output only valid JSON. No markdown, no explanation.",
            userPrompt,
            "{\"strategyCode\":\"\",\"selectedQuestionIds\":[],\"questionOrder\":{},\"selectionReasons\":{},\"suppressedQuestionIds\":[],\"learnerSummary\":\"\"}",
            "json_only",
            null,
            800
        );

        Instant start = Instant.now();
        LlmCallContext startContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_LLM_SELECTION, null);
        llmCallLogger.logStart(startContext);
        try {
            LlmTextResult result = llmGateway.generate(LlmStage.DIAGNOSIS_LLM_SELECTION, prompt);
            LlmCallContext successContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_LLM_SELECTION, result.model());
            llmCallLogger.logSuccess(
                successContext,
                toMetrics(result, start),
                result.usage() == null ? null : result.usage().finishReason(),
                result.usage() != null && result.usage().truncated()
            );
            JsonNode root = llmJsonParser.parse(
                result.text(),
                successContext.stage().name(),
                successContext.model(),
                successContext.traceId(),
                successContext.requestId()
            );
            return parseSelectionResult(root);
        } catch (LlmJsonParseException e) {
            llmCallLogger.logFailure(
                startContext,
                LlmFailureType.LLM_JSON_PARSE_ERROR,
                e.diagnosticSummary(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw e;
        } catch (Exception e) {
            llmCallLogger.logFailure(
                startContext,
                LlmFailureType.LLM_API_ERROR,
                e.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw e;
        }
    }

    private static LlmCallMetrics toMetrics(LlmTextResult result, Instant start) {
        if (result == null || result.usage() == null) {
            return new LlmCallMetrics(LlmObservabilityHelper.elapsedMs(start), -1, -1, -1);
        }
        return new LlmCallMetrics(
            LlmObservabilityHelper.elapsedMs(start),
            result.usage().tokenInput() == null ? -1 : result.usage().tokenInput(),
            result.usage().tokenOutput() == null ? -1 : result.usage().tokenOutput(),
            result.usage().totalTokens() == null ? -1 : result.usage().totalTokens()
        );
    }

    private static String serializeCandidates(List<DiagnosisQuestionCandidate> candidates) {
        List<Map<String, String>> list = candidates.stream()
            .map(c -> Map.<String, String>of(
                "questionId", c.questionId(),
                "dimension", c.dimension().name(),
                "intent", nullToEmpty(c.intentCode()),
                "description", c.content() != null && c.content().title() != null ? c.content().title() : ""
            ))
            .toList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String formatLearnerProfile(DiagnosisLearnerProfileSnapshot p) {
        if (p == null) return "{}";
        return String.format(
            "learnerStage=%s goalClarity=%s timeConstraint=%s confidenceLevel=%s riskTags=%s historySignals=%s evidence=%s",
            p.learnerStage(),
            p.goalClarity(),
            p.timeConstraint(),
            p.confidenceLevel(),
            p.riskTags(),
            p.historySignals(),
            p.evidence()
        );
    }

    private static String formatRuleStrategy(DiagnosisStrategyDecision s) {
        if (s == null) return "FOUNDATION_FIRST";
        return String.format(
            "strategyCode=%s priorityDimensions=%s targetQuestionCount=%d",
            s.strategyCode(),
            s.priorityDimensions(),
            s.targetQuestionCount()
        );
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static DiagnosisLlmSelectionResult parseSelectionResult(JsonNode root) {
        String strategyCode = root.path("strategyCode").asText("FOUNDATION_FIRST");
        List<String> selectedQuestionIds = new ArrayList<>();
        for (JsonNode n : root.path("selectedQuestionIds")) {
            String id = n.asText(null);
            if (id != null && !id.isBlank()) selectedQuestionIds.add(id);
        }
        Map<String, Integer> questionOrder = new LinkedHashMap<>();
        JsonNode orderNode = root.path("questionOrder");
        if (orderNode.isObject()) {
            orderNode.fields().forEachRemaining(e ->
                questionOrder.put(e.getKey(), e.getValue().asInt(0)));
        }
        Map<String, String> selectionReasons = new LinkedHashMap<>();
        JsonNode reasonsNode = root.path("selectionReasons");
        if (reasonsNode.isObject()) {
            reasonsNode.fields().forEachRemaining(e ->
                selectionReasons.put(e.getKey(), e.getValue().asText("")));
        }
        List<String> suppressedQuestionIds = new ArrayList<>();
        for (JsonNode n : root.path("suppressedQuestionIds")) {
            String id = n.asText(null);
            if (id != null && !id.isBlank()) suppressedQuestionIds.add(id);
        }
        String learnerSummary = root.path("learnerSummary").asText("");
        return new DiagnosisLlmSelectionResult(
            strategyCode,
            selectedQuestionIds,
            questionOrder,
            selectionReasons,
            suppressedQuestionIds,
            learnerSummary
        );
    }
}
