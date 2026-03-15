package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.AdvanceStepRequest;
import com.pandanav.learning.api.dto.task.AdvanceStepResponse;
import com.pandanav.learning.api.dto.task.LearningStepResponse;
import com.pandanav.learning.application.usecase.AdvanceLearningStepUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.model.CompletionRule;
import com.pandanav.learning.domain.model.LearningStep;
import com.pandanav.learning.domain.repository.LearningStepRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdvanceLearningStepService implements AdvanceLearningStepUseCase {

    private final TaskRepository taskRepository;
    private final LearningStepRepository learningStepRepository;

    public AdvanceLearningStepService(TaskRepository taskRepository, LearningStepRepository learningStepRepository) {
        this.taskRepository = taskRepository;
        this.learningStepRepository = learningStepRepository;
    }

    @Override
    public AdvanceStepResponse advance(Long taskId, Long stepId, AdvanceStepRequest request) {
        Long userId = UserContextHolder.getUserId();
        boolean taskExists = (userId == null
            ? taskRepository.findById(taskId)
            : taskRepository.findByIdAndUserPk(taskId, userId)).isPresent();
        if (!taskExists) {
            throw new NotFoundException("Session or task not found.");
        }

        LearningStep step = learningStepRepository.findByIdAndTaskId(stepId, taskId)
            .orElseThrow(() -> new NotFoundException("Learning step not found."));
        if (step.getStatus() != LearningStepStatus.ACTIVE) {
            throw new ConflictException("Only ACTIVE step can be advanced.");
        }

        LearningStepStatus target = parseTarget(request.action());
        if (target == LearningStepStatus.DONE) {
            validateCompletionRule(step.getCompletionRule(), request);
            step.markDone();
        } else if (target == LearningStepStatus.FAILED) {
            step.markFailed();
        } else {
            step.markSkipped();
        }
        learningStepRepository.updateStatus(step.getId(), step.getStatus());

        LearningStep nextStep = activateNextTodoStep(taskId);
        LearningStepResponse nextStepResponse = nextStep == null ? null : toStepResponse(nextStep);
        return new AdvanceStepResponse(
            taskId,
            toStepResponse(step),
            nextStepResponse,
            nextStepResponse == null ? "该任务步骤已结束。" : "继续下一步：" + nextStepResponse.objective()
        );
    }

    private LearningStep activateNextTodoStep(Long taskId) {
        return learningStepRepository.findByTaskIdOrderByStepOrder(taskId).stream()
            .filter(s -> s.getStatus() == LearningStepStatus.TODO)
            .findFirst()
            .map(step -> {
                step.activate();
                learningStepRepository.updateStatus(step.getId(), step.getStatus());
                return step;
            })
            .orElse(null);
    }

    private void validateCompletionRule(CompletionRule rule, AdvanceStepRequest request) {
        if (rule == null) {
            return;
        }
        int attempts = request.attemptCount() == null ? 1 : request.attemptCount();
        Integer threshold = rule.threshold();
        if (threshold != null && attempts < threshold) {
            throw new ConflictException("Current attempts do not satisfy completion threshold.");
        }
        Integer maxAttempts = rule.maxAttempts();
        if (maxAttempts != null && attempts > maxAttempts) {
            throw new ConflictException("Attempt count exceeds maxAttempts.");
        }
        List<String> required = rule.requiredEvidenceTypes();
        if (required == null || required.isEmpty()) {
            return;
        }
        Set<String> incoming = request.evidenceTypes() == null
            ? Set.of()
            : request.evidenceTypes().stream()
            .filter(v -> v != null && !v.isBlank())
            .map(v -> v.trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
        boolean missingRequired = required.stream()
            .map(v -> v == null ? "" : v.trim().toUpperCase(Locale.ROOT))
            .anyMatch(v -> !incoming.contains(v));
        if (missingRequired) {
            throw new ConflictException("Required evidence types are missing.");
        }
    }

    private LearningStepStatus parseTarget(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DONE", "COMPLETE", "COMPLETED" -> LearningStepStatus.DONE;
            case "FAILED", "FAIL" -> LearningStepStatus.FAILED;
            case "SKIPPED", "SKIP" -> LearningStepStatus.SKIPPED;
            default -> throw new ConflictException("Unsupported step action: " + action);
        };
    }

    private LearningStepResponse toStepResponse(LearningStep step) {
        return new LearningStepResponse(
            step.getId(),
            step.getTaskId(),
            step.getStage().name(),
            step.getType(),
            step.getStepOrder(),
            step.getStatus().name(),
            step.getObjective(),
            step.getCompletionRule()
        );
    }
}

