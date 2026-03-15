package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.LearnerSignalTier;
import com.pandanav.learning.domain.model.LearnerEvidenceSummary;
import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanAdjustments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnerEvidenceAggregatorRegressionTest {

    private final LearnerEvidenceAggregator aggregator = new LearnerEvidenceAggregator();

    @Test
    void shouldPassTenRegressionCasesForPreviewEvidenceAndRisk() {
        List<RegressionCase> cases = List.of(
            new RegressionCase("case-01-weak-concept", LearnerSignalTier.WEAK, LearnerSignalTier.WEAK, LearnerSignalTier.UNKNOWN, List.of(42, 45, 47), "断链并反复返工", "flat"),
            new RegressionCase("case-02-weak-code-mapping", LearnerSignalTier.UNKNOWN, LearnerSignalTier.STABLE, LearnerSignalTier.WEAK, List.of(58, 61, 63), "会概念但落不到代码", "flat"),
            new RegressionCase("case-03-stable-high-confidence", LearnerSignalTier.STABLE, LearnerSignalTier.STABLE, LearnerSignalTier.STABLE, List.of(78, 84, 88), "稳定性会下降", "up"),
            new RegressionCase("case-04-downward-trend", LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, List.of(82, 74, 66), "稳定性会下降", "down"),
            new RegressionCase("case-05-low-evidence", LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, List.of(), "稳定性会下降", "flat"),
            new RegressionCase("case-06-relationship-risk", LearnerSignalTier.STABLE, LearnerSignalTier.WEAK, LearnerSignalTier.UNKNOWN, List.of(70, 71, 73), "断链并反复返工", "flat"),
            new RegressionCase("case-07-borderline-up", LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, List.of(50, 58), "稳定性会下降", "up"),
            new RegressionCase("case-08-borderline-down", LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, List.of(66, 57), "稳定性会下降", "down"),
            new RegressionCase("case-09-weak-confidence-hint", LearnerSignalTier.WEAK, LearnerSignalTier.UNKNOWN, LearnerSignalTier.UNKNOWN, List.of(48, 48, 49), "断链并反复返工", "flat"),
            new RegressionCase("case-10-mixed-medium", LearnerSignalTier.UNKNOWN, LearnerSignalTier.STABLE, LearnerSignalTier.STABLE, List.of(63, 65, 68), "稳定性会下降", "flat")
        );

        for (RegressionCase item : cases) {
            LearningPlanPlanningContext context = buildContext(item.name(), item.scores());
            LearnerSignalSnapshot snapshot = buildSnapshot(item.concept(), item.relationship(), item.codeMapping());
            LearnerEvidenceSummary summary = aggregator.aggregate(context, snapshot, "二叉树遍历");

            assertNotNull(summary, item.name());
            assertNotNull(summary.machineSignals(), item.name());
            assertFalse(summary.machineSignals().isEmpty(), item.name());
            assertNotNull(summary.topEvidence(), item.name());
            assertFalse(summary.topEvidence().isEmpty(), item.name());
            assertTrue(summary.topEvidence().size() <= 3, item.name());
            assertTrue(summary.whyThisStep().contains("二叉树遍历"), item.name());
            assertTrue(summary.skipRisk().contains(item.expectedRiskSnippet()), item.name());
            assertTrue(summary.machineTrend().equals(item.expectedTrend()), item.name());
            assertFalse(summary.confidenceHint().isBlank(), item.name());
        }
    }

    private LearningPlanPlanningContext buildContext(String suffix, List<Integer> scores) {
        return new LearningPlanPlanningContext(
            1L,
            "goal-" + suffix,
            "diag-" + suffix,
            "course-a",
            "chapter-a",
            "掌握树结构的核心路径",
            10L,
            List.of(),
            List.of("边界遗漏", "索引越界"),
            scores,
            List.of("前置概念", "关系推导"),
            "需要先补基础再训练",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private LearnerSignalSnapshot buildSnapshot(
        LearnerSignalTier concept,
        LearnerSignalTier relationship,
        LearnerSignalTier codeMapping
    ) {
        return new LearnerSignalSnapshot(
            concept,
            relationship,
            codeMapping,
            LearnerSignalTier.UNKNOWN,
            LearnerSignalTier.UNKNOWN,
            LearnerSignalTier.UNKNOWN,
            LearnerSignalTier.UNKNOWN,
            LearnerSignalTier.UNKNOWN,
            concept == LearnerSignalTier.STABLE ? LearnerSignalTier.STABLE : concept == LearnerSignalTier.WEAK ? LearnerSignalTier.WEAK : LearnerSignalTier.UNKNOWN
        );
    }

    private record RegressionCase(
        String name,
        LearnerSignalTier concept,
        LearnerSignalTier relationship,
        LearnerSignalTier codeMapping,
        List<Integer> scores,
        String expectedRiskSnippet,
        String expectedTrend
    ) {
    }
}
