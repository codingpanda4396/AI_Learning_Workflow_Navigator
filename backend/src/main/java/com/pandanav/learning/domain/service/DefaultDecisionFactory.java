package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.AlternativeExplanation;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Component
public class DefaultDecisionFactory {

    public LlmPlanDecisionResult create(LearningPlanPlanningContext context, LearnerState learnerState, PlanCandidateSet candidateSet) {
        EntryCandidate selectedEntry = pickEntry(candidateSet.entries());
        StrategyCandidate selectedStrategy = pickStrategy(context, learnerState, candidateSet.strategies());
        IntensityCandidate selectedIntensity = pickIntensity(context, learnerState, candidateSet.intensities());
        List<String> nextActions = candidateSet.actionTemplates().stream()
            .map(ActionTemplate::title)
            .filter(item -> item != null && !item.isBlank())
            .limit(3)
            .toList();
        return new LlmPlanDecisionResult(
            selectedEntry.conceptId(),
            selectedStrategy.code(),
            selectedIntensity.code(),
            buildHeroReason(selectedEntry, selectedStrategy),
            buildCurrentStateSummary(learnerState),
            buildEvidenceBullets(context, learnerState, selectedEntry),
            buildAlternativeExplanations(selectedStrategy, candidateSet.strategies()),
            ensureThreeActions(nextActions)
        );
    }

    private EntryCandidate pickEntry(List<EntryCandidate> entries) {
        if (entries == null || entries.isEmpty()) {
            return new EntryCandidate("bootstrap-1", "当前章节起步节点", "默认起步候选。", 12, "MEDIUM");
        }
        return entries.get(0);
    }

    private StrategyCandidate pickStrategy(
        LearningPlanPlanningContext context,
        LearnerState learnerState,
        List<StrategyCandidate> strategies
    ) {
        if (strategies == null || strategies.isEmpty()) {
            return new StrategyCandidate("FOUNDATION_FIRST", "先补基础", "默认策略。", "短期推进偏慢。");
        }
        String profilePreferred = resolveProfilePreferredStrategyCode(context == null ? null : context.learnerProfileSnapshot());
        if (profilePreferred != null) {
            for (StrategyCandidate strategy : strategies) {
                if (profilePreferred.equals(strategy.code())) {
                    return strategy;
                }
            }
        }
        String preferred = resolvePreferredStrategyCode(learnerState);
        for (StrategyCandidate strategy : strategies) {
            if (preferred.equals(strategy.code())) {
                return strategy;
            }
        }
        return strategies.get(0);
    }

    private IntensityCandidate pickIntensity(
        LearningPlanPlanningContext context,
        LearnerState learnerState,
        List<IntensityCandidate> intensities
    ) {
        if (intensities == null || intensities.isEmpty()) {
            return new IntensityCandidate("STANDARD", "标准节奏", 15, "默认节奏。");
        }
        String profilePreferred = resolveProfilePreferredIntensityCode(context == null ? null : context.learnerProfileSnapshot());
        if (profilePreferred != null) {
            for (IntensityCandidate intensity : intensities) {
                if (profilePreferred.equals(intensity.code())) {
                    return intensity;
                }
            }
        }
        String preferred = switch (learnerState.pacePreference()) {
            case LIGHT -> "LIGHT";
            case INTENSIVE -> "INTENSIVE";
            case NORMAL, UNKNOWN -> "STANDARD";
        };
        for (IntensityCandidate intensity : intensities) {
            if (preferred.equals(intensity.code())) {
                return intensity;
            }
        }
        return intensities.get(0);
    }

    private String resolvePreferredStrategyCode(LearnerState learnerState) {
        if (learnerState.foundationStatus() == FoundationStatus.WEAK
            || learnerState.currentBlockType() == CurrentBlockType.FOUNDATION_GAP) {
            return "FOUNDATION_FIRST";
        }
        if (learnerState.practiceReadiness() == PracticeReadiness.READY
            || learnerState.currentBlockType() == CurrentBlockType.APPLICATION_GAP) {
            return "PRACTICE_FIRST";
        }
        if (learnerState.goalOrientation() == GoalOrientation.QUICK_START
            && learnerState.pacePreference() == PacePreference.INTENSIVE) {
            return "FAST_TRACK";
        }
        return "FOUNDATION_FIRST";
    }

    private String buildHeroReason(EntryCandidate selectedEntry, StrategyCandidate selectedStrategy) {
        return "优先选择「" + selectedEntry.conceptName() + "」，并采用「" + selectedStrategy.label() + "」，可在当前证据下兼顾稳定性与推进效率。";
    }

    private String buildCurrentStateSummary(LearnerState learnerState) {
        String block = learnerState.primaryBlockDescription() == null ? "当前学习状态需要先做稳健收敛。" : learnerState.primaryBlockDescription();
        return block + " 当前建议以小步快跑方式获取下一轮反馈。";
    }

    private List<String> buildEvidenceBullets(
        LearningPlanPlanningContext context,
        LearnerState learnerState,
        EntryCandidate selectedEntry
    ) {
        List<String> bullets = new ArrayList<>();
        if (learnerState.evidenceSummaries() != null) {
            bullets.addAll(learnerState.evidenceSummaries().stream()
                .filter(item -> item != null && !item.isBlank())
                .limit(2)
                .toList());
        }
        appendProfileEvidence(bullets, context == null ? null : context.learnerProfileSnapshot());
        bullets.add("候选起点「" + selectedEntry.conceptName() + "」在当前路径中的收益优先级更高。");
        return bullets.stream().limit(3).toList();
    }

    private void appendProfileEvidence(List<String> bullets, LearnerProfileSnapshot snapshot) {
        if (snapshot == null || bullets.size() >= 2) {
            return;
        }
        String preference = readHint(snapshot.getStrategyHints(), "learningPreference");
        String budget = readHint(snapshot.getConstraints(), "timeBudget");
        if (!preference.isBlank()) {
            bullets.add("画像显示学习偏好为「" + preference + "」，候选策略已按该偏好优先排序。");
        } else if (!budget.isBlank()) {
            bullets.add("画像约束显示时间预算偏「" + budget + "」，节奏候选已做匹配。");
        }
    }

    private String resolveProfilePreferredStrategyCode(LearnerProfileSnapshot snapshot) {
        String preference = readHint(snapshot == null ? null : snapshot.getStrategyHints(), "learningPreference");
        String supportPriority = readHint(snapshot == null ? null : snapshot.getStrategyHints(), "supportPriority");
        if ("PRACTICE_FIRST".equals(preference) || "PROJECT_DRIVEN".equals(preference)) {
            return "PRACTICE_FIRST";
        }
        if ("CONCEPT_FIRST".equals(preference) || "UNDERSTANDING".equals(supportPriority) || "SPACED_REVIEW".equals(supportPriority)) {
            return "FOUNDATION_FIRST";
        }
        if ("TRAINING_VARIANTS".equals(supportPriority) || "GUIDED_PRACTICE".equals(supportPriority)) {
            return "PRACTICE_FIRST";
        }
        return null;
    }

    private String resolveProfilePreferredIntensityCode(LearnerProfileSnapshot snapshot) {
        String intensity = readHint(snapshot == null ? null : snapshot.getConstraints(), "learningIntensity");
        String budget = readHint(snapshot == null ? null : snapshot.getConstraints(), "timeBudget");
        if ("LIGHT".equals(intensity) || "LIGHT".equals(budget)) {
            return "LIGHT";
        }
        if ("INTENSIVE".equals(intensity) || "IMMERSIVE".equals(intensity) || "IMMERSIVE".equals(budget)) {
            return "INTENSIVE";
        }
        if ("STANDARD".equals(intensity) || "STANDARD".equals(budget)) {
            return "STANDARD";
        }
        return null;
    }

    private String readHint(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        Object raw = source.get(key);
        if (raw == null) {
            return "";
        }
        return String.valueOf(raw).trim().toUpperCase(Locale.ROOT);
    }

    private List<AlternativeExplanation> buildAlternativeExplanations(
        StrategyCandidate selected,
        List<StrategyCandidate> strategies
    ) {
        if (strategies == null || strategies.isEmpty()) {
            return List.of();
        }
        return strategies.stream()
            .filter(item -> !item.code().equals(selected.code()))
            .map(item -> new AlternativeExplanation(item.code(), item.label(), item.description(), item.tradeoff()))
            .limit(3)
            .toList();
    }

    private List<String> ensureThreeActions(List<String> actions) {
        List<String> result = new ArrayList<>(actions);
        while (result.size() < 3) {
            result.add(switch (result.size()) {
                case 0 -> "先完成结构梳理";
                case 1 -> "补齐关键理解";
                default -> "做一轮短练习并复盘";
            });
        }
        return result.stream().limit(3).toList();
    }
}
