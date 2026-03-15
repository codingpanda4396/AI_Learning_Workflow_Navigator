package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.LearningStepResponse;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.StageContent;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.model.AttemptLlmMetadata;
import com.pandanav.learning.domain.model.CompletionRule;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningStep;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.LlmCallLog;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningStepRepository;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskRunnerService implements RunTaskUseCase {

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerService.class);

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;
    private final LearningStepRepository learningStepRepository;
    private final StageContentGenerator stageContentGenerator;
    private final LlmCallLogRepository llmCallLogRepository;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier llmFailureClassifier;

    public TaskRunnerService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository,
        LearningStepRepository learningStepRepository,
        StageContentGenerator stageContentGenerator,
        LlmCallLogRepository llmCallLogRepository,
        LlmProperties llmProperties,
        ObjectMapper objectMapper,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
        this.learningStepRepository = learningStepRepository;
        this.stageContentGenerator = stageContentGenerator;
        this.llmCallLogRepository = llmCallLogRepository;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.llmCallLogger = llmCallLogger;
        this.llmFailureClassifier = llmFailureClassifier;
    }

    @Override
    public RunTaskResponse run(Long taskId) {
        Long userId = UserContextHolder.getUserId();
        Task task = (userId == null
            ? taskRepository.findById(taskId)
            : taskRepository.findByIdAndUserPk(taskId, userId))
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        if (task.getStatus() == TaskStatus.SUCCEEDED && hasOutput(task.getOutputJson())) {
            JsonNode cachedOutput = parseOutput(task.getOutputJson());
            List<LearningStep> steps = ensureLearningSteps(task, cachedOutput);
            return toResponse(task, "CACHED", "Output loaded from existing successful attempt.", cachedOutput, steps);
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
        StageContent llmStageContent = null;
        String promptKey = null;
        String generationReason;
        boolean fallbackUsed = false;
        try {
            llmStageContent = generateByLlm(task, userId);
            enforceLightJsonGuardrail(llmStageContent);
            generated = llmStageContent.content();
            promptKey = llmStageContent.promptKey();
            metadata = new AttemptLlmMetadata(
                llmStageContent.provider(),
                llmStageContent.model(),
                llmStageContent.promptVersion(),
                llmStageContent.invocationProfile().name(),
                llmStageContent.usage() == null ? null : llmStageContent.usage().tokenInput(),
                llmStageContent.usage() == null ? null : llmStageContent.usage().tokenOutput(),
                llmStageContent.usage() == null ? null : llmStageContent.usage().reasoningTokens(),
                llmStageContent.usage() == null ? null : llmStageContent.usage().latencyMs(),
                llmStageContent.usage() == null ? null : llmStageContent.usage().finishReason(),
                llmStageContent.usage() != null && llmStageContent.usage().timeout(),
                llmStageContent.truncated(),
                llmStageContent.generationMode()
            );
            generationReason = "Generated by LLM successfully.";
        } catch (Exception ex) {
            if (!llmProperties.isFallbackToRule()) {
                taskRepository.markAttemptFailed(attemptId, ex.getMessage(), AttemptLlmMetadata.none("LLM"));
                llmCallLogRepository.save(new LlmCallLog(
                    attemptId,
                    "TASK_RUN",
                    null,
                    llmProperties.getProvider(),
                    llmProperties.getModel(),
                    task.getStage().name(),
                    "unknown",
                    "{\"note\":\"request payload unavailable\"}",
                    "{\"error\":\"" + sanitizeReason(ex.getMessage()) + "\",\"finish_reason\":\"error\"}",
                    "{}",
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    "error",
                    ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timed out"),
                    false,
                    false,
                    false,
                    false
                ));
                throw ex;
            }
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.TASK_RUN, llmStageContent == null ? llmProperties.getModel() : llmStageContent.model()),
                llmFailureClassifier.classifyFallback(ex),
                -1
            );
            generated = generateByStage(task);
            metadata = AttemptLlmMetadata.none("TEMPLATE_FALLBACK");
            generationReason = "LLM failed: " + sanitizeReason(ex.getMessage());
            fallbackUsed = true;
        }

        String outputJson = writeJson(generated);
        taskRepository.markAttemptSucceeded(attemptId, outputJson, metadata);

        if ("LLM".equals(metadata.generationMode())) {
            llmCallLogRepository.save(new LlmCallLog(
                attemptId,
                "TASK_RUN",
                metadata.invocationProfile(),
                metadata.llmProvider(),
                metadata.llmModel(),
                promptKey,
                metadata.promptVersion(),
                toJson(llmStageContent == null ? null : llmStageContent.requestPayload()),
                toJson(buildResponseSummary(llmStageContent == null ? null : llmStageContent.responsePayload())),
                outputJson,
                "SUCCEEDED",
                metadata.latencyMs(),
                metadata.tokenInput(),
                metadata.tokenOutput(),
                metadata.reasoningTokens(),
                metadata.finishReason(),
                metadata.timeout(),
                fallbackUsed,
                llmStageContent != null && llmStageContent.parseSuccess(),
                llmStageContent != null && llmStageContent.schemaValid(),
                metadata.truncated()
            ));
        }

        task.markSucceeded(outputJson);
        List<LearningStep> steps = ensureLearningSteps(task, generated);
        return toResponse(task, metadata.generationMode(), generationReason, generated, steps);
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

    private String toJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "{\"note\":\"payload missing\"}";
        }
        return writeJson(node);
    }

    private JsonNode buildResponseSummary(JsonNode responsePayload) {
        if (responsePayload == null || responsePayload.isNull() || responsePayload.isMissingNode()) {
            return objectMapper.valueToTree(Map.of("note", "response payload missing"));
        }
        JsonNode usage = responsePayload.path("usage");
        String finishReason = responsePayload.path("choices").path(0).path("finish_reason").asText(null);
        String content = responsePayload.path("choices").path(0).path("message").path("content").asText("");
        if (content.length() > 600) {
            content = content.substring(0, 600);
        }
        return objectMapper.valueToTree(Map.of(
            "usage", usage.isMissingNode() ? objectMapper.createObjectNode() : usage,
            "finish_reason", finishReason == null ? "" : finishReason,
            "choices", List.of(Map.of("message", Map.of("content", content)))
        ));
    }

    private RunTaskResponse toResponse(
        Task task,
        String generationMode,
        String generationReason,
        JsonNode output,
        List<LearningStep> steps
    ) {
        LearningStepResponse currentStep = toCurrentStep(steps);
        return new RunTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            TaskStatus.SUCCEEDED.name(),
            generationMode,
            generationReason,
            output,
            currentStep,
            buildNextStepHint(currentStep),
            steps.stream().map(this::toStepResponse).toList()
        );
    }

    private List<LearningStep> ensureLearningSteps(Task task, JsonNode output) {
        if (task.getStage() != com.pandanav.learning.domain.enums.Stage.TRAINING) {
            return List.of();
        }
        List<LearningStep> existing = learningStepRepository.findByTaskIdOrderByStepOrder(task.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        int stepCount = resolveTrainingStepCount(output);
        List<LearningStep> created = java.util.stream.IntStream.rangeClosed(1, stepCount)
            .mapToObj(order -> buildTrainingStep(task, output, order))
            .toList();
        return learningStepRepository.saveAll(created);
    }

    private int resolveTrainingStepCount(JsonNode output) {
        JsonNode questions = output == null ? null : output.path("questions");
        if (questions != null && questions.isArray() && questions.size() > 0) {
            return questions.size();
        }
        return 1;
    }

    private LearningStep buildTrainingStep(Task task, JsonNode output, int order) {
        LearningStep step = new LearningStep();
        step.setTaskId(task.getId());
        step.setStage(task.getStage());
        step.setType("QUESTION");
        step.setStepOrder(order);
        step.setStatus(order == 1 ? LearningStepStatus.ACTIVE : LearningStepStatus.TODO);
        step.setObjective(resolveQuestionObjective(output, order));
        step.setCompletionRule(new CompletionRule("MANUAL_CONFIRM", 1, List.of(), 3));
        return step;
    }

    private String resolveQuestionObjective(JsonNode output, int order) {
        JsonNode questions = output == null ? null : output.path("questions");
        if (questions != null && questions.isArray() && questions.size() >= order) {
            JsonNode item = questions.get(order - 1);
            String question = item.path("question").asText(null);
            if (question != null && !question.isBlank()) {
                return question;
            }
        }
        return "完成训练步骤 " + order;
    }

    private LearningStepResponse toCurrentStep(List<LearningStep> steps) {
        Optional<LearningStep> active = steps.stream()
            .filter(step -> step.getStatus() == LearningStepStatus.ACTIVE)
            .findFirst();
        if (active.isPresent()) {
            return toStepResponse(active.get());
        }
        return steps.stream()
            .filter(step -> step.getStatus() == LearningStepStatus.TODO)
            .findFirst()
            .map(this::toStepResponse)
            .orElse(null);
    }

    private String buildNextStepHint(LearningStepResponse currentStep) {
        if (currentStep == null) {
            return "当前任务暂无可执行步骤。";
        }
        return "继续下一步：" + currentStep.objective();
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

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown error";
        }
        return reason.replace("\"", "'");
    }

    private StageContent generateByLlm(Task task, Long userId) {
        if (!llmProperties.isReady() || !llmProperties.isEnabled()) {
            throw new InternalServerException("LLM is disabled.");
        }
        LearningSession session = (userId == null
            ? sessionRepository.findById(task.getSessionId())
            : sessionRepository.findByIdAndUserPk(task.getSessionId(), userId))
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

    private void enforceLightJsonGuardrail(StageContent stageContent) {
        if (stageContent == null || stageContent.usage() == null || stageContent.invocationProfile() == null) {
            return;
        }
        Integer completionTokens = stageContent.usage().tokenOutput();
        Integer warningThreshold = llmProperties.resolveProfile(stageContent.invocationProfile(), stageContent.promptKey())
            .completionWarningThreshold();
        if (completionTokens != null && warningThreshold != null && completionTokens > warningThreshold) {
            log.warn(
                "LLM completion tokens exceed threshold, fallback will be used. profile={}, promptKey={}, completionTokens={}, threshold={}",
                stageContent.invocationProfile(),
                stageContent.promptKey(),
                completionTokens,
                warningThreshold
            );
            throw new InternalServerException("completion tokens exceed threshold");
        }
    }

    private JsonNode generateByStage(Task task) {
        String conceptLabel = resolveConceptLabel(task);
        return switch (task.getStage()) {
            case STRUCTURE -> objectMapper.valueToTree(Map.of(
                "title", safeShort(conceptLabel, 20),
                "summary", "建立" + safeShort(conceptLabel, 20) + "的核心结构认识",
                "key_points", List.of("定义边界", "核心关系", "常见变化", "最小流程"),
                "common_misconceptions", List.of("只记结论", "忽略条件"),
                "suggested_sequence", List.of("先定义", "再分层", "看关系", "做总结")
            ));
            case UNDERSTANDING -> objectMapper.valueToTree(Map.of(
                "concept_explanation", "先明确概念定义，再确认它在当前任务中的作用。",
                "analogy", "像按规则切换步骤。",
                "worked_example", "先给出条件，再按顺序判断，最后得到结果。",
                "step_by_step_reasoning", List.of("先看条件", "再看变化", "最后验结果"),
                "common_errors", List.of("跳过条件", "混淆顺序"),
                "check_questions", List.of("触发条件是什么", "结果如何验证")
            ));
            case TRAINING -> objectMapper.valueToTree(Map.of(
                "questions", List.of(
                    Map.of("id", "q1", "type", "BASIC", "question", "说明" + safeShort(conceptLabel, 12), "reference_points", List.of("定义", "条件"), "difficulty", "EASY"),
                    Map.of("id", "q2", "type", "APPLICATION", "question", "给场景说明如何应用", "reference_points", List.of("步骤", "结果"), "difficulty", "MEDIUM"),
                    Map.of("id", "q3", "type", "REASONING", "question", "条件变化后结果怎样变", "reference_points", List.of("推导", "边界"), "difficulty", "HARD")
                )
            ));
            case REFLECTION -> objectMapper.valueToTree(Map.of(
                "reflection_prompt", "回想你最容易出错的一步。",
                "review_checklist", List.of("能否说定义", "能否讲顺序", "能否辨误区"),
                "next_step_suggestion", "先复述一遍，再做一题短练习。"
            ));
        };
    }

    private String resolveConceptLabel(Task task) {
        return conceptNodeRepository.findById(task.getNodeId())
            .map(ConceptNode::getName)
            .filter(name -> name != null && !name.isBlank())
            .orElseGet(() -> extractConceptFromObjective(task.getObjective()));
    }

    private String extractConceptFromObjective(String objective) {
        if (objective == null || objective.isBlank()) {
            return "当前知识点";
        }
        String trimmed = objective.trim();
        if (trimmed.startsWith("梳理") && trimmed.endsWith("的核心结构")) {
            return trimmed.substring(2, trimmed.length() - "的核心结构".length());
        }
        if (trimmed.startsWith("理解") && trimmed.endsWith("的机制与易错点")) {
            return trimmed.substring(2, trimmed.length() - "的机制与易错点".length());
        }
        if (trimmed.startsWith("完成") && trimmed.endsWith("的针对性训练")) {
            return trimmed.substring(2, trimmed.length() - "的针对性训练".length());
        }
        if (trimmed.startsWith("反思") && trimmed.endsWith("的错误与改进方向")) {
            return trimmed.substring(2, trimmed.length() - "的错误与改进方向".length());
        }
        return trimmed;
    }

    private String safeShort(String value, int maxLen) {
        String safe = value == null || value.isBlank() ? "当前知识点" : value.trim();
        return safe.length() <= maxLen ? safe : safe.substring(0, maxLen);
    }
}
