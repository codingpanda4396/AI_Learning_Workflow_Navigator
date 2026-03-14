package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
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
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.domain.model.ActionTemplate;
import com.pandanav.learning.domain.model.EntryCandidate;
import com.pandanav.learning.domain.model.IntensityCandidate;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.StrategyCandidate;
import com.pandanav.learning.domain.service.LearningPlanDecisionPromptBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningPlanDecisionLlmServiceTest {

    @Test
    void shouldReturnDecisionWhenLlmJsonIsValid() {
        LlmGateway gateway = new StaticGateway(new LlmTextResult(validJson(), "provider", "model", LlmInvocationProfile.LIGHT_JSON_TASK, null, null, null));
        LearningPlanDecisionLlmService service = service(gateway);

        Optional<LlmPlanDecisionResult> result = service.decide(sampleLearnerState(), sampleCandidateSet());

        assertTrue(result.isPresent());
        assertEquals("n1", result.get().selectedConceptId());
        assertEquals("FOUNDATION_FIRST", result.get().selectedStrategyCode());
        assertEquals(3, result.get().nextActions().size());
    }

    @Test
    void shouldReturnEmptyWhenJsonInvalid() {
        LlmGateway gateway = new StaticGateway(new LlmTextResult("not-json", "provider", "model", LlmInvocationProfile.LIGHT_JSON_TASK, null, null, null));
        LearningPlanDecisionLlmService service = service(gateway);

        Optional<LlmPlanDecisionResult> result = service.decide(sampleLearnerState(), sampleCandidateSet());

        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenOutputTruncated() {
        LlmGateway gateway = new StaticGateway(new LlmTextResult(
            validJson(),
            "provider",
            "model",
            LlmInvocationProfile.LIGHT_JSON_TASK,
            new LlmUsage(100, 200, 300, -1, 900, "length", false, true),
            null,
            null
        ));
        LearningPlanDecisionLlmService service = service(gateway);

        Optional<LlmPlanDecisionResult> result = service.decide(sampleLearnerState(), sampleCandidateSet());

        assertFalse(result.isPresent());
    }

    private LearningPlanDecisionLlmService service(LlmGateway gateway) {
        return new LearningPlanDecisionLlmService(
            gateway,
            new LlmJsonParser(new ObjectMapper()),
            new LearningPlanDecisionPromptBuilder()
        );
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
            "基础证据充足",
            "当前基础依赖不稳定",
            null,
            List.of("近期错误集中在概念连接")
        );
    }

    private PlanCandidateSet sampleCandidateSet() {
        return new PlanCandidateSet(
            List.of(new EntryCandidate("n1", "链表基础", "优先处理基础连接", 12, "HIGH")),
            List.of(new StrategyCandidate("FOUNDATION_FIRST", "先补基础", "先补基础再推进", "短期推进较慢")),
            List.of(new IntensityCandidate("STANDARD", "标准节奏", 15, "默认节奏")),
            List.of(new ActionTemplate("STRUCTURE", "建立结构图", "明确依赖", "画图并口述", "AI补缺口", 6))
        );
    }

    private String validJson() {
        return """
            {
              "selectedConceptId":"n1",
              "selectedStrategyCode":"FOUNDATION_FIRST",
              "selectedIntensityCode":"STANDARD",
              "heroReason":"先补齐链表基础可以降低后续回退。",
              "currentStateSummary":"当前主要卡点在前置概念连接，先做稳健收敛更高效。",
              "evidenceBullets":[
                "近期错误主要出现在概念边界判定。",
                "当前节点在依赖链上游，收益优先级更高。"
              ],
              "alternativeExplanations":[
                {"strategyCode":"FOUNDATION_FIRST","label":"先补基础","reason":"先稳住依赖","tradeoff":"短期推进慢"}
              ],
              "nextActions":[
                "先画出链表基础关系图",
                "用一正一反例解释关键边界",
                "完成3题短练习并复盘错因"
              ]
            }
            """;
    }

    private record StaticGateway(LlmTextResult result) implements LlmGateway {
        @Override
        public LlmTextResult generate(com.pandanav.learning.domain.llm.model.LlmStage stage, LlmPrompt prompt) {
            return result;
        }
    }
}
