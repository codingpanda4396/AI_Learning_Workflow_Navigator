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
            You are a learning coach. Evaluate learning goals for clarity and executability.
            Return strict JSON only.
            """;
        String userPrompt = """
            Evaluate this goal and return JSON with fields:
            goal_score (0-100),
            summary,
            strengths (string array, 2-3 items),
            risks (string array, 2-3 items),
            rewritten_goal (single concise sentence)
            Context:
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
        String summary = parsed.path("summary").asText("Goal is usable but can be sharpened.");
        List<String> strengths = toStringList(parsed.path("strengths"), List.of("Learning intent is clear."));
        List<String> risks = toStringList(parsed.path("risks"), List.of("Success criteria are not explicit."));
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

        List<String> strengths = List.of(
            "Topic direction is stated.",
            "Learning motivation is explicit."
        );
        List<String> risks = List.of(
            "Scope may still be broad.",
            "Outcome criteria are not fully quantified."
        );
        String rewritten = "Within 7 days, master " + request.chapterId()
            + " in " + request.courseId()
            + ", and complete 3 practice tasks with score >= 80.";
        String summary = score >= 75
            ? "Goal is actionable with minor refinement."
            : "Goal needs clearer scope and measurable outcome.";
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

