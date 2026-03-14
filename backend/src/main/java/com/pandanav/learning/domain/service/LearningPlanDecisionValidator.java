package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DecisionFallbackLevel;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.AlternativeExplanation;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanDecisionValidationResult;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class LearningPlanDecisionValidator {

    public LearningPlanDecisionValidationResult validateAndFallback(
        LearnerState learnerState,
        PlanCandidateSet candidateSet,
        LlmPlanDecisionResult llmDecision,
        LlmPlanDecisionResult defaultDecision
    ) {
        List<String> l3Errors = validateContext(candidateSet, defaultDecision);
        if (!l3Errors.isEmpty()) {
            return new LearningPlanDecisionValidationResult(
                robustStartDecision(candidateSet, defaultDecision),
                DecisionFallbackLevel.L3_ROBUST_START,
                l3Errors
            );
        }

        if (llmDecision == null) {
            return new LearningPlanDecisionValidationResult(
                defaultDecision,
                DecisionFallbackLevel.L2_FULL_DEFAULT,
                List.of("llm_decision_missing")
            );
        }

        List<String> l2Errors = new ArrayList<>();
        l2Errors.addAll(validateStructure(llmDecision));
        l2Errors.addAll(validateCandidateConstraints(llmDecision, candidateSet));
        l2Errors.addAll(validateBusinessConsistency(llmDecision, candidateSet));
        if (!l2Errors.isEmpty()) {
            return new LearningPlanDecisionValidationResult(
                defaultDecision,
                DecisionFallbackLevel.L2_FULL_DEFAULT,
                l2Errors
            );
        }

        List<String> l1Errors = validateCopyQuality(llmDecision);
        if (!l1Errors.isEmpty()) {
            return new LearningPlanDecisionValidationResult(
                repairCopyOnly(llmDecision, defaultDecision),
                DecisionFallbackLevel.L1_COPY_REPAIRED,
                l1Errors
            );
        }

        return new LearningPlanDecisionValidationResult(
            llmDecision,
            DecisionFallbackLevel.NONE,
            List.of()
        );
    }

    private List<String> validateContext(PlanCandidateSet candidateSet, LlmPlanDecisionResult defaultDecision) {
        List<String> errors = new ArrayList<>();
        if (candidateSet == null) {
            errors.add("candidate_set_missing");
            return errors;
        }
        if (candidateSet.entries() == null || candidateSet.entries().isEmpty()) {
            errors.add("entry_candidates_empty");
        }
        if (candidateSet.strategies() == null || candidateSet.strategies().isEmpty()) {
            errors.add("strategy_candidates_empty");
        }
        if (candidateSet.intensities() == null || candidateSet.intensities().isEmpty()) {
            errors.add("intensity_candidates_empty");
        }
        if (defaultDecision == null) {
            errors.add("default_decision_missing");
        }
        return errors;
    }

    private List<String> validateStructure(LlmPlanDecisionResult decision) {
        List<String> errors = new ArrayList<>();
        requireText("selectedConceptId", decision.selectedConceptId(), 2, errors);
        requireText("selectedStrategyCode", decision.selectedStrategyCode(), 2, errors);
        requireText("selectedIntensityCode", decision.selectedIntensityCode(), 2, errors);
        requireText("heroReason", decision.heroReason(), 8, errors);
        requireText("currentStateSummary", decision.currentStateSummary(), 8, errors);

        if (decision.evidenceBullets() == null || decision.evidenceBullets().isEmpty() || decision.evidenceBullets().size() > 3) {
            errors.add("evidenceBullets_size_invalid");
        }
        if (decision.nextActions() == null || decision.nextActions().size() != 3) {
            errors.add("nextActions_size_invalid");
        }
        if (decision.alternativeExplanations() != null && decision.alternativeExplanations().size() > 3) {
            errors.add("alternativeExplanations_size_invalid");
        }
        return errors;
    }

    private List<String> validateCandidateConstraints(LlmPlanDecisionResult decision, PlanCandidateSet candidateSet) {
        List<String> errors = new ArrayList<>();
        Set<String> entryIds = toEntryIds(candidateSet.entries());
        Set<String> strategyCodes = toStrategyCodes(candidateSet.strategies());
        Set<String> intensityCodes = toIntensityCodes(candidateSet.intensities());

        if (!entryIds.contains(normalize(decision.selectedConceptId()))) {
            errors.add("selectedConceptId_out_of_candidates");
        }
        if (!strategyCodes.contains(normalize(decision.selectedStrategyCode()))) {
            errors.add("selectedStrategyCode_out_of_candidates");
        }
        if (!intensityCodes.contains(normalize(decision.selectedIntensityCode()))) {
            errors.add("selectedIntensityCode_out_of_candidates");
        }
        if (decision.alternativeExplanations() != null) {
            for (AlternativeExplanation item : decision.alternativeExplanations()) {
                if (item == null || !strategyCodes.contains(normalize(item.strategyCode()))) {
                    errors.add("alternative_strategy_out_of_candidates");
                    break;
                }
            }
        }
        return errors;
    }

    private List<String> validateBusinessConsistency(LlmPlanDecisionResult decision, PlanCandidateSet candidateSet) {
        List<String> errors = new ArrayList<>();
        EntryCandidate selected = findEntry(candidateSet.entries(), decision.selectedConceptId());
        if (selected == null) {
            errors.add("selected_entry_missing");
            return errors;
        }
        String conceptName = normalizeText(selected.conceptName());
        Set<String> actionHints = new LinkedHashSet<>();
        actionHints.add(conceptName);
        if (candidateSet.actionTemplates() != null) {
            for (ActionTemplate item : candidateSet.actionTemplates()) {
                actionHints.add(normalizeText(item.title()));
                actionHints.add(normalizeText(item.goal()));
            }
        }

        boolean related = decision.nextActions() != null && decision.nextActions().stream()
            .map(this::normalizeText)
            .anyMatch(action -> containsAnyHint(action, actionHints));
        if (!related) {
            errors.add("nextActions_not_related_to_selected_concept");
        }
        return errors;
    }

    private List<String> validateCopyQuality(LlmPlanDecisionResult decision) {
        List<String> errors = new ArrayList<>();
        if (isWeakText(decision.heroReason(), 16)) {
            errors.add("heroReason_too_short_or_empty");
        }
        if (isWeakText(decision.currentStateSummary(), 16)) {
            errors.add("currentStateSummary_too_short_or_empty");
        }
        if (hasDuplicate(decision.evidenceBullets())) {
            errors.add("evidenceBullets_has_duplicates");
        }
        if (hasDuplicate(decision.nextActions())) {
            errors.add("nextActions_has_duplicates");
        }
        if (containsFiller(decision.heroReason()) || containsFiller(decision.currentStateSummary())) {
            errors.add("contains_filler_text");
        }
        return errors;
    }

    private LlmPlanDecisionResult repairCopyOnly(LlmPlanDecisionResult llmDecision, LlmPlanDecisionResult defaultDecision) {
        List<String> evidence = keepDistinct(llmDecision.evidenceBullets(), 3);
        if (evidence.isEmpty()) {
            evidence = keepDistinct(defaultDecision.evidenceBullets(), 3);
        }
        List<String> actions = keepDistinct(llmDecision.nextActions(), 3);
        if (actions.size() < 3) {
            actions = keepDistinct(defaultDecision.nextActions(), 3);
        }
        return new LlmPlanDecisionResult(
            llmDecision.selectedConceptId(),
            llmDecision.selectedStrategyCode(),
            llmDecision.selectedIntensityCode(),
            pickGoodText(llmDecision.heroReason(), defaultDecision.heroReason(), 16),
            pickGoodText(llmDecision.currentStateSummary(), defaultDecision.currentStateSummary(), 16),
            ensureMinSize(evidence, keepDistinct(defaultDecision.evidenceBullets(), 3), 1, 3),
            llmDecision.alternativeExplanations() == null ? List.of() : llmDecision.alternativeExplanations().stream().limit(3).toList(),
            ensureMinSize(actions, keepDistinct(defaultDecision.nextActions(), 3), 3, 3)
        );
    }

    private LlmPlanDecisionResult robustStartDecision(PlanCandidateSet candidateSet, LlmPlanDecisionResult defaultDecision) {
        if (defaultDecision == null) {
            return new LlmPlanDecisionResult(
                firstEntryId(candidateSet),
                firstStrategyCode(candidateSet),
                firstIntensityCode(candidateSet),
                "当前上下文证据不足，先采用稳健起步方案获取第一轮有效反馈。",
                "建议先执行低风险、短闭环动作，避免跨主题跳转导致额外负担。",
                List.of("先完成一轮结构梳理，记录具体卡点。"),
                List.of(),
                List.of("画出当前概念关系图", "完成一次微型理解校验", "基于结果决定提速或回补")
            );
        }
        return new LlmPlanDecisionResult(
            defaultDecision.selectedConceptId(),
            defaultDecision.selectedStrategyCode(),
            defaultDecision.selectedIntensityCode(),
            "当前上下文证据不足，采用稳健起步版本以确保可执行性。",
            "先收集新一轮学习证据，再做策略微调。",
            defaultDecision.evidenceBullets() == null || defaultDecision.evidenceBullets().isEmpty()
                ? List.of("先执行一轮低风险任务以补齐证据。")
                : defaultDecision.evidenceBullets().stream().limit(3).toList(),
            List.of(),
            ensureMinSize(keepDistinct(defaultDecision.nextActions(), 3), List.of("先完成结构梳理", "补齐关键理解", "做一轮短练习并复盘"), 3, 3)
        );
    }

    private String firstEntryId(PlanCandidateSet candidateSet) {
        if (candidateSet == null || candidateSet.entries() == null || candidateSet.entries().isEmpty()) {
            return "bootstrap-1";
        }
        return candidateSet.entries().get(0).conceptId();
    }

    private String firstStrategyCode(PlanCandidateSet candidateSet) {
        if (candidateSet == null || candidateSet.strategies() == null || candidateSet.strategies().isEmpty()) {
            return "FOUNDATION_FIRST";
        }
        return candidateSet.strategies().get(0).code();
    }

    private String firstIntensityCode(PlanCandidateSet candidateSet) {
        if (candidateSet == null || candidateSet.intensities() == null || candidateSet.intensities().isEmpty()) {
            return "STANDARD";
        }
        return candidateSet.intensities().get(0).code();
    }

    private String pickGoodText(String primary, String fallback, int minLength) {
        if (!isWeakText(primary, minLength) && !containsFiller(primary)) {
            return primary.trim();
        }
        return normalizeText(fallback);
    }

    private boolean isWeakText(String text, int minLength) {
        return text == null || text.isBlank() || text.trim().length() < minLength;
    }

    private boolean containsFiller(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("根据你的情况")
            || normalized.contains("综合来看")
            || normalized.contains("建议继续努力")
            || normalized.contains("保持节奏");
    }

    private boolean hasDuplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        Set<String> set = new HashSet<>();
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized.isBlank()) {
                continue;
            }
            if (!set.add(normalized)) {
                return true;
            }
        }
        return false;
    }

    private List<String> keepDistinct(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeText(value);
            if (!normalized.isBlank()) {
                set.add(normalized);
            }
            if (set.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private List<String> ensureMinSize(List<String> primary, List<String> fallback, int minSize, int maxSize) {
        List<String> result = new ArrayList<>(primary == null ? List.of() : primary);
        if (fallback != null) {
            for (String item : fallback) {
                if (result.size() >= maxSize) {
                    break;
                }
                if (!result.contains(item)) {
                    result.add(item);
                }
            }
        }
        while (result.size() < minSize) {
            result.add("执行一次短闭环动作并记录反馈");
        }
        return result.stream().limit(maxSize).toList();
    }

    private boolean containsAnyHint(String value, Set<String> hints) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String hint : hints) {
            if (hint == null || hint.isBlank()) {
                continue;
            }
            if (value.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private EntryCandidate findEntry(List<EntryCandidate> entries, String conceptId) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String expected = normalize(conceptId);
        for (EntryCandidate entry : entries) {
            if (normalize(entry.conceptId()).equals(expected)) {
                return entry;
            }
        }
        return null;
    }

    private Set<String> toEntryIds(List<EntryCandidate> entries) {
        if (entries == null) {
            return Set.of();
        }
        return entries.stream().map(EntryCandidate::conceptId).map(this::normalize).collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private Set<String> toStrategyCodes(List<StrategyCandidate> strategies) {
        if (strategies == null) {
            return Set.of();
        }
        return strategies.stream().map(StrategyCandidate::code).map(this::normalize).collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private Set<String> toIntensityCodes(List<IntensityCandidate> intensities) {
        if (intensities == null) {
            return Set.of();
        }
        return intensities.stream().map(IntensityCandidate::code).map(this::normalize).collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private void requireText(String field, String value, int minLength, List<String> errors) {
        if (value == null || value.isBlank() || value.trim().length() < minLength) {
            errors.add(field + "_invalid");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
