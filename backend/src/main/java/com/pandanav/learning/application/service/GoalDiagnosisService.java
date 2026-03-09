package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.api.dto.session.GoalDiagnosisRequest;
import com.pandanav.learning.api.dto.session.GoalDiagnosisResponse;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.GoalDiagnosisContext;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoalDiagnosisService {

    private final LlmGateway llmGateway;
    private final LlmProperties llmProperties;
    private final PromptTemplateProvider promptTemplateProvider;
    private final LlmJsonParser llmJsonParser;
    private final PromptOutputValidator promptOutputValidator;

    public GoalDiagnosisService(
        LlmGateway llmGateway,
        LlmProperties llmProperties,
        PromptTemplateProvider promptTemplateProvider,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator
    ) {
        this.llmGateway = llmGateway;
        this.llmProperties = llmProperties;
        this.promptTemplateProvider = promptTemplateProvider;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
    }

    public GoalDiagnosisResponse diagnose(GoalDiagnosisRequest request) {
        if (llmProperties.isEnabled() && llmProperties.isReady()) {
            try {
                return diagnoseByLlm(request);
            } catch (Exception ignore) {
                // fallback to rule-based diagnosis
            }
        }
        return diagnoseByRule(request);
    }

    private GoalDiagnosisResponse diagnoseByLlm(GoalDiagnosisRequest request) {
        LlmPrompt prompt = promptTemplateProvider.buildGoalDiagnosisPrompt(new GoalDiagnosisContext(
            request.courseId(),
            request.chapterId(),
            request.goalText()
        ));

        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());
        if (!(parsed instanceof ObjectNode objectNode)) {
            throw new IllegalStateException("Goal diagnosis output must be JSON object.");
        }

        List<String> errors = promptOutputValidator.validateGoalDiagnosis(objectNode);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Goal diagnosis output invalid: " + String.join("; ", errors));
        }

        GoalDiagnosisResponse.SmartBreakdown breakdown = readSmartBreakdown(objectNode.path("smart_breakdown"));
        return new GoalDiagnosisResponse(
            objectNode.path("goal_score").asInt(),
            breakdown,
            new GoalDiagnosisResponse.SummaryResponse(
                objectNode.path("summary").asText(),
                toStringList(objectNode.path("strengths"), List.of("学习方向明确。")),
                toStringList(objectNode.path("risks"), List.of("成功标准尚不够明确。")),
                objectNode.path("rewritten_goal").asText(request.goalText())
            )
        );
    }

    private GoalDiagnosisResponse diagnoseByRule(GoalDiagnosisRequest request) {
        String goal = request.goalText().trim();

        int specific = containsSpecificSubject(goal) ? 16 : 11;
        int measurable = containsMeasure(goal) ? 16 : 10;
        int achievable = goal.length() <= 120 ? 16 : 11;
        int relevant = 15;
        int timeBound = containsTimeline(goal) ? 16 : 10;

        int score = Math.min(100, specific + measurable + achievable + relevant + timeBound);

        List<String> strengths = List.of("学习主题明确。", "目标动机清晰。");
        List<String> risks = List.of("学习范围可能偏大。", "结果衡量标准还可更具体。");

        String rewritten = "在 7 天内完成 " + request.chapterId()
            + " 章节的 3 次训练并提交总结，训练平均分不低于 80 分。";
        String summary = score >= 75
            ? "目标整体可执行，建议补充更清晰的验收标准。"
            : "目标需要进一步收敛范围，并补充可衡量结果。";

        GoalDiagnosisResponse.SmartBreakdown breakdown = new GoalDiagnosisResponse.SmartBreakdown(
            specific,
            measurable,
            achievable,
            relevant,
            timeBound
        );

        return new GoalDiagnosisResponse(
            score,
            breakdown,
            new GoalDiagnosisResponse.SummaryResponse(summary, strengths, risks, rewritten)
        );
    }

    private GoalDiagnosisResponse.SmartBreakdown readSmartBreakdown(JsonNode node) {
        return new GoalDiagnosisResponse.SmartBreakdown(
            node.path("specific_score").asInt(),
            node.path("measurable_score").asInt(),
            node.path("achievable_score").asInt(),
            node.path("relevant_score").asInt(),
            node.path("time_bound_score").asInt()
        );
    }

    private boolean containsMeasure(String text) {
        return text.matches(".*(\\d+|%|score|分|次|题).*" );
    }

    private boolean containsTimeline(String text) {
        return text.matches(".*(day|week|month|天|周|月).*" );
    }

    private boolean containsSpecificSubject(String text) {
        return text.length() > 8 && !text.equals("学习这个章节");
    }

    private List<String> toStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return values.isEmpty() ? fallback : values;
    }
}
