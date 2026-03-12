package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.practice.LlmPracticeGenerator;
import com.pandanav.learning.application.service.practice.PracticeDraftItem;
import com.pandanav.learning.application.service.practice.PracticeGeneratorRequest;
import com.pandanav.learning.application.service.practice.PracticeGeneratorResult;
import com.pandanav.learning.application.service.practice.RulePracticeGenerator;
import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.PracticeFeedbackAction;
import com.pandanav.learning.domain.enums.PracticeItemSource;
import com.pandanav.learning.domain.enums.PracticeItemStatus;
import com.pandanav.learning.domain.enums.PracticeQuizStatus;
import com.pandanav.learning.domain.enums.SessionStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import com.pandanav.learning.domain.repository.PracticeQuizRepository;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PracticeServiceImpl implements PracticeService {

    private static final Logger log = LoggerFactory.getLogger(PracticeServiceImpl.class);
    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final PracticeRepository practiceRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final PracticeQuizRepository practiceQuizRepository;
    private final PracticeFeedbackReportRepository practiceFeedbackReportRepository;
    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final LearningEventRepository learningEventRepository;
    private final RulePracticeGenerator rulePracticeGenerator;
    private final LlmPracticeGenerator llmPracticeGenerator;
    private final PracticeFeedbackReportGenerator practiceFeedbackReportGenerator;
    private final PracticeQuizAsyncService practiceQuizAsyncService;
    private final MasteryService masteryService;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public PracticeServiceImpl(
        PracticeRepository practiceRepository,
        PracticeSubmissionRepository practiceSubmissionRepository,
        PracticeQuizRepository practiceQuizRepository,
        PracticeFeedbackReportRepository practiceFeedbackReportRepository,
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        LearningEventRepository learningEventRepository,
        RulePracticeGenerator rulePracticeGenerator,
        LlmPracticeGenerator llmPracticeGenerator,
        PracticeFeedbackReportGenerator practiceFeedbackReportGenerator,
        PracticeQuizAsyncService practiceQuizAsyncService,
        MasteryService masteryService,
        LlmProperties llmProperties,
        ObjectMapper objectMapper
    ) {
        this.practiceRepository = practiceRepository;
        this.practiceSubmissionRepository = practiceSubmissionRepository;
        this.practiceQuizRepository = practiceQuizRepository;
        this.practiceFeedbackReportRepository = practiceFeedbackReportRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.learningEventRepository = learningEventRepository;
        this.rulePracticeGenerator = rulePracticeGenerator;
        this.llmPracticeGenerator = llmPracticeGenerator;
        this.practiceFeedbackReportGenerator = practiceFeedbackReportGenerator;
        this.practiceQuizAsyncService = practiceQuizAsyncService;
        this.masteryService = masteryService;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PracticeQuiz requestQuizGeneration(Long sessionId, Long taskId, Long userId) {
        Task task = requireTrainingTask(sessionId, taskId, userId);
        PracticeQuiz existing = practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId).orElse(null);
        if (existing != null) {
            if (existing.getGenerationStatus() == TaskStatus.PENDING
                || existing.getGenerationStatus() == TaskStatus.RUNNING
                || existing.getGenerationStatus() == TaskStatus.SUCCEEDED) {
                return existing;
            }
            if (existing.getStatus() != PracticeQuizStatus.FAILED) {
                return existing;
            }
        }

        PracticeQuiz quiz = new PracticeQuiz();
        quiz.setSessionId(sessionId);
        quiz.setTaskId(taskId);
        quiz.setUserId(userId);
        quiz.setNodeId(task.getNodeId());
        quiz.setStatus(PracticeQuizStatus.GENERATING);
        quiz.setGenerationStatus(TaskStatus.PENDING);
        quiz.setQuestionCount(0);
        quiz.setAnsweredCount(0);
        PracticeQuiz saved = practiceQuizRepository.save(quiz);
        sessionRepository.updateStatus(sessionId, SessionStatus.PRACTICING);
        practiceQuizAsyncService.generateQuizAsync(saved.getId(), sessionId, taskId, userId);
        return saved;
    }

    @Override
    public PracticeQuiz getQuiz(Long sessionId, Long taskId, Long userId) {
        requireTrainingTask(sessionId, taskId, userId);
        return practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId)
            .orElseThrow(() -> new NotFoundException("Practice quiz not found."));
    }

    @Override
    public PracticeFeedbackReport getFeedbackReport(Long sessionId, Long taskId, Long userId) {
        PracticeQuiz quiz = getQuiz(sessionId, taskId, userId);
        return practiceFeedbackReportRepository.findByQuizId(quiz.getId())
            .orElseThrow(() -> new NotFoundException("Practice feedback report not found."));
    }

    @Override
    @Transactional
    public PracticeQuiz applyFeedbackAction(Long sessionId, Long taskId, Long userId, String action) {
        PracticeQuiz quiz = getQuiz(sessionId, taskId, userId);
        if (quiz.getStatus() != PracticeQuizStatus.REPORT_READY
            && quiz.getStatus() != PracticeQuizStatus.REVIEWING
            && quiz.getStatus() != PracticeQuizStatus.NEXT_ROUND) {
            throw new ConflictException("Practice feedback action is not available yet.");
        }
        PracticeFeedbackAction resolved;
        try {
            resolved = PracticeFeedbackAction.fromValue(action);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        PracticeQuizStatus targetStatus = resolved == PracticeFeedbackAction.REVIEW
            ? PracticeQuizStatus.REVIEWING
            : PracticeQuizStatus.NEXT_ROUND;
        practiceQuizRepository.updateStatus(quiz.getId(), targetStatus, null);
        sessionRepository.updateStatus(sessionId, resolved == PracticeFeedbackAction.REVIEW ? SessionStatus.LEARNING : SessionStatus.PRACTICING);
        return practiceQuizRepository.findById(quiz.getId()).orElseThrow(() -> new NotFoundException("Practice quiz not found."));
    }

    @Override
    public List<PracticeItem> listPracticeItems(Long sessionId, Long taskId, Long userId) {
        PracticeQuiz quiz = practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId).orElse(null);
        if (quiz != null) {
            return practiceRepository.findByQuizId(quiz.getId());
        }
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
        List<PracticeItem> saved = saveGeneratedItems(null, task, userId, result);
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

    @Override
    @Transactional
    public PracticeSubmission submitPracticeAnswer(
        Long sessionId,
        Long taskId,
        Long practiceItemId,
        Long userId,
        String userAnswer
    ) {
        requireTrainingTask(sessionId, taskId, userId);
        PracticeItem item = practiceRepository.findByIdAndUserPk(practiceItemId, userId)
            .orElseThrow(() -> new NotFoundException("Practice item not found."));
        if (!item.getSessionId().equals(sessionId) || !item.getTaskId().equals(taskId)) {
            throw new NotFoundException("Practice item not found.");
        }
        if (userAnswer == null || userAnswer.isBlank()) {
            throw new BadRequestException("user_answer must not be blank.");
        }

        PracticeQuiz quiz = practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId)
            .orElseThrow(() -> new ConflictException("Practice quiz has not been generated."));
        Optional<PracticeSubmission> previous = practiceSubmissionRepository.findLatestByPracticeItemIdAndUserPk(practiceItemId, userId);
        JudgementResult judgement = judge(item, userAnswer);

        PracticeSubmission submission = new PracticeSubmission();
        submission.setPracticeItemId(practiceItemId);
        submission.setQuizId(quiz.getId());
        submission.setSessionId(sessionId);
        submission.setTaskId(taskId);
        submission.setUserId(userId);
        submission.setUserAnswer(userAnswer.trim());
        submission.setScore(judgement.score());
        submission.setCorrect(judgement.correct());
        submission.setFeedback(judgement.feedback());
        submission.setErrorTagsJson(toJson(judgement.errorTags().stream().map(Enum::name).toList()));
        submission.setJudgeMode("RULE");
        submission.setPromptVersion("rule-v1");
        submission.setJudgingStatus(TaskStatus.SUCCEEDED);
        submission.setJudgingStartedAt(java.time.OffsetDateTime.now());
        submission.setJudgingFinishedAt(java.time.OffsetDateTime.now());

        PracticeSubmission saved = practiceSubmissionRepository.save(submission);
        practiceRepository.updateStatus(practiceItemId, PracticeItemStatus.ANSWERED);
        masteryService.recalculateNodeMastery(sessionId, taskId, userId);
        logSubmission(sessionId, userId, item, saved, previous.isPresent());
        updateQuizProgressAndFeedback(quiz, userId);
        return saved;
    }

    @Override
    public List<PracticeSubmission> listPracticeSubmissions(Long sessionId, Long taskId, Long userId) {
        requireTrainingTask(sessionId, taskId, userId);
        return practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId);
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

    void generateQuizInternal(Long quizId, Long sessionId, Long taskId, Long userId) {
        try {
            practiceQuizRepository.updateGenerationState(quizId, TaskStatus.RUNNING, null, null);
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
            List<PracticeItem> saved = saveGeneratedItems(quizId, task, userId, result);
            practiceQuizRepository.markGenerated(quizId, saved.size(), result.source(), result.promptVersion());
            sessionRepository.updateStatus(sessionId, SessionStatus.PRACTICING);
            logGeneration(sessionId, userId, task, result, saved.size());
        } catch (Exception ex) {
            practiceQuizRepository.updateGenerationState(quizId, TaskStatus.FAILED, sanitizeReason(ex.getMessage()), "QUIZ_GENERATION_FAILED");
            practiceQuizRepository.updateStatus(quizId, PracticeQuizStatus.FAILED, sanitizeReason(ex.getMessage()));
            sessionRepository.updateStatus(sessionId, SessionStatus.FAILED);
            log.warn("practice quiz generation failed: quizId={}, sessionId={}, taskId={}, reason={}", quizId, sessionId, taskId, sanitizeReason(ex.getMessage()));
        }
    }

    private List<PracticeItem> saveGeneratedItems(Long quizId, Task task, Long userId, PracticeGeneratorResult result) {
        return result.items().stream()
            .map(item -> toEntity(quizId, task, userId, item, result))
            .map(practiceRepository::save)
            .toList();
    }

    private PracticeItem toEntity(Long quizId, Task task, Long userId, PracticeDraftItem item, PracticeGeneratorResult result) {
        PracticeItem entity = new PracticeItem();
        entity.setSessionId(task.getSessionId());
        entity.setTaskId(task.getId());
        entity.setQuizId(quizId);
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
        entity.setStatus(PracticeItemStatus.READY);
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

    private void updateQuizProgressAndFeedback(PracticeQuiz quiz, Long userId) {
        List<PracticeItem> items = practiceRepository.findByQuizId(quiz.getId());
        List<PracticeSubmission> submissions = practiceSubmissionRepository.findByQuizId(quiz.getId());
        long answered = submissions.stream()
            .map(PracticeSubmission::getPracticeItemId)
            .distinct()
            .count();
        practiceQuizRepository.updateAnsweredCount(quiz.getId(), (int) answered);
        if (answered < items.size()) {
            practiceQuizRepository.updateStatus(quiz.getId(), PracticeQuizStatus.ANSWERING, null);
            sessionRepository.updateStatus(quiz.getSessionId(), SessionStatus.PRACTICING);
            return;
        }

        PracticeFeedbackReport existing = practiceFeedbackReportRepository.findByQuizId(quiz.getId()).orElse(null);
        if (existing == null) {
            PracticeFeedbackReport report = practiceFeedbackReportGenerator.generate(quiz, items, submissions);
            practiceFeedbackReportRepository.save(report);
        }
        practiceQuizRepository.updateStatus(quiz.getId(), PracticeQuizStatus.REPORT_READY, null);
        sessionRepository.updateStatus(quiz.getSessionId(), SessionStatus.REPORT_READY);
        logFeedbackGenerated(quiz.getSessionId(), userId, quiz.getTaskId(), quiz.getId());
    }

    private JudgementResult judge(PracticeItem item, String userAnswer) {
        return switch (item.getQuestionType()) {
            case SINGLE_CHOICE -> judgeByExactMatch(
                userAnswer,
                item.getStandardAnswer(),
                "Correct choice.",
                "Incorrect choice. Re-check key concept and options.",
                List.of(ErrorTag.CARELESS_MISTAKE)
            );
            case TRUE_FALSE -> judgeByExactMatch(
                normalizeTrueFalse(userAnswer),
                normalizeTrueFalse(item.getStandardAnswer()),
                "Judgement is correct.",
                "Judgement is incorrect. Re-check concept condition.",
                List.of(ErrorTag.CONCEPT_CONFUSION)
            );
            case SHORT_ANSWER -> judgeShortAnswer(userAnswer, item.getStandardAnswer());
        };
    }

    private JudgementResult judgeByExactMatch(
        String userAnswer,
        String standardAnswer,
        String passFeedback,
        String failFeedback,
        List<ErrorTag> failTags
    ) {
        String normalizedUser = normalizeText(userAnswer);
        String normalizedStandard = normalizeText(standardAnswer);
        boolean correct = !normalizedUser.isBlank()
            && !normalizedStandard.isBlank()
            && normalizedUser.equalsIgnoreCase(normalizedStandard);
        return new JudgementResult(
            correct ? 100 : 0,
            correct,
            correct ? passFeedback : failFeedback,
            correct ? List.of() : failTags
        );
    }

    private JudgementResult judgeShortAnswer(String userAnswer, String standardAnswer) {
        String normalizedUser = normalizeText(userAnswer);
        String normalizedStandard = normalizeText(standardAnswer);
        if (normalizedUser.isBlank()) {
            return new JudgementResult(
                0,
                false,
                "Answer is empty. Add core concept and reasoning steps.",
                List.of(ErrorTag.ANSWER_INCOMPLETE)
            );
        }
        if (normalizedStandard.isBlank()) {
            return new JudgementResult(
                60,
                false,
                "Short answer captured. Detailed semantic grading can be upgraded with LLM later.",
                List.of(ErrorTag.ANSWER_INCOMPLETE)
            );
        }
        if (normalizedUser.equalsIgnoreCase(normalizedStandard)
            || normalizedUser.contains(normalizedStandard)
            || normalizedStandard.contains(normalizedUser)) {
            return new JudgementResult(100, true, "Answer is close to the reference answer.", List.of());
        }

        Set<String> keywords = extractKeywords(normalizedStandard);
        if (keywords.isEmpty()) {
            return new JudgementResult(
                60,
                false,
                "Answer partially matched. Add clearer steps and key terms.",
                List.of(ErrorTag.MISSING_STEPS, ErrorTag.ANSWER_INCOMPLETE)
            );
        }

        int matched = 0;
        for (String keyword : keywords) {
            if (normalizedUser.toLowerCase().contains(keyword.toLowerCase())) {
                matched++;
            }
        }
        double ratio = (double) matched / keywords.size();
        if (ratio >= 0.7d) {
            return new JudgementResult(
                85,
                true,
                "Answer covers most key points, but wording can be more precise.",
                List.of()
            );
        }
        if (ratio >= 0.4d) {
            return new JudgementResult(
                60,
                false,
                "Answer is partially correct. Add missing reasoning steps.",
                List.of(ErrorTag.MISSING_STEPS, ErrorTag.ANSWER_INCOMPLETE)
            );
        }
        return new JudgementResult(
            30,
            false,
            "Answer misses core concept points. Revisit the concept definition first.",
            List.of(ErrorTag.CONCEPT_CONFUSION, ErrorTag.ANSWER_INCOMPLETE)
        );
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("[\\p{L}\\p{N}]{3,}").matcher(text);
        while (matcher.find()) {
            keywords.add(matcher.group().toLowerCase());
        }
        return keywords;
    }

    private String normalizeTrueFalse(String value) {
        String normalized = normalizeText(value).toLowerCase();
        return switch (normalized) {
            case "true", "t", "yes", "y", "1", "是", "对", "正确" -> "true";
            case "false", "f", "no", "n", "0", "否", "错", "错误" -> "false";
            default -> normalized;
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
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

    private void logSubmission(
        Long sessionId,
        Long userId,
        PracticeItem item,
        PracticeSubmission submission,
        boolean repeatedSubmission
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", item.getTaskId());
        payload.put("practice_item_id", item.getId());
        payload.put("question_type", item.getQuestionType().name());
        payload.put("score", submission.getScore());
        payload.put("is_correct", submission.getCorrect());
        payload.put("error_tags", submission.getErrorTagsJson());
        payload.put("repeated_submission", repeatedSubmission);

        LearningEvent event = new LearningEvent();
        event.setSessionId(sessionId);
        event.setUserId(userId);
        event.setEventType("PRACTICE_ANSWER_SUBMITTED");
        event.setEventData(toJson(payload));
        learningEventRepository.save(event);
    }

    private void logFeedbackGenerated(Long sessionId, Long userId, Long taskId, Long quizId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", taskId);
        payload.put("quiz_id", quizId);

        LearningEvent event = new LearningEvent();
        event.setSessionId(sessionId);
        event.setUserId(userId);
        event.setEventType("PRACTICE_FEEDBACK_GENERATED");
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

    private record JudgementResult(
        Integer score,
        Boolean correct,
        String feedback,
        List<ErrorTag> errorTags
    ) {
    }
}
