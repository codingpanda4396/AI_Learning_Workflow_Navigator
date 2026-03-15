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
            你是学习诊断规划助手，从候选题目中选出最合适的诊断题。

            硬性约束：
            1. 只能从提供的候选题目中选择，不得新增题目。
            2. 所选题目须覆盖必选维度，并尽量体现学习者起点。
            3. 所有自然语言输出必须为简体中文，禁止输出英文的 explanation、label、summary。
            4. selectionReasons：每个 questionId 对应一句简短中文说明，8～16 个中文字符，产品化表述（例如：确认图论基础、定位最模糊环节、对齐学习目标、控制学习节奏）。
            5. learnerSummary：仅总结当前已提供的证据（如目标、章节/主题）；不得断言用户尚未回答的内容（如“中等时间”“基础阶段”“目标明确”等）。只可写“本次将确认……”之类，不可写“用户已具备……”。若 profile 中仅有 goal 与 topic，则 summary 只概括这两点并说明本次将确认哪些维度。

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

            仅返回 JSON，不要 markdown 或解释。结构示例（所有中文字段请按上述要求用简体中文填写）：
            {
              "strategyCode": "FOUNDATION_FIRST",
              "selectedQuestionIds": ["q_foundation", "q_goal_style", "q_time_budget", "q_topic_focus", "q_learning_preference"],
              "questionOrder": { "q_foundation": 1, "q_goal_style": 2, "q_time_budget": 3, "q_topic_focus": 4, "q_learning_preference": 5 },
              "selectionReasons": { "q_foundation": "确认图论基础", "q_goal_style": "对齐学习目标", "q_time_budget": "控制学习节奏", "q_topic_focus": "定位最模糊环节", "q_learning_preference": "定制内容交付方式" },
              "suppressedQuestionIds": [],
              "learnerSummary": "当前已知目标与章节，本次将确认前置基础、时间投入与具体模糊点。"
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
