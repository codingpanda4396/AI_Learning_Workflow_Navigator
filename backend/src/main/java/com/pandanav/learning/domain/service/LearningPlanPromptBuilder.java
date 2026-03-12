package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import org.springframework.stereotype.Component;

@Component
public class LearningPlanPromptBuilder {

    public LlmPrompt build(LearningPlanPlanningContext context, LearningPlanPreview rulePreview) {
        String system = """
            You refine a rule-based personalized learning plan.
            Keep the chosen path fixed.
            Return one JSON object only.
            Do not output markdown.
            """;
        String user = """
            goal_id=%s
            diagnosis_id=%s
            goal_text=%s
            learner_profile=%s
            weak_points=%s
            recent_error_tags=%s
            recent_scores=%s
            candidate_path=%s
            candidate_reasons=%s

            Constraints:
            - Do not invent new path nodes.
            - reasons must sound individualized, not generic.
            - stages must be STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION.
            - task_preview must explain learner action, AI support, and stage purpose.
            - focus list 2-4 items.
            - reason count 2-4 items.
            - estimated minutes per task 4-20.
            JSON keys:
            {
              "headline":"",
              "reasons":[{"type":"","title":"","description":""}],
              "focuses":[""],
              "task_preview":[{"stage":"","title":"","goal":"","learner_action":"","ai_support":"","estimated_minutes":0}]
            }
            """.formatted(
            context.goalId(),
            context.diagnosisId(),
            safe(context.goalText()),
            safe(context.learnerProfileSummary()),
            context.weakPointLabels(),
            context.recentErrorTags(),
            context.recentScores(),
            rulePreview.pathPreview(),
            rulePreview.reasons()
        );
        return new LlmPrompt(
            PromptTemplateKey.LEARNING_PLAN_V1,
            PromptTemplateKey.LEARNING_PLAN_V1.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_V1.promptVersion(),
            LlmInvocationProfile.HEAVY_REASONING_TASK,
            system,
            user,
            "{\"headline\":\"\",\"reasons\":[{\"type\":\"\",\"title\":\"\",\"description\":\"\"}],\"focuses\":[\"\"],\"task_preview\":[{\"stage\":\"STRUCTURE\",\"title\":\"\",\"goal\":\"\",\"learner_action\":\"\",\"ai_support\":\"\",\"estimated_minutes\":8}]}",
            "json_only",
            null,
            500
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value.trim();
    }
}
