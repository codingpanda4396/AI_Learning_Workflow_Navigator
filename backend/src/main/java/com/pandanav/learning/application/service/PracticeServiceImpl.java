package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.practice.LlmPracticeGenerator;
import com.pandanav.learning.application.service.practice.PracticeDraftItem;
import com.pandanav.learning.application.service.practice.PracticeGeneratorRequest;
import com.pandanav.learning.application.service.practice.PracticeGeneratorResult;
import com.pandanav.learning.application.service.practice.RulePracticeGenerator;
import com.pandanav.learning.domain.enums.PracticeItemSource;
import com.pandanav.learning.domain.enums.PracticeItemStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeServiceImpl implements PracticeService {

    private static final Logger log = LoggerFactory.getLogger(PracticeServiceImpl.class);
    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final PracticeRepository practiceRepository;
    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final LearningEventRepository learningEventRepository;
    private final RulePracticeGenerator rulePracticeGenerator;
    private final LlmPracticeGenerator llmPracticeGenerator;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public PracticeServiceImpl(
        PracticeRepository practiceRepository,
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        LearningEventRepository learningEventRepository,
        RulePracticeGenerator rulePracticeGenerator,
        LlmPracticeGenerator llmPracticeGenerator,
        LlmProperties llmProperties,
        ObjectMapper objectMapper
    ) {
        this.practiceRepository = practiceRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.learningEventRepository = learningEventRepository;
        this.rulePracticeGenerator = rulePracticeGenerator;
        this.llmPracticeGenerator = llmPracticeGenerator;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PracticeItem> listPracticeItems(Long sessionId, Long taskId, Long userId) {
        requireTrainingTask(sessionId, taskId, userId);
        return practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId);
    }

    @Override
    @Transactional
    public List<PracticeItem> generatePracticeItems(Long sessionId, Long taskId, Long userId) {
        Task task = requireTrainingTask(sessionId, taskId, userId);
        ConceptNode node = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        PracticeGeneratorRequest request = new PracticeGeneratorRequest(
            sessionId,
            taskId,
            userId,
            node.getId(),
            node.getName(),
            task.getObjective(),
            task.getOutputJson()
        );

        PracticeGeneratorResult result = generateWithFallback(request);
        List<PracticeItem> saved = saveGeneratedItems(task, userId, result);
        logGeneration(sessionId, userId, task, result, saved.size());

        return saved;
    }

    @Override
    @Transactional
    public List<PracticeItem> getOrCreatePracticeItems(Long sessionId, Long taskId, Long userId) {
        List<PracticeItem> existing = listPracticeItems(sessionId, taskId, userId);
        if (!existing.isEmpty()) {
            return existing;
        }
        return generatePracticeItems(sessionId, taskId, userId);
    }

    private PracticeGeneratorResult generateWithFallback(PracticeGeneratorRequest request) {
        if (llmProperties.isReady() && llmProperties.isEnabled()) {
            try {
                PracticeGeneratorResult result = llmPracticeGenerator.generate(request);
                log.info(
                    "practice generation succeeded via LLM: sessionId={}, taskId={}, userId={}, items={}, provider={}, model={}, promptVersion={}",
                    request.sessionId(), request.taskId(), request.userId(), result.items().size(),
                    result.provider(), result.model(), result.promptVersion()
                );
                return result;
            } catch (Exception ex) {
                if (!llmProperties.isFallbackToRule()) {
                    throw ex;
                }
                log.warn(
                    "practice generation fell back to RULE after LLM failure: sessionId={}, taskId={}, userId={}, reason={}",
                    request.sessionId(), request.taskId(), request.userId(), sanitizeReason(ex.getMessage())
                );
                PracticeGeneratorResult fallback = rulePracticeGenerator.generate(request);
                return new PracticeGeneratorResult(
                    fallback.items(),
                    fallback.source(),
                    true,
                    false,
                    fallback.promptVersion(),
                    null,
                    null,
                    null,
                    null,
                    null
                );
            }
        }

        PracticeGeneratorResult fallback = rulePracticeGenerator.generate(request);
        log.info(
            "practice generation used RULE directly: sessionId={}, taskId={}, userId={}, llmEnabled={}, llmReady={}",
            request.sessionId(), request.taskId(), request.userId(), llmProperties.isEnabled(), llmProperties.isReady()
        );
        return fallback;
    }

    private List<PracticeItem> saveGeneratedItems(Task task, Long userId, PracticeGeneratorResult result) {
        return result.items().stream()
            .map(item -> toEntity(task, userId, item, result))
            .map(practiceRepository::save)
            .toList();
    }

    private PracticeItem toEntity(Task task, Long userId, PracticeDraftItem item, PracticeGeneratorResult result) {
        PracticeItem entity = new PracticeItem();
        entity.setSessionId(task.getSessionId());
        entity.setTaskId(task.getId());
        entity.setUserId(userId);
        entity.setNodeId(task.getNodeId());
        entity.setStage(Stage.TRAINING);
        entity.setQuestionType(item.questionType());
        entity.setStem(item.stem());
        entity.setOptionsJson(toJson(item.options()));
        entity.setStandardAnswer(item.standardAnswer());
        entity.setExplanation(item.explanation());
        entity.setDifficulty(item.difficulty());
        entity.setSource(PracticeItemSource.fromDb(result.source()));
        entity.setStatus(PracticeItemStatus.ACTIVE);
        entity.setPromptVersion(result.promptVersion());
        entity.setTokenInput(result.tokenInput());
        entity.setTokenOutput(result.tokenOutput());
        entity.setLatencyMs(result.latencyMs());
        return entity;
    }

    private Task requireTrainingTask(Long sessionId, Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndUserPk(taskId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (!task.getSessionId().equals(sessionId)) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }

        if (task.getStage() != Stage.TRAINING) {
            throw new ConflictException("Practice generation is only allowed for TRAINING stage.");
        }

        sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        return task;
    }

    private void logGeneration(Long sessionId, Long userId, Task task, PracticeGeneratorResult result, int count) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", task.getId());
        payload.put("node_id", task.getNodeId());
        payload.put("source", result.source());
        payload.put("llm_parse_succeeded", result.llmParseSucceeded());
        payload.put("fallback_triggered", result.fallbackTriggered());
        payload.put("generated_count", count);
        payload.put("prompt_version", result.promptVersion());
        payload.put("provider", result.provider());
        payload.put("model", result.model());
        payload.put("token_input", result.tokenInput());
        payload.put("token_output", result.tokenOutput());
        payload.put("latency_ms", result.latencyMs());

        LearningEvent event = new LearningEvent();
        event.setSessionId(sessionId);
        event.setUserId(userId);
        event.setEventType("PRACTICE_ITEMS_GENERATED");
        event.setEventData(toJson(payload));
        learningEventRepository.save(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize practice generation payload.");
        }
    }

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.replace("\"", "'");
    }
}
