package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.domain.model.Stage;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TaskStatus;
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

        if (task.getStatus() != TaskStatus.PENDING
            && task.getStatus() != TaskStatus.FAILED
            && task.getStatus() != TaskStatus.CANCELLED) {
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
                    Map.of("type", "concepts", "title", "核心概念", "bullets", List.of("定义", "关键术语", "边界条件")),
                    Map.of("type", "structure", "title", "结构组成", "items", List.of("组成部分 A", "组成部分 B", "输入输出关系")),
                    Map.of("type", "relations", "title", "上下文关系", "items", List.of("与前置知识的关系", "与后续知识的关系")),
                    Map.of("type", "summary", "title", "总结", "text", "先形成结构化认知，再进入机制理解。")
                )
            ));
            case UNDERSTANDING -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "核心概念", "bullets", List.of("关键定义", "必要条件", "触发条件")),
                    Map.of("type", "mechanism", "title", "机制链路", "steps", List.of("步骤1：输入与前提", "步骤2：状态变化", "步骤3：输出与结果")),
                    Map.of("type", "misconceptions", "title", "常见误区", "items", List.of("误区1：只记结论", "误区2：忽略边界情况")),
                    Map.of("type", "summary", "title", "总结", "text", "理解因果链路比背诵步骤更重要。")
                )
            ));
            case TRAINING -> objectMapper.valueToTree(Map.of(
                "questions", List.of(
                    Map.of("id", "q1", "type", "short_answer", "prompt", "解释该机制的核心原理。"),
                    Map.of("id", "q2", "type", "scenario", "prompt", "给定场景下推导关键状态变化。")
                ),
                "variants", List.of(
                    Map.of("name", "基础版", "focus", "概念准确性"),
                    Map.of("name", "进阶版", "focus", "边界条件与反例")
                ),
                "rubric", Map.of(
                    "full_score", 100,
                    "dimensions", List.of(
                        Map.of("name", "概念准确", "weight", 40),
                        Map.of("name", "机制推理", "weight", 40),
                        Map.of("name", "表达清晰", "weight", 20)
                    )
                )
            ));
            case REFLECTION -> objectMapper.valueToTree(Map.of(
                "diagnosis", "本轮学习已覆盖主路径，但对边界条件掌握仍需加强。",
                "reflection_points", List.of("概念是否能用自己的话复述", "是否能解释关键因果关系", "是否能识别常见误区"),
                "next_steps", List.of("复盘错题对应知识点", "完成一组变式训练", "用 3 分钟做口述总结")
            ));
        };
    }
}
