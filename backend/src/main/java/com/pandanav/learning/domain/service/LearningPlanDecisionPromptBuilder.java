package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningPlanDecisionPromptBuilder {

    private static final int MAX_OUTPUT_TOKENS = 900;

    public LlmPrompt build(LearnerState learnerState, PlanCandidateSet candidateSet) {
        String system = """
            You are a constrained decision assistant for learning-plan selection.
            You must only select IDs/codes that already exist in candidate lists.
            Return exactly one JSON object and nothing else.
            Do not output markdown code fences.
            """;
        String user = """
            learner_state:
            - goal_orientation=%s
            - preferred_learning_mode=%s
            - pace_preference=%s
            - current_block_type=%s
            - evidence_level=%s
            - motivation_risk=%s
            - foundation_status=%s
            - practice_readiness=%s
            - concept_code_gap=%s
            - frustration_risk=%s
            - confidence_reason_summary=%s
            - primary_block_description=%s
            - evidence_summaries=%s

            entry_candidates=%s
            strategy_candidates=%s
            intensity_candidates=%s
            action_templates=%s

            hard_constraints:
            - selectedConceptId must be one of entry_candidates.conceptId
            - selectedStrategyCode must be one of strategy_candidates.code
            - selectedIntensityCode must be one of intensity_candidates.code
            - evidenceBullets size 1-3, no duplicates, each <= 48 Chinese chars or <= 20 English words
            - alternativeExplanations size 0-3, strategyCode must be from strategy_candidates.code
            - nextActions must contain exactly 3 concise actionable items
            - all text should be concise and front-end friendly

            Required JSON schema:
            {
              "selectedConceptId":"string",
              "selectedStrategyCode":"string",
              "selectedIntensityCode":"string",
              "heroReason":"string",
              "currentStateSummary":"string",
              "evidenceBullets":["string"],
              "alternativeExplanations":[
                {
                  "strategyCode":"string",
                  "label":"string",
                  "reason":"string",
                  "tradeoff":"string"
                }
              ],
              "nextActions":["string"]
            }
            """.formatted(
            enumName(learnerState.goalOrientation()),
            enumName(learnerState.preferredLearningMode()),
            enumName(learnerState.pacePreference()),
            enumName(learnerState.currentBlockType()),
            enumName(learnerState.evidenceLevel()),
            enumName(learnerState.motivationRisk()),
            enumName(learnerState.foundationStatus()),
            enumName(learnerState.practiceReadiness()),
            enumName(learnerState.conceptCodeGap()),
            enumName(learnerState.frustrationRisk()),
            safe(learnerState.confidenceReasonSummary()),
            safe(learnerState.primaryBlockDescription()),
            safeList(learnerState.evidenceSummaries()),
            formatEntryCandidates(candidateSet.entries()),
            formatStrategyCandidates(candidateSet.strategies()),
            formatIntensityCandidates(candidateSet.intensities()),
            formatActionTemplates(candidateSet.actionTemplates())
        );
        return new LlmPrompt(
            PromptTemplateKey.LEARNING_PLAN_DECISION_V1,
            PromptTemplateKey.LEARNING_PLAN_DECISION_V1.promptKey(),
            PromptTemplateKey.LEARNING_PLAN_DECISION_V1.promptVersion(),
            LlmInvocationProfile.LIGHT_JSON_TASK,
            system,
            user,
            "{\"selectedConceptId\":\"\",\"selectedStrategyCode\":\"\",\"selectedIntensityCode\":\"\",\"heroReason\":\"\",\"currentStateSummary\":\"\",\"evidenceBullets\":[\"\"],\"alternativeExplanations\":[{\"strategyCode\":\"\",\"label\":\"\",\"reason\":\"\",\"tradeoff\":\"\"}],\"nextActions\":[\"\",\"\",\"\"]}",
            "json_only",
            null,
            MAX_OUTPUT_TOKENS
        );
    }

    private String formatEntryCandidates(List<EntryCandidate> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        return entries.stream()
            .map(item -> "{conceptId=%s, conceptName=%s, reason=%s, estimatedMinutes=%s, priority=%s}".formatted(
                safe(item.conceptId()),
                safe(item.conceptName()),
                safe(item.reason()),
                item.estimatedMinutes() == null ? "(none)" : item.estimatedMinutes(),
                safe(item.priority())
            ))
            .toList()
            .toString();
    }

    private String formatStrategyCandidates(List<StrategyCandidate> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            return "[]";
        }
        return strategies.stream()
            .map(item -> "{code=%s, label=%s, description=%s, tradeoff=%s}".formatted(
                safe(item.code()),
                safe(item.label()),
                safe(item.description()),
                safe(item.tradeoff())
            ))
            .toList()
            .toString();
    }

    private String formatIntensityCandidates(List<IntensityCandidate> intensities) {
        if (intensities == null || intensities.isEmpty()) {
            return "[]";
        }
        return intensities.stream()
            .map(item -> "{code=%s, label=%s, estimatedMinutes=%s, rationale=%s}".formatted(
                safe(item.code()),
                safe(item.label()),
                item.estimatedMinutes() == null ? "(none)" : item.estimatedMinutes(),
                safe(item.rationale())
            ))
            .toList()
            .toString();
    }

    private String formatActionTemplates(List<ActionTemplate> actions) {
        if (actions == null || actions.isEmpty()) {
            return "[]";
        }
        return actions.stream()
            .map(item -> "{stage=%s, title=%s, goal=%s, learnerAction=%s, aiSupport=%s, estimatedMinutes=%s}".formatted(
                safe(item.stage()),
                safe(item.title()),
                safe(item.goal()),
                safe(item.learnerAction()),
                safe(item.aiSupport()),
                item.estimatedMinutes() == null ? "(none)" : item.estimatedMinutes()
            ))
            .toList()
            .toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(none)" : value.trim();
    }

    private String safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream().map(this::safe).toList().toString();
    }

    private String enumName(Enum<?> value) {
        return value == null ? "(none)" : value.name();
    }
}
