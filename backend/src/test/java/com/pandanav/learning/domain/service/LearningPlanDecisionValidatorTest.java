package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.DecisionFallbackLevel;
import com.pandanav.learning.domain.enums.ConceptCodeGap;
import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.FrustrationRisk;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.enums.PreferredLearningMode;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.AlternativeExplanation;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanDecisionValidationResult;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningPlanDecisionValidatorTest {

    private final LearningPlanDecisionValidator validator = new LearningPlanDecisionValidator();

    @Test
    void shouldUseL1WhenOnlyCopyQualityIsPoor() {
        LearningPlanDecisionValidationResult result = validator.validateAndFallback(
            sampleContext(),
            sampleLearnerState(),
            sampleCandidateSet(),
            new LlmPlanDecisionResult(
                "n1",
                "FOUNDATION_FIRST",
                "STANDARD",
                "综合来看建议继续努力",
                "综合来看建议继续努力",
                List.of("重复证据", "重复证据"),
                List.of(),
                List.of("围绕链表基础先画图", "围绕链表基础先画图", "围绕链表基础先画图")
            ),
            defaultDecision()
        );

        assertEquals(DecisionFallbackLevel.L1_COPY_REPAIRED, result.fallbackLevel());
        assertEquals("n1", result.finalDecision().selectedConceptId());
        assertEquals("FOUNDATION_FIRST", result.finalDecision().selectedStrategyCode());
        assertEquals(3, result.finalDecision().nextActions().size());
    }

    @Test
    void shouldUseL2WhenSelectionOutOfCandidates() {
        LearningPlanDecisionValidationResult result = validator.validateAndFallback(
            sampleContext(),
            sampleLearnerState(),
            sampleCandidateSet(),
            new LlmPlanDecisionResult(
                "unknown",
                "UNKNOWN",
                "UNKNOWN",
                "这是一个足够长且看起来合法的理由说明文本。",
                "这是一个足够长且看起来合法的当前状态总结文本。",
                List.of("证据一"),
                List.of(new AlternativeExplanation("UNKNOWN", "x", "y", "z")),
                List.of("围绕链表基础先画结构图", "围绕链表基础做两道题", "围绕链表基础复盘")
            ),
            defaultDecision()
        );

        assertEquals(DecisionFallbackLevel.L2_FULL_DEFAULT, result.fallbackLevel());
        assertEquals(defaultDecision().selectedConceptId(), result.finalDecision().selectedConceptId());
    }

    @Test
    void shouldUseL3WhenContextInsufficient() {
        LearningPlanDecisionValidationResult result = validator.validateAndFallback(
            sampleContext(),
            sampleLearnerState(),
            new PlanCandidateSet(List.of(), List.of(), List.of(), List.of()),
            null,
            defaultDecision()
        );

        assertEquals(DecisionFallbackLevel.L3_ROBUST_START, result.fallbackLevel());
        assertEquals(3, result.finalDecision().nextActions().size());
        assertTrue(result.finalDecision().heroReason().contains("证据不足"));
    }

    private LearnerState sampleLearnerState() {
        return new LearnerState(
            GoalOrientation.UNDERSTAND_PRINCIPLE,
            PreferredLearningMode.LEARN_THEN_PRACTICE,
            PacePreference.NORMAL,
            CurrentBlockType.FOUNDATION_GAP,
            EvidenceLevel.MEDIUM,
            MotivationRisk.MEDIUM,
            FoundationStatus.WEAK,
            PracticeReadiness.NEEDS_WARMUP,
            ConceptCodeGap.MEDIUM,
            FrustrationRisk.MEDIUM,
            "当前证据中等",
            "前置基础不稳定",
            null,
            List.of("近期错因集中在概念边界")
        );
    }

    private PlanCandidateSet sampleCandidateSet() {
        return new PlanCandidateSet(
            List.of(new EntryCandidate("n1", "链表基础", "先补前置依赖", 12, "HIGH")),
            List.of(
                new StrategyCandidate("FOUNDATION_FIRST", "先补基础", "先补再推", "短期慢"),
                new StrategyCandidate("PRACTICE_FIRST", "先练后学", "先暴露盲点", "挫败风险更高")
            ),
            List.of(new IntensityCandidate("STANDARD", "标准节奏", 15, "默认强度")),
            List.of(
                new ActionTemplate("STRUCTURE", "建立结构图", "明确依赖", "先画图", "AI补缺口", 6),
                new ActionTemplate("UNDERSTANDING", "补齐关键理解", "解释边界", "讲清例子", "AI切换解释角度", 8),
                new ActionTemplate("TRAINING", "做定向训练", "验证回补", "做短练", "AI归因", 10)
            )
        );
    }

    private LearningPlanPlanningContext sampleContext() {
        return new LearningPlanPlanningContext(
            1L, "goal-1", "diag-1", "course-1", "chapter-1", "goal", null,
            List.of(), List.of(), List.of(), List.of(), "summary", PlanAdjustments.defaults(),
            null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private LlmPlanDecisionResult defaultDecision() {
        return new LlmPlanDecisionResult(
            "n1",
            "FOUNDATION_FIRST",
            "STANDARD",
            "优先补齐链表基础可降低后续回退风险并提升稳定性。",
            "当前学习状态需要先把前置概念连接稳住，再推进训练。",
            List.of("当前卡点集中在前置概念边界。", "先补基础收益更高。"),
            List.of(),
            List.of("先画出链表基础关系图", "解释两个关键边界例子", "完成3题短练并复盘")
        );
    }
}
