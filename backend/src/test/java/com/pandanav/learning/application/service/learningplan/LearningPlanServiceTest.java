package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.plan.AdjustLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.application.command.AdjustLearningPlanCommand;
import com.pandanav.learning.application.command.ConfirmLearningPlanCommand;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.domain.enums.LearningPlanStatus;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PreferredLearningMode;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlan;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.PlanGuidance;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import com.pandanav.learning.domain.model.PreviewEnhancement;
import com.pandanav.learning.domain.model.StrategyComparison;
import com.pandanav.learning.domain.model.StrategyOptionComparison;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningPlanRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.observability.LearningPlanMetricsLogger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPlanServiceTest {

    @Test
    void shouldPreviewAndPersistLearningPlan() {
        PlanningContextAssembler assembler = mock(PlanningContextAssembler.class);
        LearningPlanOrchestrator orchestrator = mock(LearningPlanOrchestrator.class);
        LearningPlanRepository repository = mock(LearningPlanRepository.class);

        when(assembler.assemble(any())).thenReturn(sampleContext());
        when(orchestrator.preview(any())).thenReturn(sampleOrchestratedPlan(samplePreview(), false, List.of()));
        when(repository.save(any())).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(88L);
            return plan;
        });

        LearningPlanService service = service(assembler, orchestrator, repository);
        LearningPlanPreviewResponse response = service.preview(new PreviewLearningPlanCommand(
            1L,
            "diag-1",
            null,
            null,
            null,
            "goal-1",
            new LearningPlanAdjustmentsDto("STANDARD", "LEARN_THEN_PRACTICE", true)
        ));

        assertEquals("88", response.planId());
        assertEquals("tree basics", response.recommendedEntry().title());
        assertNotNull(response.recommendedEntry().reason());
        assertNotNull(response.learnerSnapshot());
        assertNotNull(response.recommendedStrategy());
        assertEquals("FOUNDATION_FIRST", response.recommendedStrategy().code());
        assertNotNull(response.status());
        assertNotNull(response.previewOnly());
        assertNotNull(response.committed());
        assertNotNull(response.nextActions());
        assertEquals(3, response.nextActions().size());
        assertNotNull(response.adjustments());
        assertNotNull(response.startGuide());
        assertNotNull(response.traceId());
    }

    @Test
    void shouldAdjustLearningPlan() {
        PlanningContextAssembler assembler = mock(PlanningContextAssembler.class);
        LearningPlanOrchestrator orchestrator = mock(LearningPlanOrchestrator.class);
        LearningPlanRepository repository = mock(LearningPlanRepository.class);
        SessionRepository sessionRepository = mock(SessionRepository.class);

        ObjectMapper objectMapper = new ObjectMapper();
        LearningPlan stored = new LearningPlan();
        stored.setId(99L);
        stored.setUserId(1L);
        stored.setGoalId("goal-1");
        stored.setDiagnosisId("diag-1");
        stored.setStatus(LearningPlanStatus.DRAFT);
        stored.setSummaryJson(write(objectMapper, samplePreview().summary()));
        stored.setReasonsJson(write(objectMapper, samplePreview().reasons()));
        stored.setFocusesJson(write(objectMapper, samplePreview().focuses()));
        stored.setPathPreviewJson(write(objectMapper, samplePreview().pathPreview()));
        stored.setTaskPreviewJson(write(objectMapper, samplePreview().taskPreview()));
        stored.setAdjustmentsJson(write(objectMapper, samplePreview().adjustments()));
        stored.setPlanningContextJson(write(objectMapper, sampleContext()));

        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            return plan;
        });
        when(orchestrator.preview(any())).thenReturn(sampleOrchestratedPlan(samplePreview(), false, List.of()));

        LearningPlanService service = new LearningPlanService(
            assembler,
            orchestrator,
            repository,
            new PreviewTemplateExplanationAssembler(),
            sessionRepository,
            mock(TaskRepository.class),
            mock(ConceptNodeRepository.class),
            objectiveStrategy(),
            objectMapper,
            new DefaultLearnerStateInterpreter(),
            new DefaultLearnerSignalInterpreter(),
            new LearnerEvidenceAggregator(),
            mock(LearningPlanMetricsLogger.class)
        );

        AdjustLearningPlanResponse response = service.adjust(new AdjustLearningPlanCommand(
            1L,
            null,
            99L,
            "FAST_TRACK",
            "Need a faster route",
            10,
            "TIME_LIMITED"
        ));

        assertNotNull(response.result());
        assertNotNull(response.changeSummary());
        assertTrueContains(response.adjustmentReason(), "FAST_TRACK");
    }

    @Test
    void shouldConfirmLearningPlanIntoSessionAndTasks() {
        PlanningContextAssembler assembler = mock(PlanningContextAssembler.class);
        LearningPlanOrchestrator orchestrator = mock(LearningPlanOrchestrator.class);
        LearningPlanRepository repository = mock(LearningPlanRepository.class);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        ConceptNodeRepository conceptNodeRepository = mock(ConceptNodeRepository.class);

        ObjectMapper objectMapper = new ObjectMapper();
        LearningPlan stored = new LearningPlan();
        stored.setId(99L);
        stored.setUserId(1L);
        stored.setGoalId("goal-1");
        stored.setDiagnosisId("diag-1");
        stored.setStatus(LearningPlanStatus.DRAFT);
        stored.setSummaryJson(write(objectMapper, samplePreview().summary()));
        stored.setReasonsJson(write(objectMapper, samplePreview().reasons()));
        stored.setFocusesJson(write(objectMapper, samplePreview().focuses()));
        stored.setPathPreviewJson(write(objectMapper, samplePreview().pathPreview()));
        stored.setTaskPreviewJson(write(objectMapper, samplePreview().taskPreview()));
        stored.setAdjustmentsJson(write(objectMapper, samplePreview().adjustments()));
        stored.setPlanningContextJson(write(objectMapper, sampleContext()));

        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(stored));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("chapter-1")).thenReturn(List.of(
            node(101L, "tree basics", 1),
            node(102L, "binary tree traversal", 2)
        ));
        when(sessionRepository.save(any())).thenAnswer(invocation -> {
            LearningSession session = invocation.getArgument(0);
            session.setId(500L);
            return session;
        });
        when(taskRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Task> tasks = invocation.getArgument(0);
            long id = 800L;
            for (Task task : tasks) {
                task.setId(id++);
            }
            return tasks;
        });
        when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LearningPlanService service = new LearningPlanService(
            assembler,
            orchestrator,
            repository,
            new PreviewTemplateExplanationAssembler(),
            sessionRepository,
            taskRepository,
            conceptNodeRepository,
            objectiveStrategy(),
            objectMapper,
            new DefaultLearnerStateInterpreter(),
            new DefaultLearnerSignalInterpreter(),
            new LearnerEvidenceAggregator(),
            mock(LearningPlanMetricsLogger.class)
        );

        ConfirmLearningPlanResponse response = service.confirm(new ConfirmLearningPlanCommand(99L, 1L));

        assertEquals(500L, response.sessionId());
        assertEquals(101L, response.currentNodeId());
        assertNotNull(response.firstTaskId());
    }

    private LearningPlanService service(
        PlanningContextAssembler assembler,
        LearningPlanOrchestrator orchestrator,
        LearningPlanRepository repository
    ) {
        return new LearningPlanService(
            assembler,
            orchestrator,
            repository,
            new PreviewTemplateExplanationAssembler(),
            mock(SessionRepository.class),
            mock(TaskRepository.class),
            mock(ConceptNodeRepository.class),
            objectiveStrategy(),
            new ObjectMapper(),
            new DefaultLearnerStateInterpreter(),
            new DefaultLearnerSignalInterpreter(),
            new LearnerEvidenceAggregator(),
            mock(LearningPlanMetricsLogger.class)
        );
    }

    private TaskObjectiveTemplateStrategy objectiveStrategy() {
        return (stage, conceptName) -> stage.name() + ":" + conceptName;
    }

    private LearningPlanPlanningContext sampleContext() {
        return new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "master binary trees",
            null,
            List.of(
                new LearningPlanContextNode("101", 101L, "tree basics", 1, 1, 40, 2, List.of("LOW_MASTERY"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("102", 102L, "binary tree traversal", 2, 2, 55, 1, List.of(), List.of(), List.of("101"))
            ),
            List.of("CONCEPT_CONFUSION"),
            List.of(55, 68),
            List.of("tree basics"),
            "Current weak point is tree basics",
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
            null
        );
    }

    private LearningPlanOrchestrator.OrchestratedPlan sampleOrchestratedPlan(
        LearningPlanPreview preview,
        boolean fallback,
        List<String> fallbackReasons
    ) {
        LearnerStateSnapshot learnerState = new LearnerStateSnapshot(
            GoalOrientation.UNDERSTAND_PRINCIPLE,
            PreferredLearningMode.LEARN_THEN_PRACTICE,
            PacePreference.NORMAL,
            CurrentBlockType.FOUNDATION_GAP,
            EvidenceLevel.MEDIUM,
            MotivationRisk.MEDIUM,
            "已有部分学习证据，推荐可信度中等。",
            "你当前主要卡在前置基础不稳。",
            null
        );
        PersonalizedNarrative narrative = new PersonalizedNarrative(
            "你当前主要卡在前置基础不稳。",
            List.of("系统检测到你的薄弱点集中在基础节点。", "当前推荐节点直接影响后续路径。"),
            "先稳住基础有助于后续连续推进。",
            "如果跳过当前步骤，后续训练容易反复卡住。",
            "本轮先不追求覆盖更多新内容，先追求基础稳定。",
            "完成本轮后会根据新表现动态调整。"
        );
        return new LearningPlanOrchestrator.OrchestratedPlan(
            preview,
            null,
            fallback ? PlanSource.RULE_FALLBACK : PlanSource.LLM,
            fallback,
            fallbackReasons,
            learnerState,
            new DecisionPlan(preview.summary().recommendedStartNodeId(), preview.summary().recommendedPace(), preview.summary().alternatives(), preview.summary().whyNow(), preview.reasons()),
            narrative,
            sampleEnhancement(),
            fallback ? NarrativeSource.FALLBACK : NarrativeSource.LLM,
            !fallback,
            fallback ? String.join(",", fallbackReasons) : null
        );
    }

    private PreviewEnhancement sampleEnhancement() {
        return new PreviewEnhancement(
            new PlanGuidance(
                "先补基础更稳。",
                "其他策略会放大当前阻塞。",
                "你在建立整体理解阶段。",
                "先画出本轮概念关系并口述。",
                "检查是否能完整说出依赖链。",
                "更稳但短期偏慢。",
                "表现好会提速推进。",
                "仍卡住会回补解释。",
                "时间少就切到压缩版。",
                "先开始第一步。",
                List.of("画关系图", "说依赖链"),
                "把框架说顺。",
                "是否进入稳定节奏。",
                "LOW_EVIDENCE_SAFE_START",
                "每轮根据表现自动校准。",
                "证据少时先低风险起步，再快速校准。"
            ),
            new StrategyComparison(
                "FOUNDATION_FIRST",
                List.of(
                    new StrategyOptionComparison("FOUNDATION_FIRST", "先补基础", "基础薄弱", "需要极速冲刺", "短期速度慢"),
                    new StrategyOptionComparison("FAST_TRACK", "快速推进", "基础较稳", "当前依赖断裂", "后续回退风险高"),
                    new StrategyOptionComparison("PRACTICE_FIRST", "先做题带学", "需要快速暴露盲点", "概念理解薄弱", "挫败感上升"),
                    new StrategyOptionComparison("COMPRESSED_10_MIN", "10分钟压缩版", "可用时间碎片化", "需要完整学习链路", "后续需要补课")
                )
            )
        );
    }

    private LearningPlanPreview samplePreview() {
        return new LearningPlanPreview(
            new LearningPlanSummary(
                "Strengthen basics before advancing",
                "101",
                "tree basics",
                "STANDARD",
                36,
                2,
                4,
                "Start from the biggest blocker first",
                "This prerequisite still blocks later traversal work.",
                "HIGH",
                "tree basics",
                "Map the structure",
                8,
                "HIGH",
                List.of(
                    new PlanAlternative("FAST_TRACK", "Fast track", "Move faster", "Higher risk"),
                    new PlanAlternative("FOUNDATION_FIRST", "Foundation first", "Stay stable", "Slower"),
                    new PlanAlternative("PRACTICE_FIRST", "Practice first", "Expose gaps", "Can feel harder"),
                    new PlanAlternative("COMPRESSED_10_MIN", "10 minute version", "Shrink current step", "Needs follow-up")
                ),
                List.of("Reduce backtracking", "Unlock later traversal"),
                List.of("binary tree traversal"),
                "Move into traversal understanding",
                "LLM",
                false,
                List.of()
            ),
            List.of(new PlanReason("WEAKNESS_MATCH", "Start with the basics", "The learner still needs a stronger prerequisite foundation before moving ahead.")),
            List.of("solidify tree basics", "connect traversal to the basics"),
            List.of(
                new PlanPathNode("101", "tree basics", 1, 40, "LEARNING", true, 18, "prerequisite core"),
                new PlanPathNode("102", "binary tree traversal", 2, 55, "LEARNING", false, 18, "next focus")
            ),
            List.of(
                new PlanTaskPreview("STRUCTURE", "t1", "g1", "a1a1a1a1", "s1s1s1s1", 6),
                new PlanTaskPreview("UNDERSTANDING", "t2", "g2", "a2a2a2a2", "s2s2s2s2", 8),
                new PlanTaskPreview("TRAINING", "t3", "g3", "a3a3a3a3", "s3s3s3s3", 12),
                new PlanTaskPreview("REFLECTION", "t4", "g4", "a4a4a4a4", "s4s4s4s4", 5)
            ),
            PlanAdjustments.defaults()
        );
    }

    private ConceptNode node(Long id, String name, Integer orderNo) {
        ConceptNode node = new ConceptNode();
        node.setId(id);
        node.setName(name);
        node.setOrderNo(orderNo);
        return node;
    }

    private String write(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void assertTrueContains(String text, String expected) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected [" + text + "] to contain [" + expected + "]");
        }
    }
}
