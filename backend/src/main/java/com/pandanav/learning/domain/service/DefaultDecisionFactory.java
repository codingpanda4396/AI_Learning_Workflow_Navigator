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
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultDecisionFactory {

    public LlmPlanDecisionResult create(LearnerState learnerState, PlanCandidateSet candidateSet) {
        EntryCandidate selectedEntry = pickEntry(candidateSet.entries());
        StrategyCandidate selectedStrategy = pickStrategy(learnerState, candidateSet.strategies());
        IntensityCandidate selectedIntensity = pickIntensity(learnerState, candidateSet.intensities());
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
            buildEvidenceBullets(learnerState, selectedEntry),
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

    private StrategyCandidate pickStrategy(LearnerState learnerState, List<StrategyCandidate> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            return new StrategyCandidate("FOUNDATION_FIRST", "先补基础", "默认策略。", "短期推进偏慢。");
        }
        String preferred = resolvePreferredStrategyCode(learnerState);
        for (StrategyCandidate strategy : strategies) {
            if (preferred.equals(strategy.code())) {
                return strategy;
            }
        }
        return strategies.get(0);
    }

    private IntensityCandidate pickIntensity(LearnerState learnerState, List<IntensityCandidate> intensities) {
        if (intensities == null || intensities.isEmpty()) {
            return new IntensityCandidate("STANDARD", "标准节奏", 15, "默认节奏。");
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

    private List<String> buildEvidenceBullets(LearnerState learnerState, EntryCandidate selectedEntry) {
        List<String> bullets = new ArrayList<>();
        if (learnerState.evidenceSummaries() != null) {
            bullets.addAll(learnerState.evidenceSummaries().stream()
                .filter(item -> item != null && !item.isBlank())
                .limit(2)
                .toList());
        }
        bullets.add("候选起点「" + selectedEntry.conceptName() + "」在当前路径中的收益优先级更高。");
        return bullets.stream().limit(3).toList();
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
