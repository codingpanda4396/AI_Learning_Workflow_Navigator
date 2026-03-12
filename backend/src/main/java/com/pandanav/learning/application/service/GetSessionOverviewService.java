package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.MasterySummaryResponse;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.session.ProgressResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.api.dto.session.TimelineItemResponse;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GetSessionOverviewService implements GetSessionOverviewUseCase {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;

    public GetSessionOverviewService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
    }

    @Override
    public SessionOverviewResponse execute(Long sessionId) {
        Long userId = UserContextHolder.getUserId();
        var session = (userId == null
            ? sessionRepository.findById(sessionId)
            : sessionRepository.findByIdAndUserPk(sessionId, userId))
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<Task> timelineTasks = taskRepository.findBySessionIdWithStatus(sessionId);

        List<TimelineItemResponse> timeline = timelineTasks.stream()
            .map(task -> new TimelineItemResponse(
                task.getId(),
                task.getStage().name(),
                task.getNodeId(),
                task.getStatus().name()
            ))
            .toList();

        NextTaskResponse nextTask = timelineTasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.SUCCEEDED)
            .findFirst()
            .map(task -> new NextTaskResponse(task.getId(), task.getStage().name(), task.getNodeId()))
            .orElse(null);

        List<MasterySummaryResponse> masterySummary = masteryRepository
            .findByUserIdAndChapterId(session.getUserId(), session.getChapterId())
            .stream()
            .map(mastery -> new MasterySummaryResponse(
                mastery.getNodeId(),
                mastery.getNodeName(),
                mastery.getMasteryValue() == null ? BigDecimal.ZERO : mastery.getMasteryValue()
            ))
            .toList();

        int totalTaskCount = timelineTasks.size();
        int completedTaskCount = (int) timelineTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.SUCCEEDED)
            .count();
        BigDecimal completionRate = totalTaskCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(completedTaskCount)
                .divide(BigDecimal.valueOf(totalTaskCount), 4, java.math.RoundingMode.HALF_UP);

        return new SessionOverviewResponse(
            session.getId(),
            session.getCourseId(),
            session.getChapterId(),
            session.getGoalText(),
            session.getCurrentNodeId(),
            session.getCurrentStage() == null ? null : session.getCurrentStage().name(),
            session.getStatus() == null ? null : session.getStatus().name(),
            timeline,
            nextTask,
            masterySummary,
            new ProgressResponse(completedTaskCount, totalTaskCount, completionRate)
        );
    }
}


