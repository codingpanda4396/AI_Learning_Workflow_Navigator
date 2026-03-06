package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlannedTaskResponse;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Stage;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TaskStatus;
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

    public PlanSessionTasksService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskRepository taskRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public PlanSessionResponse execute(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            throw new NotFoundException("No concept nodes found for chapter.");
        }

        List<Task> tasksToSave = new ArrayList<>();
        for (ConceptNode node : nodes) {
            tasksToSave.add(buildTask(sessionId, node, Stage.STRUCTURE));
            tasksToSave.add(buildTask(sessionId, node, Stage.UNDERSTANDING));
            tasksToSave.add(buildTask(sessionId, node, Stage.TRAINING));
            tasksToSave.add(buildTask(sessionId, node, Stage.REFLECTION));
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

    private Task buildTask(Long sessionId, ConceptNode node, Stage stage) {
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(node.getId());
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(objective(stage, node.getName()));
        return task;
    }

    private String objective(Stage stage, String conceptName) {
        return switch (stage) {
            case STRUCTURE -> "为【" + conceptName + "】构建结构：定义 / 组成 / 关键机制 / 与上下文关系";
            case UNDERSTANDING -> "解释【" + conceptName + "】机制链路：核心原理、因果关系、常见误区";
            case TRAINING -> "围绕【" + conceptName + "】生成训练任务，用于检测掌握度";
            case REFLECTION -> "基于【" + conceptName + "】学习结果总结错因并给出下一步建议";
        };
    }
}
