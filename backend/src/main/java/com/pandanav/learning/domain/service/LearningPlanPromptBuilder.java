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
            You are generating an explainable AI learning decision.
            Keep the chosen path fixed and only refine the decision object around it.
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
            requested_strategy=%s
            requested_time_budget_minutes=%s
            adjustment_reason=%s
            user_feedback=%s
            candidate_summary=%s
            candidate_path=%s
            candidate_reasons=%s
            candidate_tasks=%s

            Constraints:
            - Do not invent new path nodes.
            - Only output one JSON object.
            - No markdown fences.
            - No prose before or after JSON.
            - task_preview must contain exactly 4 items.
            - task_preview stages must appear exactly once each in this order: STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION.
            - decision_reasons must cover why this recommendation is chosen now.
            - alternatives must include FAST_TRACK, FOUNDATION_FIRST, PRACTICE_FIRST, COMPRESSED_10_MIN.
            - confidence must be one of HIGH, MEDIUM, LOW.
            - decision_reasons.type must be one of WEAKNESS_MATCH, DEPENDENCY, EFFICIENCY, RISK_CONTROL.
            - current_task.priority must be one of HIGH, MEDIUM, LOW.
            - focus list 2-4 items.
            - benefit list 2-4 items.
            - next_unlocks list 1-4 items.
            - decision_reasons count 3-4 items.
            - estimated minutes per task 4-20.
            - Use concise, front-end friendly wording.
            Required JSON schema:
            {
              "headline":"string",
              "subtitle":"string",
              "why_now":"string",
              "confidence":"HIGH|MEDIUM|LOW",
              "current_focus_label":"string",
              "current_task":{
                "task_title":"string",
                "estimated_minutes":"integer 4-20",
                "priority":"HIGH|MEDIUM|LOW"
              },
              "decision_reasons":[
                {
                  "type":"WEAKNESS_MATCH|DEPENDENCY|EFFICIENCY|RISK_CONTROL",
                  "title":"string",
                  "description":"string"
                }
              ],
              "alternatives":[
                {
                  "strategy":"FAST_TRACK|FOUNDATION_FIRST|PRACTICE_FIRST|COMPRESSED_10_MIN",
                  "label":"string",
                  "description":"string",
                  "tradeoff":"string"
                }
              ],
              "focuses":["string"],
              "benefits":["string"],
              "next_unlocks":["string"],
              "next_step_label":"string",
              "task_preview":[
                {
                  "stage":"STRUCTURE|UNDERSTANDING|TRAINING|REFLECTION",
                  "title":"string",
                  "goal":"string",
                  "learner_action":"string",
                  "ai_support":"string",
                  "estimated_minutes":"integer 4-20"
                }
              ]
            }
            """.formatted(
            context.goalId(),
            safe(context.diagnosisId()),
            safe(context.goalText()),
            safe(context.learnerProfileSummary()),
            context.weakPointLabels(),
            context.recentErrorTags(),
            context.recentScores(),
            safe(context.requestedStrategy()),
            context.requestedTimeBudgetMinutes() == null ? "(none)" : context.requestedTimeBudgetMinutes(),
            safe(context.adjustmentReason()),
            safe(context.userFeedback()),
            rulePreview.summary(),
            rulePreview.pathPreview(),
            rulePreview.reasons(),
            rulePreview.taskPreview()
        );
        return new LlmPrompt(
            PromptTemplateKey.LEARNING_PLAN_V1,
            PromptTemplateKey.LEARNING_PLAN_V1.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_V1.promptVersion(),
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"headline\":\"\",\"subtitle\":\"\",\"why_now\":\"\",\"confidence\":\"MEDIUM\",\"current_focus_label\":\"\",\"current_task\":{\"task_title\":\"\",\"estimated_minutes\":8,\"priority\":\"MEDIUM\"},\"decision_reasons\":[{\"type\":\"WEAKNESS_MATCH\",\"title\":\"\",\"description\":\"\"}],\"alternatives\":[{\"strategy\":\"FAST_TRACK\",\"label\":\"\",\"description\":\"\",\"tradeoff\":\"\"}],\"focuses\":[\"\"],\"benefits\":[\"\"],\"next_unlocks\":[\"\"],\"next_step_label\":\"\",\"task_preview\":[{\"stage\":\"STRUCTURE\",\"title\":\"\",\"goal\":\"\",\"learner_action\":\"\",\"ai_support\":\"\",\"estimated_minutes\":8}]}",
            "json_only",
            null,
            1200
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(none)" : value.trim();
    }
}
