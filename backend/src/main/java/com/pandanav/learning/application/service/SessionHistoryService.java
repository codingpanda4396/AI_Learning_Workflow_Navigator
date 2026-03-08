package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.ProgressResponse;
import com.pandanav.learning.api.dto.session.SessionHistoryItemResponse;
import com.pandanav.learning.api.dto.session.SessionHistoryResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SessionHistoryService {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final GetSessionOverviewUseCase getSessionOverviewUseCase;
    private final LearningEventRepository learningEventRepository;

    public SessionHistoryService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        GetSessionOverviewUseCase getSessionOverviewUseCase,
        LearningEventRepository learningEventRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.getSessionOverviewUseCase = getSessionOverviewUseCase;
        this.learningEventRepository = learningEventRepository;
    }

    public SessionHistoryResponse listHistory(Integer page, Integer pageSize, String status) {
        Long userId = UserContextHolder.getRequiredUserId();
        int normalizedPage = page == null ? 1 : page;
        int normalizedPageSize = pageSize == null ? 10 : pageSize;
        if (normalizedPage < 1 || normalizedPageSize < 1 || normalizedPageSize > 100) {
            throw new BadRequestException("Invalid page or page_size.");
        }
        int offset = (normalizedPage - 1) * normalizedPageSize;
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();

        List<SessionHistoryItemResponse> items = sessionRepository
            .findHistoryByUserPk(userId, normalizedStatus, normalizedPageSize, offset)
            .stream()
            .map(session -> new SessionHistoryItemResponse(
                session.getId(),
                session.getCourseId(),
                session.getChapterId(),
                session.getGoalText(),
                session.getStatus(),
                buildProgress(session.getId()),
                session.getLastActiveAt()
            ))
            .toList();

        long total = sessionRepository.countHistoryByUserPk(userId, normalizedStatus);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);
        return new SessionHistoryResponse(normalizedPage, normalizedPageSize, total, totalPages, items);
    }

    public SessionOverviewResponse getSessionDetail(Long sessionId) {
        ensureOwnership(sessionId);
        return getSessionOverviewUseCase.execute(sessionId);
    }

    public SessionOverviewResponse resume(Long sessionId) {
        Long userId = ensureOwnership(sessionId);
        sessionRepository.touchLastActive(sessionId);

        LearningEvent event = new LearningEvent();
        event.setSessionId(sessionId);
        event.setUserId(userId);
        event.setEventType("SESSION_RESUMED");
        event.setEventData("{}");
        learningEventRepository.save(event);

        return getSessionOverviewUseCase.execute(sessionId);
    }

    private Long ensureOwnership(Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));
        return userId;
    }

    private ProgressResponse buildProgress(Long sessionId) {
        List<Task> tasks = taskRepository.findBySessionIdWithStatus(sessionId);
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.SUCCEEDED).count();
        BigDecimal completionRate = total == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        return new ProgressResponse(completed, total, completionRate);
    }
}
