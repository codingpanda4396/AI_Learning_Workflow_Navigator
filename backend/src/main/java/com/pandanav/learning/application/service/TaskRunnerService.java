package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.llm.model.StageContent;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.AttemptLlmMetadata;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.LlmCallLog;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TaskRunnerService implements RunTaskUseCase {

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;
    private final StageContentGenerator stageContentGenerator;
    private final LlmCallLogRepository llmCallLogRepository;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public TaskRunnerService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository,
        StageContentGenerator stageContentGenerator,
        LlmCallLogRepository llmCallLogRepository,
        LlmProperties llmProperties,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
        this.stageContentGenerator = stageContentGenerator;
        this.llmCallLogRepository = llmCallLogRepository;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public RunTaskResponse run(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        if (task.getStatus() == TaskStatus.SUCCEEDED && hasOutput(task.getOutputJson())) {
            return toResponse(task, "CACHED", parseOutput(task.getOutputJson()));
        }

        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ConflictException("Task is currently running.");
        }

        if (!task.canRun()) {
            throw new ConflictException("Task is not in a runnable state.");
        }

        Long attemptId = taskRepository.createRunningAttempt(taskId);
        task.markRunning();

        JsonNode generated;
        AttemptLlmMetadata metadata;
        try {
            StageContent stageContent = generateByLlm(task);
            generated = stageContent.content();
            metadata = new AttemptLlmMetadata(
                stageContent.provider(),
                stageContent.model(),
                stageContent.promptVersion(),
                stageContent.usage() == null ? null : stageContent.usage().tokenInput(),
                stageContent.usage() == null ? null : stageContent.usage().tokenOutput(),
                stageContent.usage() == null ? null : stageContent.usage().latencyMs(),
                stageContent.generationMode()
            );
        } catch (Exception ex) {
            if (!llmProperties.isFallbackToRule()) {
                taskRepository.markAttemptFailed(attemptId, ex.getMessage(), AttemptLlmMetadata.none("LLM"));
                llmCallLogRepository.save(new LlmCallLog(
                    attemptId, "TASK_RUN", llmProperties.getProvider(), llmProperties.getModel(),
                    task.getStage().name() + "_PROMPT_V1", "v1", "{}", "{\"error\":\"" + ex.getMessage() + "\"}",
                    "{}", "FAILED", null, null, null
                ));
                throw ex;
            }
            generated = generateByStage(task);
            metadata = AttemptLlmMetadata.none("TEMPLATE_FALLBACK");
        }

        String outputJson = writeJson(generated);

        taskRepository.markAttemptSucceeded(attemptId, outputJson, metadata);
        if ("LLM".equals(metadata.generationMode())) {
            llmCallLogRepository.save(new LlmCallLog(
                attemptId,
                "TASK_RUN",
                metadata.llmProvider(),
                metadata.llmModel(),
                task.getStage().name() + "_PROMPT_V1",
                metadata.promptVersion(),
                "{}",
                "{}",
                outputJson,
                "SUCCEEDED",
                metadata.latencyMs(),
                metadata.tokenInput(),
                metadata.tokenOutput()
            ));
        }
        task.markSucceeded(outputJson);

        return toResponse(task, metadata.generationMode(), generated);
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

    private RunTaskResponse toResponse(Task task, String generationMode, JsonNode output) {
        return new RunTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            TaskStatus.SUCCEEDED.name(),
            generationMode,
            output
        );
    }

    private StageContent generateByLlm(Task task) {
        if (!llmProperties.isReady() || !llmProperties.isEnabled()) {
            throw new InternalServerException("LLM is disabled.");
        }
        LearningSession session = sessionRepository.findById(task.getSessionId())
            .orElseThrow(() -> new NotFoundException("Session or task not found."));
        ConceptNode node = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException("Session or task not found."));
        Mastery mastery = masteryRepository.findByUserIdAndNodeId(session.getUserId(), node.getId()).orElse(null);
        StageGenerationContext context = new StageGenerationContext(
            task.getId(),
            task.getSessionId(),
            session.getChapterId(),
            node.getId(),
            node.getName(),
            task.getStage(),
            task.getObjective(),
            null,
            mastery == null ? null : mastery.getMasteryValue()
        );
        return stageContentGenerator.generate(context);
    }

    private JsonNode generateByStage(Task task) {
        return switch (task.getStage()) {
            case STRUCTURE -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "核心概念", "bullets", List.of("定义", "关键术语", "适用边界")),
                    Map.of("type", "structure", "title", "结构拆解", "items", List.of("核心组成 A", "核心组成 B", "输入输出关系")),
                    Map.of("type", "relations", "title", "关联关系", "items", List.of("前置知识依赖", "后续延伸方向")),
                    Map.of("type", "summary", "title", "总结", "text", "先建立结构框架，再进入机制理解与训练。")
                )
            ));
            case UNDERSTANDING -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "关键知识点", "bullets", List.of("核心定义", "必要条件", "触发条件")),
                    Map.of("type", "mechanism", "title", "机制流程", "steps", List.of("步骤一：输入条件", "步骤二：状态变化", "步骤三：输出结果")),
                    Map.of("type", "misconceptions", "title", "常见误区", "items", List.of("误区一：概念混淆", "误区二：忽略边界条件")),
                    Map.of("type", "summary", "title", "总结", "text", "理解因果链路比死记步骤更重要。")
                )
            ));
            case TRAINING -> objectMapper.valueToTree(Map.of(
                "questions", List.of(
                    Map.of("id", "q1", "type", "short_answer", "prompt", "请解释该知识点的核心原理。"),
                    Map.of("id", "q2", "type", "scenario", "prompt", "根据给定场景推导关键状态变化。")
                ),
                "variants", List.of(
                    Map.of("name", "基础版", "focus", "概念准确性"),
                    Map.of("name", "进阶版", "focus", "边界条件处理")
                ),
                "rubric", Map.of(
                    "full_score", 100,
                    "dimensions", List.of(
                        Map.of("name", "概念理解", "weight", 40),
                        Map.of("name", "推理过程", "weight", 40),
                        Map.of("name", "表达清晰度", "weight", 20)
                    )
                )
            ));
            case REFLECTION -> objectMapper.valueToTree(Map.of(
                "diagnosis", "主路径已完成，但边界场景掌握仍需加强。",
                "reflection_points", List.of("是否能复述核心概念", "是否能解释因果链路", "是否能识别常见误区"),
                "next_steps", List.of("复盘错题", "完成一组变式训练", "进行 3 分钟口头总结")
            ));
        };
    }
}

