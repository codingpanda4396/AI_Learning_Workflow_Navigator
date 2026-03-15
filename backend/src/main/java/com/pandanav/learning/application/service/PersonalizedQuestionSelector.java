package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Selects and orders questions from the candidate pool using profile and strategy.
 * finalScore = basePriority + strategyBoost + profileSignalBoost - suppressPenalty
 */
@Component
public class PersonalizedQuestionSelector {

    private static final int STRATEGY_BOOST = 5;
    private static final int PROFILE_SIGNAL_BOOST = 3;
    private static final int SUPPRESS_PENALTY = 8;

    public List<DiagnosisQuestionDraft> select(
        List<DiagnosisQuestionCandidate> candidates,
        DiagnosisStrategyDecision strategyDecision,
        DiagnosisLearnerProfileSnapshot profileSnapshot
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        int targetCount = strategyDecision != null && strategyDecision.targetQuestionCount() > 0
            ? strategyDecision.targetQuestionCount()
            : candidates.size();

        List<ScoredCandidate> scored = candidates.stream()
            .map(c -> score(c, strategyDecision, profileSnapshot))
            .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
            .toList();

        return IntStream.range(0, Math.min(targetCount, scored.size()))
            .mapToObj(i -> {
                ScoredCandidate sc = scored.get(i);
                return new DiagnosisQuestionDraft(
                    sc.candidate.content(),
                    sc.reason,
                    i + 1,
                    sc.candidate.dimension()
                );
            })
            .toList();
    }

    private ScoredCandidate score(
        DiagnosisQuestionCandidate c,
        DiagnosisStrategyDecision strategy,
        DiagnosisLearnerProfileSnapshot profile
    ) {
        int base = c.priorityBaseScore();
        int strategyBoost = strategy != null && strategy.priorityDimensions() != null
            && strategy.priorityDimensions().contains(c.dimension().name())
            ? STRATEGY_BOOST : 0;
        int profileBoost = 0;
        for (String signal : c.triggerSignals()) {
            if (profile.weaknessTags() != null && profile.weaknessTags().contains(signal)) {
                profileBoost += PROFILE_SIGNAL_BOOST;
                break;
            }
            if (profile.behaviorSignals() != null && profile.behaviorSignals().contains(signal)) {
                profileBoost += PROFILE_SIGNAL_BOOST;
                break;
            }
        }
        int suppress = 0;
        if (strategy != null && strategy.suppressedDimensions() != null
            && strategy.suppressedDimensions().contains(c.dimension().name())) {
            suppress += SUPPRESS_PENALTY;
        }
        for (String sig : c.suppressSignals()) {
            if ("TIME_LIMITED".equals(sig) && profile.timeConstraint() != null
                && "LOW".equals(profile.timeConstraint())) {
                suppress += SUPPRESS_PENALTY / 2;
                break;
            }
        }
        int total = base + strategyBoost + profileBoost - suppress;
        String reason = buildReason(c, strategyBoost, profileBoost, suppress);
        return new ScoredCandidate(c, total, reason);
    }

    private String buildReason(DiagnosisQuestionCandidate c, int strategyBoost, int profileBoost, int suppress) {
        if (strategyBoost > 0) {
            return "策略优先维度: " + c.dimension().name();
        }
        if (profileBoost > 0) {
            return "画像信号匹配: " + c.intentCode();
        }
        if (suppress > 0) {
            return "常规维度: " + c.dimension().name();
        }
        return "基础优先级: " + c.intentCode();
    }

    private record ScoredCandidate(DiagnosisQuestionCandidate candidate, int score, String reason) {}
}
