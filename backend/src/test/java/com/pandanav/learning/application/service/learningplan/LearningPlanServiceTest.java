package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.application.command.ConfirmLearningPlanCommand;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.domain.enums.LearningPlanStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningPlan;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningPlanRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
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
        when(orchestrator.preview(any())).thenReturn(new LearningPlanOrchestrator.OrchestratedPlan(samplePreview(), null, PlanSource.LLM, false, List.of()));
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

        assertEquals("88", response.previewId());
        assertEquals("LLM", response.contentSource().code());
        assertEquals("tree basics", response.summary().recommendedStartNode().nodeName());
        assertNotNull(response.whyStartHere());
        assertEquals(2, response.priorityNodes().size());
        assertNotNull(response.keyWeaknesses());
    }

    @Test
    void shouldConfirmLearningPlanIntoSessionAndTasks() throws Exception {
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
        stored.setSummaryJson(objectMapper.writeValueAsString(samplePreview().summary()));
        stored.setReasonsJson(objectMapper.writeValueAsString(samplePreview().reasons()));
        stored.setFocusesJson(objectMapper.writeValueAsString(samplePreview().focuses()));
        stored.setPathPreviewJson(objectMapper.writeValueAsString(samplePreview().pathPreview()));
        stored.setTaskPreviewJson(objectMapper.writeValueAsString(samplePreview().taskPreview()));
        stored.setAdjustmentsJson(objectMapper.writeValueAsString(samplePreview().adjustments()));
        stored.setPlanningContextJson(objectMapper.writeValueAsString(sampleContext()));

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
            new LearningPlanExplanationAssembler(),
            sessionRepository,
            taskRepository,
            conceptNodeRepository,
            objectiveStrategy(),
            objectMapper
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
            new LearningPlanExplanationAssembler(),
            mock(SessionRepository.class),
            mock(TaskRepository.class),
            mock(ConceptNodeRepository.class),
            objectiveStrategy(),
            new ObjectMapper()
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
            PlanAdjustments.defaults()
        );
    }

    private LearningPlanPreview samplePreview() {
        return new LearningPlanPreview(
            new LearningPlanSummary("Strengthen basics before advancing", "101", "tree basics", "STANDARD", 36, 2, 4),
            List.of(new PlanReason("START_POINT", "Start with the basics", "The learner still needs a stronger prerequisite foundation before moving ahead.")),
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
}
