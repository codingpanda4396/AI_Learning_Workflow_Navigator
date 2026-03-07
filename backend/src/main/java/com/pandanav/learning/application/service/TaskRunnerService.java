package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TaskRunnerService implements RunTaskUseCase {

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public TaskRunnerService(TaskRepository taskRepository, ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RunTaskResponse run(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        if (task.getStatus() == TaskStatus.SUCCEEDED && hasOutput(task.getOutputJson())) {
            return toResponse(task, parseOutput(task.getOutputJson()));
        }

        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ConflictException("Task is currently running.");
        }

        if (!task.canRun()) {
            throw new ConflictException("Task is not in a runnable state.");
        }

        Long attemptId = taskRepository.createRunningAttempt(taskId);
        task.markRunning();

        JsonNode generated = generateByStage(task);
        String outputJson = writeJson(generated);

        taskRepository.markAttemptSucceeded(attemptId, outputJson);
        task.markSucceeded(outputJson);

        return toResponse(task, generated);
    }

    private boolean hasOutput(String outputJson) {
        return outputJson != null && !outputJson.isBlank() && !"null".equalsIgnoreCase(outputJson.trim());
    }

    private JsonNode parseOutput(String outputJson) {
        try {
            return objectMapper.readTree(outputJson);
        } catch (Exception ex) {
            throw new InternalServerException("Stored output_json is invalid.");
        }
    }

    private String writeJson(JsonNode output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize task output.");
        }
    }

    private RunTaskResponse toResponse(Task task, JsonNode output) {
        return new RunTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            TaskStatus.SUCCEEDED.name(),
            output
        );
    }

    private JsonNode generateByStage(Task task) {
        return switch (task.getStage()) {
            case STRUCTURE -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "core concepts", "bullets", List.of("definition", "terms", "boundaries")),
                    Map.of("type", "structure", "title", "structure", "items", List.of("part A", "part B", "io relation")),
                    Map.of("type", "relations", "title", "relations", "items", List.of("prerequisite relation", "next relation")),
                    Map.of("type", "summary", "title", "summary", "text", "Build structure first, then mechanism understanding.")
                )
            ));
            case UNDERSTANDING -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "concepts", "bullets", List.of("key definition", "required condition", "trigger condition")),
                    Map.of("type", "mechanism", "title", "mechanism", "steps", List.of("step 1 input", "step 2 state transition", "step 3 output")),
                    Map.of("type", "misconceptions", "title", "misconceptions", "items", List.of("misconception 1", "misconception 2")),
                    Map.of("type", "summary", "title", "summary", "text", "Understanding causal links matters more than memorizing steps.")
                )
            ));
            case TRAINING -> objectMapper.valueToTree(Map.of(
                "questions", List.of(
                    Map.of("id", "q1", "type", "short_answer", "prompt", "Explain the core principle."),
                    Map.of("id", "q2", "type", "scenario", "prompt", "Infer state transition from the scenario.")
                ),
                "variants", List.of(
                    Map.of("name", "basic", "focus", "concept accuracy"),
                    Map.of("name", "advanced", "focus", "boundary cases")
                ),
                "rubric", Map.of(
                    "full_score", 100,
                    "dimensions", List.of(
                        Map.of("name", "concept", "weight", 40),
                        Map.of("name", "reasoning", "weight", 40),
                        Map.of("name", "clarity", "weight", 20)
                    )
                )
            ));
            case REFLECTION -> objectMapper.valueToTree(Map.of(
                "diagnosis", "Main path completed, boundary-case mastery still needs reinforcement.",
                "reflection_points", List.of("can restate concept", "can explain causal chain", "can identify misconceptions"),
                "next_steps", List.of("review wrong questions", "finish one variant set", "3-minute oral recap")
            ));
        };
    }
}

