package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.session.GoalDiagnosisRequest;
import com.pandanav.learning.api.dto.session.GoalDiagnosisResponse;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalDiagnosisService {

    private final LlmGateway llmGateway;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public GoalDiagnosisService(LlmGateway llmGateway, LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
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

    private GoalDiagnosisResponse diagnoseByLlm(GoalDiagnosisRequest request) throws Exception {
        String systemPrompt = """
            你是学习目标诊断助手。
            仅返回 JSON，不要输出 markdown。
            字段值必须是简体中文。
            """;
        String userPrompt = """
            请评估学习目标质量并输出 JSON 字段：
            goal_score（0-100）,
            summary,
            strengths（字符串数组，2-3 条）,
            risks（字符串数组，2-3 条）,
            rewritten_goal（一句话，可执行、可衡量）
            上下文：
            course_id=%s
            chapter_id=%s
            goal_text=%s
            """.formatted(request.courseId(), request.chapterId(), request.goalText());

        LlmTextResult result = llmGateway.generate(new LlmPrompt(
            PromptTemplateKey.EVALUATE_PROMPT_V1,
            "v1",
            systemPrompt,
            userPrompt
        ));
        JsonNode parsed = objectMapper.readTree(result.text());
        int score = parsed.path("goal_score").isNumber() ? parsed.path("goal_score").asInt() : 70;
        String summary = parsed.path("summary").asText("目标可执行，但仍可进一步收敛。");
        List<String> strengths = toStringList(parsed.path("strengths"), List.of("学习方向明确。"));
        List<String> risks = toStringList(parsed.path("risks"), List.of("成功标准尚不够明确。"));
        String rewrittenGoal = parsed.path("rewritten_goal").asText(request.goalText());
        return new GoalDiagnosisResponse(score, new GoalDiagnosisResponse.SummaryResponse(summary, strengths, risks, rewrittenGoal));
    }

    private GoalDiagnosisResponse diagnoseByRule(GoalDiagnosisRequest request) {
        String goal = request.goalText().trim();
        int score = Math.min(95, 45 + Math.min(goal.length(), 80) / 2);
        if (containsMeasure(goal)) {
            score += 10;
        }
        if (containsTimeline(goal)) {
            score += 5;
        }
        score = Math.min(score, 100);

        List<String> strengths = List.of("学习主题明确。", "目标动机清晰。");
        List<String> risks = List.of("学习范围可能偏大。", "结果衡量标准还可更具体。");
        String rewritten = "在 7 天内掌握 " + request.courseId() + " / " + request.chapterId()
            + " 的核心机制，完成 3 道训练题，且得分不低于 80 分。";
        String summary = score >= 75
            ? "目标总体可执行，建议补充更明确的验收标准。"
            : "目标需要进一步收敛范围，并补充可衡量结果。";
        return new GoalDiagnosisResponse(score, new GoalDiagnosisResponse.SummaryResponse(summary, strengths, risks, rewritten));
    }

    private boolean containsMeasure(String text) {
        return text.matches(".*(\\d+|%|score|分|题|次|个).*");
    }

    private boolean containsTimeline(String text) {
        return text.matches(".*(day|week|month|天|周|月).*");
    }

    private List<String> toStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) {
            return fallback;
        }
        List<String> values = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return values.isEmpty() ? fallback : values;
    }
}
