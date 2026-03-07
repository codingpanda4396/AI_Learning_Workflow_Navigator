package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.service.EvaluatorService.EvaluationResult;
import com.pandanav.learning.application.service.MasteryUpdateService.MasteryUpdateResult;
import com.pandanav.learning.application.service.NextActionPolicyService.NextAction;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Stage;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TaskStatus;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubmitTrainingAnswerService implements SubmitTrainingAnswerUseCase {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";
    private static final String CONFLICT_MESSAGE = "Task is not in a submittable state.";

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvaluatorService evaluatorService;
    private final MasteryUpdateService masteryUpdateService;
    private final NextActionPolicyService nextActionPolicyService;
    private final ObjectMapper objectMapper;

    public SubmitTrainingAnswerService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        EvidenceRepository evidenceRepository,
        EvaluatorService evaluatorService,
        MasteryUpdateService masteryUpdateService,
        NextActionPolicyService nextActionPolicyService,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.evidenceRepository = evidenceRepository;
        this.evaluatorService = evaluatorService;
        this.masteryUpdateService = masteryUpdateService;
        this.nextActionPolicyService = nextActionPolicyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubmitTaskResponse submit(Long taskId, SubmitTaskRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (task.getStage() != Stage.TRAINING) {
            throw new ConflictException(CONFLICT_MESSAGE);
        }

        if (!isSubmittable(task.getStatus())) {
            throw new ConflictException(CONFLICT_MESSAGE);
        }

        LearningSession session = sessionRepository.findById(task.getSessionId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        ConceptNode node = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        EvaluationResult evaluation = evaluatorService.evaluate(node.getName(), task.getObjective(), request.userAnswer());
        MasteryUpdateResult mastery = masteryUpdateService.update(session.getUserId(), task.getNodeId(), node.getName(), evaluation.score());

        persistEvidence(session, task, evaluation);

        NextAction action = nextActionPolicyService.decide(evaluation.score(), evaluation.errorTags());
        Task nextTask = null;

        if (action == NextAction.ADVANCE_TO_NEXT_NODE) {
            Task advanced = advanceToNextNodeIfPossible(session, task.getNodeId());
            if (advanced == null) {
                action = NextAction.NOOP;
            } else {
                nextTask = advanced;
            }
        } else if (action != NextAction.NOOP) {
            nextTask = createAdaptiveTask(task.getSessionId(), task.getNodeId(), node.getName(), action);
        }

        return new SubmitTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            evaluation.score(),
            evaluation.errorTags(),
            evaluation.feedback(),
            mastery.masteryBefore(),
            mastery.masteryDelta(),
            mastery.masteryAfter(),
            action.name(),
            nextTask == null ? null : new NextTaskResponse(nextTask.getId(), nextTask.getStage().name(), nextTask.getNodeId())
        );
    }

    private boolean isSubmittable(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED || status == TaskStatus.PENDING;
    }

    private void persistEvidence(LearningSession session, Task task, EvaluationResult evaluation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", session.getId());
        payload.put("task_id", task.getId());
        payload.put("node_id", task.getNodeId());
        payload.put("score", evaluation.score());
        payload.put("error_tags", evaluation.errorTags());
        payload.put("feedback_json", Map.of(
            "diagnosis", evaluation.feedback().diagnosis(),
            "fixes", evaluation.feedback().fixes()
        ));

        Evidence evidence = new Evidence();
        evidence.setTaskId(task.getId());
        evidence.setEvidenceType("TRAINING_SUBMISSION_EVAL");
        evidence.setContentJson(toJson(payload));
        evidenceRepository.save(evidence);
    }

    private Task createAdaptiveTask(Long sessionId, Long nodeId, String conceptName, NextAction action) {
        Stage stage = mapStage(action);
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(buildObjective(action, conceptName));
        return taskRepository.save(task);
    }

    private Task advanceToNextNodeIfPossible(LearningSession session, Long currentNodeId) {
        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            return null;
        }

        int currentIndex = -1;
        Long anchorNodeId = session.getCurrentNodeId() != null ? session.getCurrentNodeId() : currentNodeId;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(anchorNodeId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0 || currentIndex + 1 >= nodes.size()) {
            return null;
        }

        ConceptNode nextNode = nodes.get(currentIndex + 1);
        sessionRepository.updateCurrentPosition(session.getId(), nextNode.getId(), Stage.STRUCTURE);

        return taskRepository.findFirstBySessionIdAndNodeIdAndStage(session.getId(), nextNode.getId(), Stage.STRUCTURE)
            .orElseGet(() -> {
                Task firstTask = new Task();
                firstTask.setSessionId(session.getId());
                firstTask.setNodeId(nextNode.getId());
                firstTask.setStage(Stage.STRUCTURE);
                firstTask.setStatus(TaskStatus.PENDING);
                firstTask.setObjective("Build structure map for concept: " + nextNode.getName());
                return taskRepository.save(firstTask);
            });
    }

    private Stage mapStage(NextAction action) {
        return switch (action) {
            case INSERT_REMEDIAL_UNDERSTANDING -> Stage.UNDERSTANDING;
            case INSERT_TRAINING_VARIANTS, INSERT_TRAINING_REINFORCEMENT -> Stage.TRAINING;
            default -> throw new IllegalArgumentException("Unsupported action for task creation: " + action);
        };
    }

    private String buildObjective(NextAction action, String conceptName) {
        return switch (action) {
            case INSERT_REMEDIAL_UNDERSTANDING ->
                "Remedial understanding for " + conceptName + ": explain mechanism and correct misconceptions.";
            case INSERT_TRAINING_VARIANTS ->
                "Variant training for " + conceptName + ": solve 3 new scenario-based questions.";
            case INSERT_TRAINING_REINFORCEMENT ->
                "Reinforcement training for " + conceptName + ": complete one advanced mixed-case drill.";
            default -> "";
        };
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize evidence payload.");
        }
    }
}
