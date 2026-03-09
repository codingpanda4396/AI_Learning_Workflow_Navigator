package com.pandanav.learning.application.service;

import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.PracticeItemSource;
import com.pandanav.learning.domain.enums.PracticeItemStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PracticeServiceImpl implements PracticeService {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final PracticeRepository practiceRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;

    public PracticeServiceImpl(
        PracticeRepository practiceRepository,
        PracticeSubmissionRepository practiceSubmissionRepository,
        TaskRepository taskRepository,
        SessionRepository sessionRepository
    ) {
        this.practiceRepository = practiceRepository;
        this.practiceSubmissionRepository = practiceSubmissionRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public PracticeItem createPracticeItem(PracticeItem item) {
        Task trainingTask = requireTrainingTask(item.getSessionId(), item.getTaskId(), null);

        if (item.getStage() == null) {
            item.setStage(Stage.TRAINING);
        }
        if (item.getNodeId() == null) {
            item.setNodeId(trainingTask.getNodeId());
        }
        if (item.getSource() == null) {
            item.setSource(PracticeItemSource.RULE);
        }
        if (item.getStatus() == null) {
            item.setStatus(PracticeItemStatus.GENERATED);
        }
        if (item.getUserId() == null) {
            LearningSession session = sessionRepository.findById(item.getSessionId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
            if (session.getUserPk() == null) {
                throw new ConflictException("Session user is missing.");
            }
            item.setUserId(session.getUserPk());
        }

        return practiceRepository.save(item);
    }

    @Override
    public List<PracticeItem> listPracticeItems(Long sessionId, Long taskId) {
        Long userPk = UserContextHolder.getUserId();
        requireTrainingTask(sessionId, taskId, userPk);

        if (userPk == null) {
            return practiceRepository.findBySessionIdAndTaskId(sessionId, taskId);
        }
        return practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userPk);
    }

    @Override
    @Transactional
    public PracticeSubmission submitPractice(Long practiceItemId, PracticeSubmission submission) {
        Long userPk = UserContextHolder.getUserId();
        PracticeItem item = (userPk == null
            ? practiceRepository.findById(practiceItemId)
            : practiceRepository.findByIdAndUserPk(practiceItemId, userPk))
            .orElseThrow(() -> new NotFoundException("Practice item not found."));

        requireTrainingTask(item.getSessionId(), item.getTaskId(), userPk);

        submission.setPracticeItemId(item.getId());
        submission.setSessionId(item.getSessionId());
        submission.setTaskId(item.getTaskId());
        submission.setUserId(item.getUserId());

        PracticeSubmission saved = practiceSubmissionRepository.save(submission);
        practiceRepository.updateStatus(item.getId(), PracticeItemStatus.ANSWERED);
        return saved;
    }

    @Override
    public PracticeTaskStats summarizeTask(Long sessionId, Long taskId) {
        Long userPk = UserContextHolder.getUserId();
        requireTrainingTask(sessionId, taskId, userPk);

        List<PracticeItem> items = userPk == null
            ? practiceRepository.findBySessionIdAndTaskId(sessionId, taskId)
            : practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userPk);

        List<PracticeSubmission> submissions = userPk == null
            ? practiceSubmissionRepository.findBySessionIdAndTaskId(sessionId, taskId)
            : practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userPk);

        Set<Long> answeredItemIds = submissions.stream()
            .map(PracticeSubmission::getPracticeItemId)
            .collect(Collectors.toSet());

        double avg = submissions.stream()
            .map(PracticeSubmission::getScore)
            .filter(score -> score != null)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(Double.NaN);

        long correctCount = submissions.stream()
            .map(PracticeSubmission::getCorrect)
            .filter(Boolean.TRUE::equals)
            .count();

        double correctRate = submissions.isEmpty() ? Double.NaN : (double) correctCount / submissions.size();

        return new PracticeTaskStats(
            items.size(),
            answeredItemIds.size(),
            Double.isNaN(avg) ? null : avg,
            Double.isNaN(correctRate) ? null : correctRate
        );
    }

    private Task requireTrainingTask(Long sessionId, Long taskId, Long userPk) {
        Task task = (userPk == null
            ? taskRepository.findById(taskId)
            : taskRepository.findByIdAndUserPk(taskId, userPk))
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (!task.getSessionId().equals(sessionId)) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }

        if (task.getStage() != Stage.TRAINING) {
            throw new ConflictException("Practice item can only bind TRAINING stage tasks.");
        }

        if (userPk != null) {
            sessionRepository.findByIdAndUserPk(sessionId, userPk)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        } else {
            sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        }

        return task;
    }
}
