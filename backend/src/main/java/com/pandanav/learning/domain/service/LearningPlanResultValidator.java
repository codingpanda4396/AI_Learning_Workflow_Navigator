package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.model.LearningPlanPreview;
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

        List<PlanReason> reasons = new ArrayList<>();
        JsonNode reasonsNode = json.path("reasons");
        requireArray(reasonsNode, "$.reasons", errors);
        if (reasonsNode.isArray()) {
            for (int i = 0; i < reasonsNode.size(); i++) {
                JsonNode item = reasonsNode.get(i);
                requireObject(item, "$.reasons[" + i + "]", errors);
                reasons.add(new PlanReason(
                    readRequiredText(item, "type", "$.reasons[" + i + "].type", errors),
                    readRequiredText(item, "title", "$.reasons[" + i + "].title", errors),
                    readRequiredText(item, "description", "$.reasons[" + i + "].description", errors)
                ));
            }
        }

        List<String> focuses = new ArrayList<>();
        JsonNode focusesNode = json.path("focuses");
        requireArray(focusesNode, "$.focuses", errors);
        if (focusesNode.isArray()) {
            for (int i = 0; i < focusesNode.size(); i++) {
                JsonNode item = focusesNode.get(i);
                if (!item.isTextual()) {
                    errors.add("$.focuses[" + i + "] must be string");
                    continue;
                }
                focuses.add(item.asText(""));
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
        return new LearningPlanLlmResult(headline, reasons, focuses, tasks);
    }

    public LearningPlanLlmResult normalize(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        if (result == null || result.taskPreview() == null || result.taskPreview().isEmpty()) {
            return result;
        }

        Map<String, PlanTaskPreview> llmByStage = new LinkedHashMap<>();
        for (PlanTaskPreview task : result.taskPreview()) {
            if (task == null || !StringUtils.hasText(task.stage())) {
                continue;
            }
            llmByStage.putIfAbsent(task.stage(), task);
        }

        List<PlanTaskPreview> normalizedTasks = new ArrayList<>();
        for (PlanTaskPreview fallbackTask : fallback.taskPreview()) {
            PlanTaskPreview llmTask = llmByStage.get(fallbackTask.stage());
            normalizedTasks.add(llmTask == null ? fallbackTask : mergeTask(llmTask, fallbackTask));
        }

        return new LearningPlanLlmResult(
            result.headline(),
            result.reasons(),
            result.focuses(),
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
        if (result.reasons() == null || result.reasons().isEmpty()) {
            errors.add("$.reasons must not be empty");
        } else {
            for (int i = 0; i < result.reasons().size(); i++) {
                PlanReason reason = result.reasons().get(i);
                if (reason.title() == null || reason.title().trim().length() < 4) {
                    errors.add("$.reasons[" + i + "].title length must be >= 4");
                }
                if (reason.description() == null || reason.description().trim().length() < 18) {
                    errors.add("$.reasons[" + i + "].description length must be >= 18");
                }
            }
        }
        if (result.focuses() == null || result.focuses().size() < 2) {
            errors.add("$.focuses must contain at least 2 items");
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

    private PlanTaskPreview mergeTask(PlanTaskPreview llmTask, PlanTaskPreview fallbackTask) {
        return new PlanTaskPreview(
            fallbackTask.stage(),
            pick(llmTask.title(), fallbackTask.title()),
            pick(llmTask.goal(), fallbackTask.goal()),
            pick(llmTask.learnerAction(), fallbackTask.learnerAction()),
            pick(llmTask.aiSupport(), fallbackTask.aiSupport()),
            llmTask.estimatedMinutes() == null || llmTask.estimatedMinutes() <= 0
                ? fallbackTask.estimatedMinutes()
                : llmTask.estimatedMinutes()
        );
    }

    private String pick(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
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

    private String normalizeStage(String value, String path, List<String> errors) {
        try {
            return LearningPlanStageMapper.normalize(value).name();
        } catch (Exception ex) {
            errors.add(path + " invalid enum value: " + value);
            return "STRUCTURE";
        }
    }
}
