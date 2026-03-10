package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.practice.SessionQuizAnswerRequest;
import com.pandanav.learning.api.dto.practice.SessionQuizSubmitRequest;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SessionQuizService {

    private final PracticeService practiceService;
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final PracticeFeedbackReportRepository practiceFeedbackReportRepository;

    public SessionQuizService(
        PracticeService practiceService,
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        PracticeSubmissionRepository practiceSubmissionRepository,
        PracticeFeedbackReportRepository practiceFeedbackReportRepository
    ) {
        this.practiceService = practiceService;
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.practiceSubmissionRepository = practiceSubmissionRepository;
        this.practiceFeedbackReportRepository = practiceFeedbackReportRepository;
    }

    @Transactional
    public PracticeQuiz generate(Long sessionId, Long userId) {
        Task task = resolveTrainingTask(sessionId, userId);
        return practiceService.requestQuizGeneration(sessionId, task.getId(), userId);
    }

    public QuizContext getStatus(Long sessionId, Long userId) {
        Task task = resolveTrainingTask(sessionId, userId);
        PracticeQuiz quiz = practiceService.getQuiz(sessionId, task.getId(), userId);
        return new QuizContext(task, quiz, List.of());
    }

    public QuizContext getQuiz(Long sessionId, Long userId) {
        Task task = resolveTrainingTask(sessionId, userId);
        PracticeQuiz quiz = practiceService.getQuiz(sessionId, task.getId(), userId);
        List<PracticeItem> items = quiz.getGenerationStatus().name().equals("SUCCEEDED")
            ? practiceService.listPracticeItems(sessionId, task.getId(), userId)
            : List.of();
        return new QuizContext(task, quiz, items);
    }

    @Transactional
    public FeedbackContext submit(Long sessionId, Long userId, SessionQuizSubmitRequest request) {
        Task task = resolveTrainingTask(sessionId, userId);
        PracticeQuiz quiz = practiceService.getQuiz(sessionId, task.getId(), userId);
        if (!"SUCCEEDED".equals(quiz.getGenerationStatus().name())) {
            throw new ConflictException("Quiz generation is not finished yet.");
        }

        List<PracticeItem> items = practiceService.listPracticeItems(sessionId, task.getId(), userId);
        if (items.isEmpty()) {
            throw new ConflictException("Quiz questions are not ready yet.");
        }

        PracticeFeedbackReport existing = practiceFeedbackReportRepository.findByQuizId(quiz.getId()).orElse(null);
        if (existing != null) {
            return buildFeedback(task, quiz, items, existing);
        }

        Map<Long, SessionQuizAnswerRequest> answersByItemId = new LinkedHashMap<>();
        for (SessionQuizAnswerRequest answer : request.answers()) {
            answersByItemId.put(answer.questionId(), answer);
        }
        List<Long> missing = items.stream()
            .map(PracticeItem::getId)
            .filter(itemId -> !answersByItemId.containsKey(itemId))
            .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("Missing answers for question ids: " + missing);
        }

        for (PracticeItem item : items) {
            PracticeSubmission latest = practiceSubmissionRepository.findLatestByQuizIdAndPracticeItemId(quiz.getId(), item.getId()).orElse(null);
            if (latest != null) {
                continue;
            }
            SessionQuizAnswerRequest answer = answersByItemId.get(item.getId());
            practiceService.submitPracticeAnswer(sessionId, task.getId(), item.getId(), userId, answer.answer());
        }

        PracticeFeedbackReport report = practiceService.getFeedbackReport(sessionId, task.getId(), userId);
        return buildFeedback(task, practiceService.getQuiz(sessionId, task.getId(), userId), items, report);
    }

    public FeedbackContext getFeedback(Long sessionId, Long userId) {
        Task task = resolveTrainingTask(sessionId, userId);
        PracticeQuiz quiz = practiceService.getQuiz(sessionId, task.getId(), userId);
        List<PracticeItem> items = practiceService.listPracticeItems(sessionId, task.getId(), userId);
        PracticeFeedbackReport report = practiceService.getFeedbackReport(sessionId, task.getId(), userId);
        return buildFeedback(task, quiz, items, report);
    }

    @Transactional
    public FeedbackContext applyNextAction(Long sessionId, Long userId, String action) {
        Task task = resolveTrainingTask(sessionId, userId);
        PracticeQuiz quiz = practiceService.applyFeedbackAction(sessionId, task.getId(), userId, action);
        PracticeFeedbackReport report = practiceService.getFeedbackReport(sessionId, task.getId(), userId);
        practiceFeedbackReportRepository.markActionSelected(report.getId(), action.trim().toUpperCase());
        PracticeFeedbackReport refreshedReport = practiceService.getFeedbackReport(sessionId, task.getId(), userId);
        List<PracticeItem> items = practiceService.listPracticeItems(sessionId, task.getId(), userId);
        return buildFeedback(task, quiz, items, refreshedReport);
    }

    private FeedbackContext buildFeedback(Task task, PracticeQuiz quiz, List<PracticeItem> items, PracticeFeedbackReport report) {
        List<SessionQuestionResult> questionResults = new ArrayList<>();
        for (PracticeItem item : items) {
            PracticeSubmission submission = practiceSubmissionRepository.findLatestByQuizIdAndPracticeItemId(quiz.getId(), item.getId()).orElse(null);
            if (submission == null) {
                continue;
            }
            questionResults.add(new SessionQuestionResult(item, submission));
        }
        return new FeedbackContext(task, quiz, report, questionResults);
    }

    private Task resolveTrainingTask(Long sessionId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Session not found."));

        if (session.getCurrentNodeId() != null) {
            Task currentTask = taskRepository.findFirstBySessionIdAndNodeIdAndStage(sessionId, session.getCurrentNodeId(), Stage.TRAINING).orElse(null);
            if (currentTask != null) {
                return currentTask;
            }
        }

        return taskRepository.findBySessionIdWithStatus(sessionId).stream()
            .filter(task -> task.getStage() == Stage.TRAINING)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Training task not found for session."));
    }

    public record QuizContext(Task task, PracticeQuiz quiz, List<PracticeItem> items) {
    }

    public record FeedbackContext(
        Task task,
        PracticeQuiz quiz,
        PracticeFeedbackReport report,
        List<SessionQuestionResult> questionResults
    ) {
    }

    public record SessionQuestionResult(PracticeItem item, PracticeSubmission submission) {
    }
}
