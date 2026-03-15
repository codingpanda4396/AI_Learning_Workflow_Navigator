package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisLlmSelectionResult;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds DiagnosisQuestionDraft list from LLM selection result and candidate pool.
 */
@Component
public class DiagnosisQuestionDraftFromSelectionFactory {

    public List<DiagnosisQuestionDraft> fromSelection(
        DiagnosisLlmSelectionResult selection,
        List<DiagnosisQuestionCandidate> candidates
    ) {
        if (selection == null || selection.selectedQuestionIds().isEmpty() || candidates == null) {
            return List.of();
        }
        Map<String, DiagnosisQuestionCandidate> byId = candidates.stream()
            .collect(Collectors.toMap(DiagnosisQuestionCandidate::questionId, c -> c, (a, b) -> a));
        Map<String, Integer> order = selection.questionOrder();
        List<String> selectedIds = selection.selectedQuestionIds();
        List<String> orderedIds = order != null && !order.isEmpty()
            ? selectedIds.stream()
                .sorted(Comparator.comparingInt(id -> order.getOrDefault(id, 999)))
                .toList()
            : selectedIds;
        List<DiagnosisQuestionDraft> drafts = new ArrayList<>();
        Map<String, String> reasons = selection.selectionReasons() != null ? selection.selectionReasons() : Map.of();
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            DiagnosisQuestionCandidate c = byId.get(id);
            if (c == null) continue;
            String reason = reasons.getOrDefault(id, "个性化选题");
            drafts.add(new DiagnosisQuestionDraft(
                c.content(),
                reason,
                i + 1,
                c.dimension()
            ));
        }
        return drafts;
    }
}
