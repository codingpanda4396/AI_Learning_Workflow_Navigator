package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisLlmSelectionResult;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates LLM selection result against candidate pool and rule strategy.
 * On failure caller should fallback to PersonalizedQuestionSelector.
 */
@Component
public class DiagnosisSelectionValidator {

    /**
     * @return true if selection is valid and safe to use
     */
    public boolean validate(
        DiagnosisLlmSelectionResult selection,
        List<DiagnosisQuestionCandidate> candidates,
        DiagnosisStrategyDecision ruleStrategy
    ) {
        if (selection == null || candidates == null || candidates.isEmpty()) {
            return false;
        }
        Set<String> candidateIds = candidates.stream()
            .map(DiagnosisQuestionCandidate::questionId)
            .collect(Collectors.toSet());
        List<String> selected = selection.selectedQuestionIds();
        if (selected == null || selected.isEmpty()) {
            return false;
        }
        for (String id : selected) {
            if (id == null || id.isBlank() || !candidateIds.contains(id)) {
                return false;
            }
        }
        int targetCount = ruleStrategy != null && ruleStrategy.targetQuestionCount() > 0
            ? ruleStrategy.targetQuestionCount()
            : 5;
        if (selected.size() != targetCount) {
            return false;
        }
        List<String> mandatory = ruleStrategy != null && ruleStrategy.priorityDimensions() != null
            ? ruleStrategy.priorityDimensions()
            : List.of("FOUNDATION", "GOAL_STYLE", "TIME_BUDGET");
        Set<String> selectedDimensions = selected.stream()
            .map(id -> findDimension(id, candidates))
            .filter(d -> d != null && !d.isBlank())
            .collect(Collectors.toSet());
        for (String dim : mandatory) {
            if (dim != null && !dim.isBlank() && !selectedDimensions.contains(dim)) {
                return false;
            }
        }
        Map<String, Integer> order = selection.questionOrder();
        if (order != null && !order.isEmpty()) {
            for (String id : selected) {
                if (!order.containsKey(id)) {
                    return false;
                }
                int o = order.get(id);
                if (o < 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String findDimension(String questionId, List<DiagnosisQuestionCandidate> candidates) {
        for (DiagnosisQuestionCandidate c : candidates) {
            if (questionId.equals(c.questionId())) {
                return c.dimension().name();
            }
        }
        return null;
    }
}
