package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlannedTaskResponse;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanSessionTasksService implements PlanSessionTasksUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TaskRepository taskRepository;
    private final TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;

    public PlanSessionTasksService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskRepository taskRepository,
        TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskRepository = taskRepository;
        this.taskObjectiveTemplateStrategy = taskObjectiveTemplateStrategy;
    }

    @Override
    public PlanSessionResponse execute(Long sessionId) {
        Long userId = UserContextHolder.getUserId();
        var session = (userId == null
            ? sessionRepository.findById(sessionId)
            : sessionRepository.findByIdAndUserPk(sessionId, userId))
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            throw new NotFoundException("No concept nodes found for chapter.");
        }

        List<Task> tasksToSave = new ArrayList<>();
        for (ConceptNode node : nodes) {
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.STRUCTURE));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.UNDERSTANDING));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.TRAINING));
            tasksToSave.add(buildTask(session.getCourseId(), session.getChapterId(), session.getGoalText(), sessionId, node, Stage.REFLECTION));
        }

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

        return new PlanSessionResponse(sessionId, responseTasks);
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
}

