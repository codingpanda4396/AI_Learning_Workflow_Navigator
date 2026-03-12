package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlannedNodeResponse;
import com.pandanav.learning.api.dto.session.PlannedNodeStageResponse;
import com.pandanav.learning.application.service.pathplan.PersonalizedPathPlannerService;
import com.pandanav.learning.application.service.pathplan.PersonalizedPlanResult;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.PlanMode;
import com.pandanav.learning.domain.enums.SessionStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanSessionTasksService implements PlanSessionTasksUseCase {

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

        List<Task> existingTasks = taskRepository.findBySessionIdWithStatus(sessionId);
        if (!existingTasks.isEmpty()) {
            sessionRepository.updateStatus(sessionId, SessionStatus.LEARNING);
            return buildPlanResponse(sessionId, existingTasks, nodes);
        }
        sessionRepository.updateStatus(sessionId, SessionStatus.PLANNING);

        PersonalizedPlanResult planResult = personalizedPathPlannerService.plan(session, mode, false);
        Map<String, Task> uniqueTasks = new LinkedHashMap<>();
        for (ConceptNode node : planResult.orderedNodes()) {
            putIfAbsent(uniqueTasks, buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.STRUCTURE));
            putIfAbsent(uniqueTasks, buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.UNDERSTANDING));
            putIfAbsent(uniqueTasks, buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.TRAINING));
            putIfAbsent(uniqueTasks, buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.REFLECTION));
        }
        appendInsertedTasks(sessionId, session.getCourseId(), session.getChapterId(), session.getGoalText(), nodes, planResult.insertedTasks(), uniqueTasks);

        List<Task> saved = taskRepository.saveAll(new ArrayList<>(uniqueTasks.values()));
        sessionRepository.updateStatus(sessionId, SessionStatus.LEARNING);
        return buildPlanResponse(sessionId, saved, nodes);
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
        return taskObjectiveTemplateStrategy.buildObjective(stage, conceptName);
    }

    private void appendInsertedTasks(
        Long sessionId,
        String courseId,
        String chapterId,
        String goalText,
        List<ConceptNode> chapterNodes,
        List<PersonalizedPathPlan.InsertedTask> insertedTasks,
        Map<String, Task> uniqueTasks
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

            String objective = inserted.objective() == null || inserted.objective().isBlank()
                ? buildObjectiveWithContext(courseId, chapterId, goalText, stage, conceptName)
                : inserted.objective().trim();
            String key = taskKey(inserted.nodeId(), stage);
            Task existing = uniqueTasks.get(key);
            if (existing != null) {
                if (existing.getObjective() == null || existing.getObjective().isBlank()) {
                    existing.setObjective(objective);
                }
                continue;
            }

            Task task = new Task();
            task.setSessionId(sessionId);
            task.setNodeId(inserted.nodeId());
            task.setStage(stage);
            task.setStatus(TaskStatus.PENDING);
            task.setObjective(objective);
            uniqueTasks.put(key, task);
        }
    }

    private void putIfAbsent(Map<String, Task> uniqueTasks, Task task) {
        uniqueTasks.putIfAbsent(taskKey(task.getNodeId(), task.getStage()), task);
    }

    private String taskKey(Long nodeId, Stage stage) {
        return nodeId + ":" + stage.name().toUpperCase(Locale.ROOT);
    }

    private PlanSessionResponse buildPlanResponse(Long sessionId, List<Task> tasks, List<ConceptNode> chapterNodes) {
        Map<Long, String> nodeNames = chapterNodes.stream()
            .collect(Collectors.toMap(ConceptNode::getId, ConceptNode::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> nodeOrder = new LinkedHashMap<>();
        for (int i = 0; i < chapterNodes.size(); i++) {
            nodeOrder.put(chapterNodes.get(i).getId(), i);
        }

        Map<Long, List<Task>> tasksByNode = tasks.stream()
            .collect(Collectors.groupingBy(Task::getNodeId, LinkedHashMap::new, Collectors.toList()));

        List<PlannedNodeResponse> plans = tasksByNode.entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> nodeOrder.getOrDefault(entry.getKey(), Integer.MAX_VALUE)))
            .map(entry -> {
                List<PlannedNodeStageResponse> stages = entry.getValue().stream()
                    .sorted(Comparator.comparing(task -> task.getStage().ordinal()))
                    .map(task -> new PlannedNodeStageResponse(
                        task.getId(),
                        task.getStage().name(),
                        task.getObjective(),
                        (task.getStatus() == null ? TaskStatus.PENDING : task.getStatus()).name()
                    ))
                    .toList();
                return new PlannedNodeResponse(
                    entry.getKey(),
                    nodeNames.getOrDefault(entry.getKey(), "Node-" + entry.getKey()),
                    summarizeNodeStatus(entry.getValue()),
                    stages
                );
            })
            .toList();

        return new PlanSessionResponse(sessionId, plans);
    }

    private String summarizeNodeStatus(List<Task> tasks) {
        if (tasks.stream().allMatch(task -> (task.getStatus() == null ? TaskStatus.PENDING : task.getStatus()) == TaskStatus.SUCCEEDED)) {
            return TaskStatus.SUCCEEDED.name();
        }
        if (tasks.stream().anyMatch(task -> (task.getStatus() == null ? TaskStatus.PENDING : task.getStatus()) == TaskStatus.FAILED)) {
            return TaskStatus.FAILED.name();
        }
        if (tasks.stream().anyMatch(task -> (task.getStatus() == null ? TaskStatus.PENDING : task.getStatus()) == TaskStatus.RUNNING)) {
            return TaskStatus.RUNNING.name();
        }
        return TaskStatus.PENDING.name();
    }
}

