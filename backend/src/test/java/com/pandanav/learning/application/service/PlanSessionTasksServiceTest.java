package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.application.service.pathplan.PersonalizedPathPlannerService;
import com.pandanav.learning.application.service.pathplan.PersonalizedPlanResult;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.PlanMode;
import com.pandanav.learning.domain.enums.PlanSource;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanSessionTasksServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;
    @Mock
    private PersonalizedPathPlannerService personalizedPathPlannerService;
    @Mock
    private PlanInstanceService planInstanceService;

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void shouldReturnExistingTasksWithoutCreatingDuplicates() {
        Long sessionId = 10L;
        LearningSession session = session(sessionId);
        Task existing = task(1001L, sessionId, 101L, Stage.TRAINING, TaskStatus.PENDING, "existing");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch-1")).thenReturn(List.of(node(101L, "Node-101", 1)));
        when(taskRepository.findBySessionIdWithStatus(sessionId)).thenReturn(List.of(existing));

        PlanSessionTasksService service = new PlanSessionTasksService(
            sessionRepository,
            conceptNodeRepository,
            taskRepository,
            taskObjectiveTemplateStrategy,
            personalizedPathPlannerService,
            planInstanceService
        );

        PlanSessionResponse response = service.execute(sessionId, PlanMode.AUTO);

        assertEquals(1, response.plans().size());
        assertEquals(101L, response.plans().get(0).nodeId());
        assertEquals("PENDING", response.plans().get(0).status());
        assertEquals(1, response.plans().get(0).stages().size());
        assertEquals(1001L, response.plans().get(0).stages().get(0).taskId());
        verify(personalizedPathPlannerService, never()).plan(any(), any(), anyBoolean());
        verify(taskRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldCreateTasksWhenSessionHasNoExistingTasks() {
        Long sessionId = 20L;
        LearningSession session = session(sessionId);
        ConceptNode node = node(101L, "Node-101", 1);
        Task saved = task(2001L, sessionId, 101L, Stage.STRUCTURE, TaskStatus.PENDING, "objective");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(taskRepository.findBySessionIdWithStatus(sessionId)).thenReturn(List.of());
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch-1")).thenReturn(List.of(node));
        when(personalizedPathPlannerService.plan(any(), any(), anyBoolean())).thenReturn(
            new PersonalizedPlanResult(
                PlanSource.RULE,
                List.of(node),
                List.of(),
                "",
                List.of(),
                List.of(node.getId()),
                null,
                null,
                null,
                null,
                false,
                List.of(),
                false
            )
        );
        when(taskObjectiveTemplateStrategy.buildObjective(any(), any())).thenReturn("objective");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(saved));

        PlanSessionTasksService service = new PlanSessionTasksService(
            sessionRepository,
            conceptNodeRepository,
            taskRepository,
            taskObjectiveTemplateStrategy,
            personalizedPathPlannerService,
            planInstanceService
        );

        PlanSessionResponse response = service.execute(sessionId, PlanMode.AUTO);

        assertEquals(1, response.plans().size());
        assertEquals(101L, response.plans().get(0).nodeId());
        assertEquals(1, response.plans().get(0).stages().size());
        assertEquals(2001L, response.plans().get(0).stages().get(0).taskId());
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    void shouldDeduplicateDuplicateNodeStageTasksBeforeSaving() {
        Long sessionId = 30L;
        LearningSession session = session(sessionId);
        ConceptNode node1 = node(101L, "Node-101", 1);
        ConceptNode node2 = node(102L, "Node-102", 2);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(taskRepository.findBySessionIdWithStatus(sessionId)).thenReturn(List.of());
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch-1")).thenReturn(List.of(node1, node2));
        when(personalizedPathPlannerService.plan(any(), any(), anyBoolean())).thenReturn(
            new PersonalizedPlanResult(
                PlanSource.RULE,
                List.of(node1, node1, node2),
                List.of(
                    new com.pandanav.learning.domain.llm.model.PersonalizedPathPlan.InsertedTask(
                        101L,
                        Stage.UNDERSTANDING.name(),
                        "duplicate inserted understanding objective",
                        "MISSING_STEPS"
                    ),
                    new com.pandanav.learning.domain.llm.model.PersonalizedPathPlan.InsertedTask(
                        102L,
                        Stage.TRAINING.name(),
                        "duplicate inserted training objective",
                        "CONCEPT_CONFUSION"
                    )
                ),
                "",
                List.of(),
                List.of(node1.getId()),
                null,
                null,
                null,
                null,
                false,
                List.of(),
                false
            )
        );
        when(taskObjectiveTemplateStrategy.buildObjective(any(), any())).thenReturn("objective");
        when(taskRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanSessionTasksService service = new PlanSessionTasksService(
            sessionRepository,
            conceptNodeRepository,
            taskRepository,
            taskObjectiveTemplateStrategy,
            personalizedPathPlannerService,
            planInstanceService
        );

        PlanSessionResponse response = service.execute(sessionId, PlanMode.AUTO);

        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Task> savedTasks = captor.getValue();

        assertEquals(8, savedTasks.size());
        assertEquals(2, response.plans().size());
        assertEquals(4, response.plans().get(0).stages().size());
        assertEquals(4, response.plans().get(1).stages().size());
        assertEquals(1L, savedTasks.stream().filter(task -> task.getNodeId().equals(101L) && task.getStage() == Stage.UNDERSTANDING).count());
        assertEquals(1L, savedTasks.stream().filter(task -> task.getNodeId().equals(102L) && task.getStage() == Stage.TRAINING).count());
        assertTrue(savedTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.PENDING));
    }

    private static LearningSession session(Long id) {
        LearningSession session = new LearningSession();
        session.setId(id);
        session.setUserId("u-1");
        session.setCourseId("course-1");
        session.setChapterId("ch-1");
        session.setGoalText("goal");
        return session;
    }

    private static ConceptNode node(Long id, String name, int orderNo) {
        ConceptNode node = new ConceptNode();
        node.setId(id);
        node.setName(name);
        node.setOrderNo(orderNo);
        return node;
    }

    private static Task task(Long id, Long sessionId, Long nodeId, Stage stage, TaskStatus status, String objective) {
        Task task = new Task();
        task.setId(id);
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(status);
        task.setObjective(objective);
        return task;
    }
}
