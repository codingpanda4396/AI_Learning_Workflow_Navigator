package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.task.FeedbackResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.service.MasteryUpdateService.MasteryUpdateResult;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.AttemptLlmMetadata;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.LlmCallLog;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
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
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubmitTrainingAnswerService implements SubmitTrainingAnswerUseCase {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";
    private static final String CONFLICT_MESSAGE = "Task is not in a submittable state.";

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final EvidenceRepository evidenceRepository;
    private final LlmCallLogRepository llmCallLogRepository;
    private final AnswerEvaluator answerEvaluator;
    private final MasteryUpdateService masteryUpdateService;
    private final NextActionPolicyService nextActionPolicyService;
    private final ObjectMapper objectMapper;

    public SubmitTrainingAnswerService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        EvidenceRepository evidenceRepository,
        LlmCallLogRepository llmCallLogRepository,
        AnswerEvaluator answerEvaluator,
        MasteryUpdateService masteryUpdateService,
        NextActionPolicyService nextActionPolicyService,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.evidenceRepository = evidenceRepository;
        this.llmCallLogRepository = llmCallLogRepository;
        this.answerEvaluator = answerEvaluator;
        this.masteryUpdateService = masteryUpdateService;
        this.nextActionPolicyService = nextActionPolicyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubmitTaskResponse submit(Long taskId, SubmitTaskRequest request) {
        Long userId = UserContextHolder.getUserId();
        Task task = (userId == null
            ? taskRepository.findById(taskId)
            : taskRepository.findByIdAndUserPk(taskId, userId))
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (!task.canSubmit()) {
            throw new ConflictException(CONFLICT_MESSAGE);
        }

        LearningSession session = (userId == null
            ? sessionRepository.findById(task.getSessionId())
            : sessionRepository.findByIdAndUserPk(task.getSessionId(), userId))
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        ConceptNode node = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        EvaluationResult evaluation = answerEvaluator.evaluate(new EvaluationContext(
            task.getId(),
            task.getSessionId(),
            task.getNodeId(),
            task.getObjective(),
            task.getOutputJson(),
            request.userAnswer(),
            null,
            task.getStage()
        ));

        MasteryUpdateResult mastery = masteryUpdateService.update(session.getUserId(), task.getNodeId(), node.getName(), evaluation.score());
        List<ErrorTag> parsedErrorTags = mapErrorTags(evaluation.errorTags());
        FeedbackResponse feedbackResponse = new FeedbackResponse(
            evaluation.feedback(),
            evaluation.weaknesses()
        );

        taskRepository.createSubmissionAttempt(
            task.getId(),
            request.userAnswer(),
            evaluation.score(),
            toJson(parsedErrorTags.stream().map(Enum::name).toList()),
            toJson(Map.of(
                "diagnosis", feedbackResponse.diagnosis(),
                "fixes", feedbackResponse.fixes(),
                "rubric", evaluation.rubric(),
                "strengths", evaluation.strengths(),
                "suggested_next_action", evaluation.suggestedNextAction()
            )),
            new AttemptLlmMetadata(
                evaluation.provider(),
                evaluation.model(),
                evaluation.promptVersion(),
                evaluation.usage() == null ? null : evaluation.usage().tokenInput(),
                evaluation.usage() == null ? null : evaluation.usage().tokenOutput(),
                evaluation.usage() == null ? null : evaluation.usage().latencyMs(),
                evaluation.provider() == null ? "RULE_FALLBACK" : "LLM"
            )
        );
        if (evaluation.provider() != null) {
            llmCallLogRepository.save(new LlmCallLog(
                null,
                "TASK_SUBMIT",
                evaluation.provider(),
                evaluation.model(),
                evaluation.promptKey(),
                evaluation.promptVersion(),
                "{}",
                "{}",
                toJson(evaluation.rawJson() == null ? Map.of() : evaluation.rawJson()),
                "SUCCEEDED",
                evaluation.usage() == null ? null : evaluation.usage().latencyMs(),
                evaluation.usage() == null ? null : evaluation.usage().tokenInput(),
                evaluation.usage() == null ? null : evaluation.usage().tokenOutput()
            ));
        }

        persistEvidence(session, task, evaluation.score(), parsedErrorTags, feedbackResponse, evaluation.strengths());

        NextAction action = nextActionPolicyService.decide(evaluation.score(), parsedErrorTags);
        Task nextTask = null;

        if (action == NextAction.ADVANCE_TO_NEXT_NODE) {
            Task advanced = advanceToNextNodeIfPossible(session, task.getNodeId());
            if (advanced == null) {
                action = NextAction.NOOP;
            } else {
                nextTask = advanced;
            }
        } else if (action != NextAction.NOOP) {
            nextTask = createAdaptiveTask(
                session.getCourseId(),
                session.getChapterId(),
                session.getGoalText(),
                task.getSessionId(),
                task.getNodeId(),
                node.getName(),
                action
            );
        }

        return new SubmitTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            evaluation.score(),
            evaluation.normalizedScore(),
            parsedErrorTags.stream().map(Enum::name).toList(),
            feedbackResponse,
            evaluation.rubric(),
            evaluation.strengths(),
            evaluation.weaknesses(),
            mastery.masteryBefore(),
            mastery.masteryDelta(),
            mastery.masteryAfter(),
            action.name(),
            nextTask == null ? null : new NextTaskResponse(nextTask.getId(), nextTask.getStage().name(), nextTask.getNodeId())
        );
    }

    private void persistEvidence(
        LearningSession session,
        Task task,
        Integer score,
        List<ErrorTag> errorTags,
        FeedbackResponse feedback,
        List<String> strengths
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", session.getId());
        payload.put("task_id", task.getId());
        payload.put("node_id", task.getNodeId());
        payload.put("score", score);
        payload.put("error_tags", errorTags.stream().map(Enum::name).toList());
        payload.put("feedback_json", Map.of(
            "diagnosis", feedback.diagnosis(),
            "fixes", feedback.fixes(),
            "strengths", strengths
        ));

        Evidence evidence = new Evidence();
        evidence.setTaskId(task.getId());
        evidence.setEvidenceType("TRAINING_SUBMISSION_EVAL");
        evidence.setContentJson(toJson(payload));
        evidenceRepository.save(evidence);
    }

    private Task createAdaptiveTask(
        String courseId,
        String chapterId,
        String goalText,
        Long sessionId,
        Long nodeId,
        String conceptName,
        NextAction action
    ) {
        Stage stage = mapStage(action);
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(buildObjective(action, conceptName, courseId, chapterId, goalText));
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

    private String buildObjective(NextAction action, String conceptName, String courseId, String chapterId, String goalText) {
        String base = switch (action) {
            case INSERT_REMEDIAL_UNDERSTANDING ->
                "Remedial understanding for " + conceptName + ": explain mechanism and correct misconceptions.";
            case INSERT_TRAINING_VARIANTS ->
                "Variant training for " + conceptName + ": solve 3 new scenario-based questions.";
            case INSERT_TRAINING_REINFORCEMENT ->
                "Reinforcement training for " + conceptName + ": complete one advanced mixed-case drill.";
            default -> "";
        };
        String goalPart = (goalText == null || goalText.isBlank()) ? "N/A" : goalText;
        return base
            + " [Course: " + courseId + "]"
            + " [Chapter: " + chapterId + "]"
            + " [Goal: " + goalPart + "]";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize submission payload.");
        }
    }

    private List<ErrorTag> mapErrorTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = java.util.Arrays.stream(ErrorTag.values()).map(Enum::name).collect(Collectors.toSet());
        return tags.stream()
            .map(v -> v == null ? "" : v.trim().toUpperCase(Locale.ROOT))
            .filter(allowed::contains)
            .map(ErrorTag::valueOf)
            .toList();
    }
}

