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
            You are generating an explainable AI co-decision learning preview.
            Keep rule-based recommendation skeleton fixed and only enhance explanation and guidance layers.
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
            - Do not change recommended_start_node_id or recommended_pace from candidate_summary.
            - Only output one JSON object.
            - No markdown fences.
            - No prose before or after JSON.
            - task_preview must contain exactly 4 items.
            - task_preview stages must appear exactly once each in this order: STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION.
            - decision_reasons must cover why this recommendation is chosen now.
            - alternatives must include FAST_TRACK, FOUNDATION_FIRST, PRACTICE_FIRST, COMPRESSED_10_MIN and stay aligned with candidate_summary alternatives.
            - strategy_comparison.options must include exactly the same 4 strategies.
            - confidence must be one of HIGH, MEDIUM, LOW.
            - decision_reasons.type must be one of WEAKNESS_MATCH, DEPENDENCY, EFFICIENCY, RISK_CONTROL.
            - current_task.priority must be one of HIGH, MEDIUM, LOW.
            - focus list 2-4 items.
            - benefit list 2-4 items.
            - next_unlocks list 1-4 items.
            - decision_reasons count 3-4 items.
            - estimated minutes per task 4-20.
            - plan_guidance.start_prompt must be <= 28 Chinese characters (or <= 18 English words).
            - kickoff_steps size must be 2-4 and each item should be directly actionable.
            - If confidence is LOW, explain it as low-risk warm start with quick adaptation, not model weakness.
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
              "strategy_comparison":{
                "current_recommended_strategy":"FAST_TRACK|FOUNDATION_FIRST|PRACTICE_FIRST|COMPRESSED_10_MIN",
                "options":[
                  {
                    "strategy":"FAST_TRACK|FOUNDATION_FIRST|PRACTICE_FIRST|COMPRESSED_10_MIN",
                    "label":"string",
                    "suitable_for":"string",
                    "not_ideal_when":"string",
                    "switching_cost_risk":"string"
                  }
                ]
              },
              "plan_guidance":{
                "why_chosen":"string",
                "why_not_alternatives":"string",
                "learner_mirror":"string",
                "first_action":"string",
                "first_checkpoint":"string",
                "plan_tradeoff":"string",
                "if_perform_well":"string",
                "if_still_struggle":"string",
                "if_no_time":"string",
                "start_prompt":"string",
                "kickoff_steps":["string"],
                "warmup_goal":"string",
                "validation_focus":"string",
                "evidence_mode":"string",
                "adaptation_policy":"string",
                "confidence_explanation":"string"
              },
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
            PromptTemplateKey.LEARNING_PLAN_V2,
            PromptTemplateKey.LEARNING_PLAN_V2.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_V2.promptVersion(),
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"headline\":\"\",\"subtitle\":\"\",\"why_now\":\"\",\"confidence\":\"MEDIUM\",\"current_focus_label\":\"\",\"current_task\":{\"task_title\":\"\",\"estimated_minutes\":8,\"priority\":\"MEDIUM\"},\"decision_reasons\":[{\"type\":\"WEAKNESS_MATCH\",\"title\":\"\",\"description\":\"\"}],\"alternatives\":[{\"strategy\":\"FAST_TRACK\",\"label\":\"\",\"description\":\"\",\"tradeoff\":\"\"}],\"strategy_comparison\":{\"current_recommended_strategy\":\"FOUNDATION_FIRST\",\"options\":[{\"strategy\":\"FOUNDATION_FIRST\",\"label\":\"\",\"suitable_for\":\"\",\"not_ideal_when\":\"\",\"switching_cost_risk\":\"\"}]},\"plan_guidance\":{\"why_chosen\":\"\",\"why_not_alternatives\":\"\",\"learner_mirror\":\"\",\"first_action\":\"\",\"first_checkpoint\":\"\",\"plan_tradeoff\":\"\",\"if_perform_well\":\"\",\"if_still_struggle\":\"\",\"if_no_time\":\"\",\"start_prompt\":\"\",\"kickoff_steps\":[\"\"],\"warmup_goal\":\"\",\"validation_focus\":\"\",\"evidence_mode\":\"\",\"adaptation_policy\":\"\",\"confidence_explanation\":\"\"},\"focuses\":[\"\"],\"benefits\":[\"\"],\"next_unlocks\":[\"\"],\"next_step_label\":\"\",\"task_preview\":[{\"stage\":\"STRUCTURE\",\"title\":\"\",\"goal\":\"\",\"learner_action\":\"\",\"ai_support\":\"\",\"estimated_minutes\":8}]}",
            "json_only",
            null,
            1800
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(none)" : value.trim();
    }
}
