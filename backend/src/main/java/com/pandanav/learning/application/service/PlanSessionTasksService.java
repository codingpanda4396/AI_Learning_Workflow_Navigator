package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlanPreviewResponse;
import com.pandanav.learning.api.dto.session.PlannedTaskResponse;
import com.pandanav.learning.application.service.pathplan.PersonalizedPathPlannerService;
import com.pandanav.learning.application.service.pathplan.PersonalizedPlanResult;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.application.usecase.PreviewSessionPlanUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.PlanMode;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.llm.model.PersonalizedPathPlan;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class PlanSessionTasksService implements PlanSessionTasksUseCase, PreviewSessionPlanUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TaskRepository taskRepository;
    private final TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;
    private final PersonalizedPathPlannerService personalizedPathPlannerService;

    public PlanSessionTasksService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskRepository taskRepository,
        TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy,
        PersonalizedPathPlannerService personalizedPathPlannerService
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskRepository = taskRepository;
        this.taskObjectiveTemplateStrategy = taskObjectiveTemplateStrategy;
        this.personalizedPathPlannerService = personalizedPathPlannerService;
    }

    @Override
    public PlanSessionResponse execute(Long sessionId, PlanMode mode) {
        Long userId = UserContextHolder.getUserId();
        var session = (userId == null
            ? sessionRepository.findById(sessionId)
            : sessionRepository.findByIdAndUserPk(sessionId, userId))
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            throw new NotFoundException("No concept nodes found for chapter.");
        }

        PersonalizedPlanResult planResult = personalizedPathPlannerService.plan(session, mode, false);
        List<Task> tasksToSave = new ArrayList<>();
        for (ConceptNode node : planResult.orderedNodes()) {
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.STRUCTURE));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.UNDERSTANDING));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.TRAINING));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.REFLECTION));
        }
        appendInsertedTasks(sessionId, session.getCourseId(), session.getChapterId(), session.getGoalText(), nodes, planResult.insertedTasks(), tasksToSave);

        List<Task> saved = taskRepository.saveAll(tasksToSave);
        List<PlannedTaskResponse> responseTasks = saved.stream()
            .map(task -> new PlannedTaskResponse(
                task.getId(),
                task.getStage().name(),
                task.getNodeId(),
                task.getObjective(),
                TaskStatus.PENDING.name()
            ))
            .toList();

        return new PlanSessionResponse(
            sessionId,
            responseTasks,
            planResult.source().name(),
            planResult.planReasoningSummary(),
            planResult.riskFlags()
        );
    }

    @Override
    public PlanPreviewResponse preview(Long sessionId, PlanMode mode) {
        Long userId = UserContextHolder.getUserId();
        var session = (userId == null
            ? sessionRepository.findById(sessionId)
            : sessionRepository.findByIdAndUserPk(sessionId, userId))
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        PersonalizedPlanResult planResult = personalizedPathPlannerService.plan(session, mode, true);
        return new PlanPreviewResponse(
            sessionId,
            planResult.source().name(),
            planResult.planReasoningSummary(),
            planResult.riskFlags()
        );
    }

    private Task buildTask(String courseId, String chapterId, String goalText, Long sessionId, ConceptNode node, Stage stage) {
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(node.getId());
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(buildObjectiveWithContext(courseId, chapterId, goalText, stage, node.getName()));
        return task;
    }

    private String buildObjectiveWithContext(String courseId, String chapterId, String goalText, Stage stage, String conceptName) {
        String base = taskObjectiveTemplateStrategy.buildObjective(stage, conceptName);
        String goalPart = (goalText == null || goalText.isBlank()) ? "N/A" : goalText;
        return base
            + " [Course: " + courseId + "]"
            + " [Chapter: " + chapterId + "]"
            + " [Goal: " + goalPart + "]";
    }

    private void appendInsertedTasks(
        Long sessionId,
        String courseId,
        String chapterId,
        String goalText,
        List<ConceptNode> chapterNodes,
        List<PersonalizedPathPlan.InsertedTask> insertedTasks,
        List<Task> tasksToSave
    ) {
        if (insertedTasks == null || insertedTasks.isEmpty()) {
            return;
        }
        Set<Long> chapterNodeIds = chapterNodes.stream().map(ConceptNode::getId).collect(java.util.stream.Collectors.toSet());
        for (PersonalizedPathPlan.InsertedTask inserted : insertedTasks.stream().limit(3).toList()) {
            if (!chapterNodeIds.contains(inserted.nodeId())) {
                continue;
            }
            Stage stage;
            try {
                stage = Stage.valueOf(inserted.stage().trim().toUpperCase());
            } catch (Exception ex) {
                continue;
            }
            if (stage != Stage.UNDERSTANDING && stage != Stage.TRAINING) {
                continue;
            }
            String conceptName = chapterNodes.stream()
                .filter(node -> node.getId().equals(inserted.nodeId()))
                .min(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
                .map(ConceptNode::getName)
                .orElse("concept");

            Task task = new Task();
            task.setSessionId(sessionId);
            task.setNodeId(inserted.nodeId());
            task.setStage(stage);
            task.setStatus(TaskStatus.PENDING);
            String objective = inserted.objective() == null || inserted.objective().isBlank()
                ? buildObjectiveWithContext(courseId, chapterId, goalText, stage, conceptName)
                : inserted.objective().trim() + " [PersonalizedTrigger: " + inserted.trigger() + "]";
            task.setObjective(objective);
            tasksToSave.add(task);
        }
    }
}

