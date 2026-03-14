package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        if (!alternativesNode.isMissingNode() && !alternativesNode.isNull()) {
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
        }

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
            benefits,
            nextUnlocks,
            nextStepLabel,
            tasks
        );
    }

    public LearningPlanLlmResult normalize(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        if (result == null) {
            return null;
        }
        LearningPlanSummary fallbackSummary = fallback.summary();
        Map<String, PlanTaskPreview> llmByStage = new LinkedHashMap<>();
        if (result.taskPreview() != null) {
            for (PlanTaskPreview task : result.taskPreview()) {
                if (task == null || !StringUtils.hasText(task.stage())) {
                    continue;
                }
                llmByStage.putIfAbsent(task.stage(), task);
            }
        }

        List<PlanTaskPreview> normalizedTasks = new ArrayList<>();
        for (PlanTaskPreview fallbackTask : fallback.taskPreview()) {
            PlanTaskPreview llmTask = llmByStage.get(fallbackTask.stage());
            normalizedTasks.add(llmTask == null ? fallbackTask : mergeTask(llmTask, fallbackTask));
        }

        return new LearningPlanLlmResult(
            pick(result.headline(), fallbackSummary.headline()),
            pick(result.subtitle(), fallbackSummary.subtitle()),
            pick(result.whyNow(), fallbackSummary.whyNow()),
            normalizeConfidence(pick(result.confidence(), fallbackSummary.confidence())),
            pick(result.currentFocusLabel(), fallbackSummary.currentFocusLabel()),
            pick(result.taskTitle(), fallbackSummary.taskTitle()),
            positiveOrFallback(result.taskEstimatedMinutes(), fallbackSummary.taskEstimatedMinutes()),
            normalizePriority(pick(result.taskPriority(), fallbackSummary.taskPriority())),
            result.reasons() == null || result.reasons().isEmpty() ? fallback.reasons() : result.reasons(),
            result.focuses() == null || result.focuses().isEmpty() ? fallback.focuses() : result.focuses(),
            result.alternatives() == null || result.alternatives().isEmpty() ? fallbackSummary.alternatives() : result.alternatives(),
            result.benefits() == null || result.benefits().isEmpty() ? fallbackSummary.benefits() : result.benefits(),
            result.nextUnlocks() == null || result.nextUnlocks().isEmpty() ? fallbackSummary.nextUnlocks() : result.nextUnlocks(),
            pick(result.nextStepLabel(), fallbackSummary.nextStepLabel()),
            normalizedTasks
        );
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
        if (result.taskPreview().size() > fallback.taskPreview().size()) {
            errors.add("$.task_preview size must be <= fallback size " + fallback.taskPreview().size());
        }
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
        }
        if (result.taskPreview() == null || result.taskPreview().isEmpty()) {
            errors.add("$.task_preview must not be empty");
        } else {
            for (int i = 0; i < result.taskPreview().size(); i++) {
                PlanTaskPreview task = result.taskPreview().get(i);
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
        return errors;
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
