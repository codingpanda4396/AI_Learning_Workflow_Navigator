package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.LearnerSignalTier;
import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DefaultLearnerSignalInterpreter implements LearnerSignalInterpreter {

    @Override
    public LearnerSignalSnapshot interpret(LearningPlanPlanningContext context) {
        List<LearningPlanContextNode> nodes = context.nodes() == null ? List.of() : context.nodes();
        int attempts = nodes.stream()
            .map(LearningPlanContextNode::attemptCount)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .sum();
        double avgMastery = nodes.stream()
            .map(LearningPlanContextNode::mastery)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0D);
        int weakReasonCount = nodes.stream()
            .map(LearningPlanContextNode::weakReasons)
            .filter(item -> item != null)
            .mapToInt(List::size)
            .sum();
        int relationTagCount = countContains(context.recentErrorTags(), "LINK", "DEPENDENCY", "RELATION", "CONFUSION");
        int codeTagCount = countContains(context.recentErrorTags(), "CODE", "IMPLEMENT", "TRANSFER", "PRACTICE");
        int expressionTagCount = countContains(context.recentErrorTags(), "EXPLAIN", "DESCRIPTION", "CONCEPT");
        boolean lowRecentScore = average(context.recentScores()) > 0D && average(context.recentScores()) < 60D;

        LearnerSignalTier conceptUnderstanding = byMastery(avgMastery);
        LearnerSignalTier relationshipUnderstanding = byCount(relationTagCount, 0, 2);
        LearnerSignalTier codeMapping = byCount(codeTagCount, 0, 2);
        LearnerSignalTier decompositionAbility = byAttempts(attempts, lowRecentScore);
        LearnerSignalTier expressionClarity = byCount(expressionTagCount, 0, 2);
        LearnerSignalTier recentStability = lowRecentScore || weakReasonCount >= 4 ? LearnerSignalTier.WEAK : LearnerSignalTier.STABLE;
        LearnerSignalTier timePressure = resolveTimePressure(context.requestedTimeBudgetMinutes(), context.userFeedback());
        LearnerSignalTier pacePreference = resolvePacePreference(context.adjustments() == null ? null : context.adjustments().intensity());
        LearnerSignalTier confidence = resolveConfidence(nodes.size(), attempts, avgMastery, weakReasonCount);

        return new LearnerSignalSnapshot(
            conceptUnderstanding,
            relationshipUnderstanding,
            codeMapping,
            decompositionAbility,
            expressionClarity,
            recentStability,
            timePressure,
            pacePreference,
            confidence
        );
    }

    private LearnerSignalTier byMastery(double avgMastery) {
        if (avgMastery <= 0D) {
            return LearnerSignalTier.UNKNOWN;
        }
        if (avgMastery < 60D) {
            return LearnerSignalTier.WEAK;
        }
        return LearnerSignalTier.STABLE;
    }

    private LearnerSignalTier byCount(int count, int unknownUpper, int weakUpper) {
        if (count <= unknownUpper) {
            return LearnerSignalTier.UNKNOWN;
        }
        if (count >= weakUpper) {
            return LearnerSignalTier.WEAK;
        }
        return LearnerSignalTier.STABLE;
    }

    private LearnerSignalTier byAttempts(int attempts, boolean lowRecentScore) {
        if (attempts <= 0) {
            return LearnerSignalTier.UNKNOWN;
        }
        if (attempts < 3 || lowRecentScore) {
            return LearnerSignalTier.WEAK;
        }
        return LearnerSignalTier.STABLE;
    }

    private LearnerSignalTier resolveTimePressure(Integer budgetMinutes, String userFeedback) {
        if (budgetMinutes == null && (userFeedback == null || userFeedback.isBlank())) {
            return LearnerSignalTier.UNKNOWN;
        }
        if (budgetMinutes != null && budgetMinutes <= 10) {
            return LearnerSignalTier.WEAK;
        }
        String feedback = userFeedback == null ? "" : userFeedback.toUpperCase(Locale.ROOT);
        if (feedback.contains("TIME_LIMITED") || feedback.contains("TOO_SLOW")) {
            return LearnerSignalTier.WEAK;
        }
        return LearnerSignalTier.STABLE;
    }

    private LearnerSignalTier resolvePacePreference(String intensity) {
        if (intensity == null || intensity.isBlank()) {
            return LearnerSignalTier.UNKNOWN;
        }
        String normalized = intensity.toUpperCase(Locale.ROOT);
        if ("LIGHT".equals(normalized) || "INTENSIVE".equals(normalized)) {
            return LearnerSignalTier.STABLE;
        }
        return LearnerSignalTier.UNKNOWN;
    }

    private LearnerSignalTier resolveConfidence(int nodeCount, int attempts, double avgMastery, int weakReasonCount) {
        if (nodeCount == 0 || attempts == 0) {
            return LearnerSignalTier.UNKNOWN;
        }
        if (avgMastery < 55D || weakReasonCount >= 4) {
            return LearnerSignalTier.WEAK;
        }
        return LearnerSignalTier.STABLE;
    }

    private int countContains(List<String> values, String... tokens) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            for (String token : tokens) {
                if (upper.contains(token)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private double average(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0D;
        }
        return values.stream().mapToInt(Integer::intValue).average().orElse(0D);
    }
}
