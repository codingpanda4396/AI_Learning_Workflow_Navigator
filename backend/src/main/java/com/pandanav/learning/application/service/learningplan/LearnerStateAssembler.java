package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.ConceptCodeGap;
import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.FrustrationRisk;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LearnerStateAssembler {

    private final LearnerStateInterpreter learnerStateInterpreter;

    public LearnerStateAssembler(LearnerStateInterpreter learnerStateInterpreter) {
        this.learnerStateInterpreter = learnerStateInterpreter;
    }

    public LearnerState assemble(LearningPlanPlanningContext context) {
        LearnerStateSnapshot snapshot = context.learnerStateSnapshot() == null
            ? learnerStateInterpreter.interpret(context)
            : context.learnerStateSnapshot();
        return new LearnerState(
            snapshot.goalOrientation(),
            snapshot.preferredLearningMode(),
            snapshot.pacePreference(),
            snapshot.currentBlockType(),
            snapshot.evidenceLevel(),
            snapshot.motivationRisk(),
            resolveFoundationStatus(context, snapshot.currentBlockType()),
            resolvePracticeReadiness(context, snapshot.currentBlockType()),
            resolveConceptCodeGap(context),
            resolveFrustrationRisk(context, snapshot.motivationRisk(), snapshot.pacePreference()),
            snapshot.confidenceReasonSummary(),
            snapshot.primaryBlockDescription(),
            snapshot.secondaryBlockDescription(),
            buildEvidenceSummaries(context, snapshot)
        );
    }

    private FoundationStatus resolveFoundationStatus(LearningPlanPlanningContext context, CurrentBlockType blockType) {
        if (blockType == CurrentBlockType.FOUNDATION_GAP) {
            return FoundationStatus.WEAK;
        }
        List<LearningPlanContextNode> nodes = safeNodes(context);
        if (nodes.isEmpty()) {
            return FoundationStatus.UNKNOWN;
        }
        double avgTopTwoMastery = nodes.stream()
            .filter(item -> item.orderNo() != null && item.orderNo() <= 2)
            .map(LearningPlanContextNode::mastery)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0D);
        if (avgTopTwoMastery <= 0D) {
            return FoundationStatus.UNKNOWN;
        }
        return avgTopTwoMastery < 60D ? FoundationStatus.WEAK : FoundationStatus.SOLID;
    }

    private PracticeReadiness resolvePracticeReadiness(LearningPlanPlanningContext context, CurrentBlockType blockType) {
        if (blockType == CurrentBlockType.FOUNDATION_GAP || blockType == CurrentBlockType.EVIDENCE_LOW) {
            return PracticeReadiness.NOT_READY;
        }
        int attempts = safeNodes(context).stream()
            .map(LearningPlanContextNode::attemptCount)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .sum();
        double averageMastery = safeNodes(context).stream()
            .map(LearningPlanContextNode::mastery)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0D);
        if (attempts >= 3 && averageMastery >= 60D) {
            return PracticeReadiness.READY;
        }
        return PracticeReadiness.NEEDS_WARMUP;
    }

    private ConceptCodeGap resolveConceptCodeGap(LearningPlanPlanningContext context) {
        int weakReasons = safeNodes(context).stream()
            .map(LearningPlanContextNode::weakReasons)
            .filter(item -> item != null)
            .mapToInt(List::size)
            .sum();
        int conceptConfusions = countContains(context.recentErrorTags(), "CONFUSION", "CONCEPT", "LINK");
        int score = weakReasons + conceptConfusions;
        if (score >= 6) {
            return ConceptCodeGap.HIGH;
        }
        if (score >= 3) {
            return ConceptCodeGap.MEDIUM;
        }
        return ConceptCodeGap.LOW;
    }

    private FrustrationRisk resolveFrustrationRisk(
        LearningPlanPlanningContext context,
        MotivationRisk motivationRisk,
        PacePreference pacePreference
    ) {
        int riskScore = switch (motivationRisk) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
        if (pacePreference == PacePreference.INTENSIVE) {
            riskScore += 1;
        }
        if (isRecentScoreLow(context.recentScores())) {
            riskScore += 1;
        }
        if (countContains(context.recentErrorTags(), "TIMEOUT", "PANIC", "CONFUSION", "WRONG") >= 2) {
            riskScore += 1;
        }
        if (riskScore >= 5) {
            return FrustrationRisk.HIGH;
        }
        if (riskScore >= 3) {
            return FrustrationRisk.MEDIUM;
        }
        return FrustrationRisk.LOW;
    }

    private List<String> buildEvidenceSummaries(LearningPlanPlanningContext context, LearnerStateSnapshot snapshot) {
        List<String> result = new ArrayList<>();
        int attempts = safeNodes(context).stream()
            .map(LearningPlanContextNode::attemptCount)
            .filter(item -> item != null)
            .mapToInt(Integer::intValue)
            .sum();
        if (attempts > 0) {
            result.add("近期累计练习尝试 " + attempts + " 次。");
        }
        if (context.recentScores() != null && !context.recentScores().isEmpty()) {
            int avg = (int) Math.round(context.recentScores().stream().mapToInt(Integer::intValue).average().orElse(0D));
            result.add("最近得分均值约 " + avg + " 分。");
        }
        if (context.weakPointLabels() != null && !context.weakPointLabels().isEmpty()) {
            result.add("薄弱点集中在：" + String.join("、", context.weakPointLabels().stream().limit(2).toList()) + "。");
        }
        if (result.isEmpty()) {
            result.add("当前证据有限，建议先稳健起步并尽快补充行为数据。");
        }
        if (snapshot.secondaryBlockDescription() != null && !snapshot.secondaryBlockDescription().isBlank()) {
            result.add(snapshot.secondaryBlockDescription());
        }
        return result.stream().limit(4).toList();
    }

    private boolean isRecentScoreLow(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return false;
        }
        return scores.stream().mapToInt(Integer::intValue).average().orElse(100D) < 60D;
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

    private List<LearningPlanContextNode> safeNodes(LearningPlanPlanningContext context) {
        if (context.nodes() == null) {
            return List.of();
        }
        return context.nodes();
    }
}
