package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanGuidance;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.StrategyComparison;
import com.pandanav.learning.domain.model.StrategyOptionComparison;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LearningPlanResultValidator {

    public LearningPlanLlmResult parse(JsonNode json) {
        List<String> errors = new ArrayList<>();
        requireObject(json, "$", errors);

        String headline = readRequiredText(json, "headline", "$.headline", errors);
        String subtitle = readOptionalText(json, "subtitle");
        String whyNow = readOptionalText(json, "why_now");
        String confidence = normalizeConfidence(readOptionalText(json, "confidence"));
        String currentFocusLabel = readOptionalText(json, "current_focus_label");

        JsonNode currentTaskNode = json.path("current_task");
        String taskTitle = null;
        Integer taskEstimatedMinutes = null;
        String taskPriority = "MEDIUM";
        if (!currentTaskNode.isMissingNode() && !currentTaskNode.isNull()) {
            requireObject(currentTaskNode, "$.current_task", errors);
            taskTitle = readOptionalText(currentTaskNode, "task_title");
            taskEstimatedMinutes = readOptionalInt(currentTaskNode, "estimated_minutes", "$.current_task.estimated_minutes", errors);
            taskPriority = normalizePriority(readOptionalText(currentTaskNode, "priority"));
        }

        List<PlanReason> reasons = new ArrayList<>();
        JsonNode reasonsNode = firstPresentArray(json, "decision_reasons", "reasons");
        requireArray(reasonsNode, "$.decision_reasons", errors);
        if (reasonsNode.isArray()) {
            for (int i = 0; i < reasonsNode.size(); i++) {
                JsonNode item = reasonsNode.get(i);
                requireObject(item, "$.decision_reasons[" + i + "]", errors);
                reasons.add(new PlanReason(
                    normalizeReasonType(readRequiredText(item, "type", "$.decision_reasons[" + i + "].type", errors)),
                    readRequiredText(item, "title", "$.decision_reasons[" + i + "].title", errors),
                    readRequiredText(item, "description", "$.decision_reasons[" + i + "].description", errors)
                ));
            }
        }

        List<String> focuses = readStringArray(json.path("focuses"), "$.focuses", errors);
        List<String> benefits = readStringArray(json.path("benefits"), "$.benefits", errors);
        List<String> nextUnlocks = readStringArray(json.path("next_unlocks"), "$.next_unlocks", errors);
        String nextStepLabel = readOptionalText(json, "next_step_label");

        List<PlanAlternative> alternatives = new ArrayList<>();
        JsonNode alternativesNode = json.path("alternatives");
        requireArray(alternativesNode, "$.alternatives", errors);
        if (alternativesNode.isArray()) {
            for (int i = 0; i < alternativesNode.size(); i++) {
                JsonNode item = alternativesNode.get(i);
                requireObject(item, "$.alternatives[" + i + "]", errors);
                alternatives.add(new PlanAlternative(
                    normalizeStrategy(readRequiredText(item, "strategy", "$.alternatives[" + i + "].strategy", errors)),
                    readRequiredText(item, "label", "$.alternatives[" + i + "].label", errors),
                    readRequiredText(item, "description", "$.alternatives[" + i + "].description", errors),
                    readRequiredText(item, "tradeoff", "$.alternatives[" + i + "].tradeoff", errors)
                ));
            }
        }

        StrategyComparison strategyComparison = parseStrategyComparison(json.path("strategy_comparison"), errors);
        PlanGuidance planGuidance = parsePlanGuidance(json.path("plan_guidance"), errors);

        List<PlanTaskPreview> tasks = new ArrayList<>();
        JsonNode tasksNode = json.path("task_preview");
        requireArray(tasksNode, "$.task_preview", errors);
        if (tasksNode.isArray()) {
            for (int i = 0; i < tasksNode.size(); i++) {
                JsonNode item = tasksNode.get(i);
                requireObject(item, "$.task_preview[" + i + "]", errors);
                String stageValue = readRequiredText(item, "stage", "$.task_preview[" + i + "].stage", errors);
                String normalizedStage = normalizeStage(stageValue, "$.task_preview[" + i + "].stage", errors);
                Integer estimatedMinutes = readRequiredInt(item, "estimated_minutes", "$.task_preview[" + i + "].estimated_minutes", errors);
                tasks.add(new PlanTaskPreview(
                    normalizedStage,
                    readRequiredText(item, "title", "$.task_preview[" + i + "].title", errors),
                    readRequiredText(item, "goal", "$.task_preview[" + i + "].goal", errors),
                    readRequiredText(item, "learner_action", "$.task_preview[" + i + "].learner_action", errors),
                    readRequiredText(item, "ai_support", "$.task_preview[" + i + "].ai_support", errors),
                    estimatedMinutes == null ? 0 : estimatedMinutes
                ));
            }
        }

        if (!errors.isEmpty()) {
            throw new LearningPlanSchemaValidationException(errors);
        }
        return new LearningPlanLlmResult(
            headline,
            subtitle,
            whyNow,
            confidence,
            currentFocusLabel,
            taskTitle,
            taskEstimatedMinutes,
            taskPriority,
            reasons,
            focuses,
            alternatives,
            strategyComparison,
            benefits,
            nextUnlocks,
            nextStepLabel,
            tasks,
            planGuidance
        );
    }

    public LearningPlanLlmResult normalize(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        return result;
    }

    public List<String> validateRawTaskPreview(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        List<String> errors = new ArrayList<>();
        if (result == null || result.taskPreview() == null || result.taskPreview().isEmpty()) {
            errors.add("$.task_preview must not be empty");
            return errors;
        }
        Map<String, Integer> stageCounts = new LinkedHashMap<>();
        for (PlanTaskPreview task : result.taskPreview()) {
            stageCounts.merge(task.stage(), 1, Integer::sum);
        }
        stageCounts.forEach((stage, count) -> {
            if (count > 1) {
                errors.add("$.task_preview duplicate stage " + stage);
            }
        });
        return errors;
    }

    public List<String> validate(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        List<String> errors = new ArrayList<>();
        if (result.headline() == null || result.headline().trim().length() < 12) {
            errors.add("$.headline length must be >= 12");
        }
        if (result.reasons() == null || result.reasons().size() < 3) {
            errors.add("$.decision_reasons must contain at least 3 items");
        } else {
            for (int i = 0; i < result.reasons().size(); i++) {
                PlanReason reason = result.reasons().get(i);
                if (reason.title() == null || reason.title().trim().length() < 4) {
                    errors.add("$.decision_reasons[" + i + "].title length must be >= 4");
                }
                if (reason.description() == null || reason.description().trim().length() < 18) {
                    errors.add("$.decision_reasons[" + i + "].description length must be >= 18");
                }
            }
        }
        if (result.focuses() == null || result.focuses().size() < 2) {
            errors.add("$.focuses must contain at least 2 items");
        }
        if (result.benefits() == null || result.benefits().size() < 2) {
            errors.add("$.benefits must contain at least 2 items");
        }
        if (result.nextUnlocks() == null || result.nextUnlocks().isEmpty()) {
            errors.add("$.next_unlocks must contain at least 1 item");
        }
        if (result.alternatives() == null || result.alternatives().size() < 4) {
            errors.add("$.alternatives must contain at least 4 items");
        } else {
            validateStrategies("$.alternatives", result.alternatives().stream().map(PlanAlternative::strategy).toList(), errors);
        }
        if (result.taskPreview() == null || result.taskPreview().isEmpty()) {
            errors.add("$.task_preview must not be empty");
        } else {
            if (result.taskPreview().size() != 4) {
                errors.add("$.task_preview must contain exactly 4 items");
            }
            List<String> expectedOrder = List.of("STRUCTURE", "UNDERSTANDING", "TRAINING", "REFLECTION");
            for (int i = 0; i < result.taskPreview().size(); i++) {
                PlanTaskPreview task = result.taskPreview().get(i);
                if (i < expectedOrder.size() && !expectedOrder.get(i).equals(task.stage())) {
                    errors.add("$.task_preview[" + i + "].stage must be " + expectedOrder.get(i));
                }
                if (task.estimatedMinutes() == null || task.estimatedMinutes() < 4 || task.estimatedMinutes() > 20) {
                    errors.add("$.task_preview[" + i + "].estimated_minutes must be between 4 and 20");
                }
                if (task.learnerAction() == null || task.learnerAction().trim().length() < 8) {
                    errors.add("$.task_preview[" + i + "].learner_action length must be >= 8");
                }
                if (task.aiSupport() == null || task.aiSupport().trim().length() < 8) {
                    errors.add("$.task_preview[" + i + "].ai_support length must be >= 8");
                }
            }
        }
        validateStrategyComparison(result.strategyComparison(), errors);
        validatePlanGuidance(result.planGuidance(), errors);
        return errors;
    }

    private StrategyComparison parseStrategyComparison(JsonNode node, List<String> errors) {
        requireObject(node, "$.strategy_comparison", errors);
        String currentRecommended = normalizeStrategy(readRequiredText(
            node,
            "current_recommended_strategy",
            "$.strategy_comparison.current_recommended_strategy",
            errors
        ));
        JsonNode optionsNode = node.path("options");
        requireArray(optionsNode, "$.strategy_comparison.options", errors);
        List<StrategyOptionComparison> options = new ArrayList<>();
        if (optionsNode.isArray()) {
            for (int i = 0; i < optionsNode.size(); i++) {
                JsonNode item = optionsNode.get(i);
                requireObject(item, "$.strategy_comparison.options[" + i + "]", errors);
                options.add(new StrategyOptionComparison(
                    normalizeStrategy(readRequiredText(item, "strategy", "$.strategy_comparison.options[" + i + "].strategy", errors)),
                    readRequiredText(item, "label", "$.strategy_comparison.options[" + i + "].label", errors),
                    readRequiredText(item, "suitable_for", "$.strategy_comparison.options[" + i + "].suitable_for", errors),
                    readRequiredText(item, "not_ideal_when", "$.strategy_comparison.options[" + i + "].not_ideal_when", errors),
                    readRequiredText(item, "switching_cost_risk", "$.strategy_comparison.options[" + i + "].switching_cost_risk", errors)
                ));
            }
        }
        return new StrategyComparison(currentRecommended, options);
    }

    private PlanGuidance parsePlanGuidance(JsonNode node, List<String> errors) {
        requireObject(node, "$.plan_guidance", errors);
        List<String> kickoffSteps = readStringArray(node.path("kickoff_steps"), "$.plan_guidance.kickoff_steps", errors);
        return new PlanGuidance(
            readRequiredText(node, "why_chosen", "$.plan_guidance.why_chosen", errors),
            readRequiredText(node, "why_not_alternatives", "$.plan_guidance.why_not_alternatives", errors),
            readRequiredText(node, "learner_mirror", "$.plan_guidance.learner_mirror", errors),
            readRequiredText(node, "first_action", "$.plan_guidance.first_action", errors),
            readRequiredText(node, "first_checkpoint", "$.plan_guidance.first_checkpoint", errors),
            readRequiredText(node, "plan_tradeoff", "$.plan_guidance.plan_tradeoff", errors),
            readRequiredText(node, "if_perform_well", "$.plan_guidance.if_perform_well", errors),
            readRequiredText(node, "if_still_struggle", "$.plan_guidance.if_still_struggle", errors),
            readRequiredText(node, "if_no_time", "$.plan_guidance.if_no_time", errors),
            readRequiredText(node, "start_prompt", "$.plan_guidance.start_prompt", errors),
            kickoffSteps,
            readRequiredText(node, "warmup_goal", "$.plan_guidance.warmup_goal", errors),
            readRequiredText(node, "validation_focus", "$.plan_guidance.validation_focus", errors),
            readRequiredText(node, "evidence_mode", "$.plan_guidance.evidence_mode", errors),
            readRequiredText(node, "adaptation_policy", "$.plan_guidance.adaptation_policy", errors),
            readRequiredText(node, "confidence_explanation", "$.plan_guidance.confidence_explanation", errors)
        );
    }

    private void validateStrategyComparison(StrategyComparison strategyComparison, List<String> errors) {
        if (strategyComparison == null) {
            errors.add("$.strategy_comparison is required");
            return;
        }
        if (!StringUtils.hasText(strategyComparison.currentRecommendedStrategy())) {
            errors.add("$.strategy_comparison.current_recommended_strategy is required");
        }
        if (strategyComparison.options() == null || strategyComparison.options().size() < 4) {
            errors.add("$.strategy_comparison.options must contain at least 4 items");
            return;
        }
        validateStrategies(
            "$.strategy_comparison.options",
            strategyComparison.options().stream().map(StrategyOptionComparison::strategy).toList(),
            errors
        );
        for (int i = 0; i < strategyComparison.options().size(); i++) {
            StrategyOptionComparison option = strategyComparison.options().get(i);
            if (!StringUtils.hasText(option.suitableFor()) || option.suitableFor().trim().length() < 6) {
                errors.add("$.strategy_comparison.options[" + i + "].suitable_for length must be >= 6");
            }
            if (!StringUtils.hasText(option.notIdealWhen()) || option.notIdealWhen().trim().length() < 6) {
                errors.add("$.strategy_comparison.options[" + i + "].not_ideal_when length must be >= 6");
            }
            if (!StringUtils.hasText(option.switchingCostRisk()) || option.switchingCostRisk().trim().length() < 6) {
                errors.add("$.strategy_comparison.options[" + i + "].switching_cost_risk length must be >= 6");
            }
        }
    }

    private void validatePlanGuidance(PlanGuidance planGuidance, List<String> errors) {
        if (planGuidance == null) {
            errors.add("$.plan_guidance is required");
            return;
        }
        requireMinLength(planGuidance.whyChosen(), "$.plan_guidance.why_chosen", 12, errors);
        requireMinLength(planGuidance.whyNotAlternatives(), "$.plan_guidance.why_not_alternatives", 12, errors);
        requireMinLength(planGuidance.learnerMirror(), "$.plan_guidance.learner_mirror", 10, errors);
        requireMinLength(planGuidance.firstAction(), "$.plan_guidance.first_action", 8, errors);
        requireMinLength(planGuidance.firstCheckpoint(), "$.plan_guidance.first_checkpoint", 8, errors);
        requireMinLength(planGuidance.planTradeoff(), "$.plan_guidance.plan_tradeoff", 10, errors);
        requireMinLength(planGuidance.ifPerformWell(), "$.plan_guidance.if_perform_well", 8, errors);
        requireMinLength(planGuidance.ifStillStruggle(), "$.plan_guidance.if_still_struggle", 8, errors);
        requireMinLength(planGuidance.ifNoTime(), "$.plan_guidance.if_no_time", 8, errors);
        requireMinLength(planGuidance.startPrompt(), "$.plan_guidance.start_prompt", 6, errors);
        requireMinLength(planGuidance.warmupGoal(), "$.plan_guidance.warmup_goal", 6, errors);
        requireMinLength(planGuidance.validationFocus(), "$.plan_guidance.validation_focus", 6, errors);
        requireMinLength(planGuidance.evidenceMode(), "$.plan_guidance.evidence_mode", 6, errors);
        requireMinLength(planGuidance.adaptationPolicy(), "$.plan_guidance.adaptation_policy", 8, errors);
        requireMinLength(planGuidance.confidenceExplanation(), "$.plan_guidance.confidence_explanation", 8, errors);
        if (planGuidance.kickoffSteps() == null || planGuidance.kickoffSteps().size() < 2 || planGuidance.kickoffSteps().size() > 4) {
            errors.add("$.plan_guidance.kickoff_steps size must be between 2 and 4");
            return;
        }
        for (int i = 0; i < planGuidance.kickoffSteps().size(); i++) {
            String step = planGuidance.kickoffSteps().get(i);
            if (!StringUtils.hasText(step) || step.trim().length() < 6) {
                errors.add("$.plan_guidance.kickoff_steps[" + i + "] length must be >= 6");
            }
        }
    }

    private void validateStrategies(String path, List<String> strategies, List<String> errors) {
        Set<String> expected = Set.of("FAST_TRACK", "FOUNDATION_FIRST", "PRACTICE_FIRST", "COMPRESSED_10_MIN");
        Set<String> actual = new HashSet<>(strategies);
        if (!actual.containsAll(expected)) {
            errors.add(path + " must include FAST_TRACK, FOUNDATION_FIRST, PRACTICE_FIRST, COMPRESSED_10_MIN");
        }
    }

    private void requireMinLength(String value, String path, int minLength, List<String> errors) {
        if (!StringUtils.hasText(value) || value.trim().length() < minLength) {
            errors.add(path + " length must be >= " + minLength);
        }
    }

    private JsonNode firstPresentArray(JsonNode root, String primary, String fallbackField) {
        JsonNode primaryNode = root.path(primary);
        return primaryNode.isMissingNode() ? root.path(fallbackField) : primaryNode;
    }

    private List<String> readStringArray(JsonNode node, String path, List<String> errors) {
        List<String> values = new ArrayList<>();
        requireArray(node, path, errors);
        if (!node.isArray()) {
            return values;
        }
        for (int i = 0; i < node.size(); i++) {
            JsonNode item = node.get(i);
            if (!item.isTextual()) {
                errors.add(path + "[" + i + "] must be string");
                continue;
            }
            values.add(item.asText(""));
        }
        return values;
    }

    private PlanTaskPreview mergeTask(PlanTaskPreview llmTask, PlanTaskPreview fallbackTask) {
        return new PlanTaskPreview(
            fallbackTask.stage(),
            pick(llmTask.title(), fallbackTask.title()),
            pick(llmTask.goal(), fallbackTask.goal()),
            pick(llmTask.learnerAction(), fallbackTask.learnerAction()),
            pick(llmTask.aiSupport(), fallbackTask.aiSupport()),
            positiveOrFallback(llmTask.estimatedMinutes(), fallbackTask.estimatedMinutes())
        );
    }

    private String pick(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private Integer positiveOrFallback(Integer primary, Integer fallback) {
        return primary == null || primary <= 0 ? fallback : primary;
    }

    private void requireObject(JsonNode node, String path, List<String> errors) {
        if (!node.isObject()) {
            errors.add(path + " must be object");
        }
    }

    private void requireArray(JsonNode node, String path, List<String> errors) {
        if (node.isMissingNode()) {
            errors.add(path + " is required");
        } else if (!node.isArray()) {
            errors.add(path + " must be array");
        }
    }

    private String readRequiredText(JsonNode node, String field, String path, List<String> errors) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            errors.add(path + " is required");
            return "";
        }
        if (!value.isTextual()) {
            errors.add(path + " must be string");
            return "";
        }
        return value.asText("");
    }

    private String readOptionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText("") : null;
    }

    private Integer readRequiredInt(JsonNode node, String field, String path, List<String> errors) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            errors.add(path + " is required");
            return null;
        }
        if (!value.isIntegralNumber()) {
            errors.add(path + " must be integer");
            return null;
        }
        return value.asInt();
    }

    private Integer readOptionalInt(JsonNode node, String field, String path, List<String> errors) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            errors.add(path + " must be integer");
            return null;
        }
        return value.asInt();
    }

    private String normalizeStage(String value, String path, List<String> errors) {
        try {
            return LearningPlanStageMapper.normalize(value).name();
        } catch (Exception ex) {
            errors.add(path + " invalid enum value: " + value);
            return "STRUCTURE";
        }
    }

    private String normalizeReasonType(String value) {
        if (!StringUtils.hasText(value)) {
            return "WEAKNESS_MATCH";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "WEAKNESS_MATCH", "DEPENDENCY", "EFFICIENCY", "RISK_CONTROL" -> normalized;
            default -> "WEAKNESS_MATCH";
        };
    }

    private String normalizeStrategy(String value) {
        if (!StringUtils.hasText(value)) {
            return "FOUNDATION_FIRST";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FAST_TRACK", "FOUNDATION_FIRST", "PRACTICE_FIRST", "COMPRESSED_10_MIN" -> normalized;
            default -> "FOUNDATION_FIRST";
        };
    }

    private String normalizeConfidence(String value) {
        if (!StringUtils.hasText(value)) {
            return "MEDIUM";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "MEDIUM";
        };
    }

    private String normalizePriority(String value) {
        if (!StringUtils.hasText(value)) {
            return "MEDIUM";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "MEDIUM";
        };
    }
}
