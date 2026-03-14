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
            Return exactly one JSON object and nothing else.
            Do not output markdown code fences.
            Do not output prefaces, explanations, notes, or trailing text.
            Every required field must be present.
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
            - Only output one JSON object.
            - No markdown fences.
            - No prose before or after JSON.
            - task_preview must contain exactly 4 items.
            - task_preview stages must appear exactly once each in this order: STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION.
            - reasons must sound individualized, not generic.
            - stages must be one of: STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION.
            - task_preview must explain learner action, AI support, and stage purpose.
            - focus list 2-4 items.
            - reason count 2-4 items.
            - estimated minutes per task 4-20.
            Required JSON schema:
            {
              "headline":"string, min length 12",
              "reasons":[
                {
                  "type":"string, required",
                  "title":"string, min length 4",
                  "description":"string, min length 18"
                }
              ],
              "focuses":["string"],
              "task_preview":[
                {
                  "stage":"enum STRUCTURE|UNDERSTANDING|TRAINING|REFLECTION",
                  "title":"string, required",
                  "goal":"string, required",
                  "learner_action":"string, min length 8",
                  "ai_support":"string, min length 8",
                  "estimated_minutes":"integer 4-20"
                }
              ]
            }
            task_preview length must equal the candidate task count exactly.
            Output all 4 stages even if some stage descriptions are brief.
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
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"headline\":\"\",\"reasons\":[{\"type\":\"\",\"title\":\"\",\"description\":\"\"}],\"focuses\":[\"\"],\"task_preview\":[{\"stage\":\"STRUCTURE\",\"title\":\"\",\"goal\":\"\",\"learner_action\":\"\",\"ai_support\":\"\",\"estimated_minutes\":8}]}",
            "json_only",
            null,
            640
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value.trim();
    }
}
