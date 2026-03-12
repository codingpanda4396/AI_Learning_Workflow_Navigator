package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LearningPlanResultValidator {

    public LearningPlanLlmResult parse(JsonNode json) {
        List<PlanReason> reasons = new ArrayList<>();
        for (JsonNode item : json.path("reasons")) {
            reasons.add(new PlanReason(
                item.path("type").asText(""),
                item.path("title").asText(""),
                item.path("description").asText("")
            ));
        }

        List<String> focuses = new ArrayList<>();
        for (JsonNode item : json.path("focuses")) {
            if (item.isTextual()) {
                focuses.add(item.asText(""));
            }
        }

        List<PlanTaskPreview> tasks = new ArrayList<>();
        for (JsonNode item : json.path("task_preview")) {
            tasks.add(new PlanTaskPreview(
                LearningPlanStageMapper.normalize(item.path("stage").asText()).name(),
                item.path("title").asText(""),
                item.path("goal").asText(""),
                item.path("learner_action").asText(""),
                item.path("ai_support").asText(""),
                item.path("estimated_minutes").asInt(0)
            ));
        }
        return new LearningPlanLlmResult(json.path("headline").asText(""), reasons, focuses, tasks);
    }

    public List<String> validate(LearningPlanLlmResult result, LearningPlanPreview fallback) {
        List<String> errors = new ArrayList<>();
        if (result.headline() == null || result.headline().trim().length() < 12) {
            errors.add("headline is too short");
        }
        if (result.reasons() == null || result.reasons().isEmpty()) {
            errors.add("reasons must not be empty");
        } else {
            for (PlanReason reason : result.reasons()) {
                if (reason.title() == null || reason.title().trim().length() < 4) {
                    errors.add("reason title is too short");
                }
                if (reason.description() == null || reason.description().trim().length() < 18) {
                    errors.add("reason description is too short");
                }
            }
        }
        if (result.focuses() == null || result.focuses().size() < 2) {
            errors.add("focuses must contain at least 2 items");
        }
        if (result.taskPreview() == null || result.taskPreview().size() != fallback.taskPreview().size()) {
            errors.add("task_preview size is invalid");
        } else {
            for (PlanTaskPreview task : result.taskPreview()) {
                if (task.estimatedMinutes() == null || task.estimatedMinutes() < 4 || task.estimatedMinutes() > 20) {
                    errors.add("estimated_minutes is out of range");
                }
                if (task.learnerAction() == null || task.learnerAction().trim().length() < 8) {
                    errors.add("learner_action is too short");
                }
                if (task.aiSupport() == null || task.aiSupport().trim().length() < 8) {
                    errors.add("ai_support is too short");
                }
            }
        }
        return errors;
    }
}
