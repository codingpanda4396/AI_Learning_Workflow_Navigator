package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.MasterySummaryResponse;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.session.ProgressResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewSummaryResponse;
import com.pandanav.learning.api.dto.session.TimelineItemResponse;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.enums.SessionStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import com.pandanav.learning.domain.repository.PracticeQuizRepository;
import com.pandanav.learning.domain.repository.PlanInstanceRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class GetSessionOverviewService implements GetSessionOverviewUseCase {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;
    private final PracticeQuizRepository practiceQuizRepository;
    private final PracticeFeedbackReportRepository practiceFeedbackReportRepository;
    private final PlanInstanceRepository planInstanceRepository;

    public GetSessionOverviewService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository,
        PracticeQuizRepository practiceQuizRepository,
        PracticeFeedbackReportRepository practiceFeedbackReportRepository,
        PlanInstanceRepository planInstanceRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
        this.practiceQuizRepository = practiceQuizRepository;
        this.practiceFeedbackReportRepository = practiceFeedbackReportRepository;
        this.planInstanceRepository = planInstanceRepository;
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

        SessionOverviewSummaryResponse summary = buildSummary(session.getId(), session.getStatus(), session.getCurrentStage(), nextTask, timelineTasks);
        Long planInstanceId = session.getCurrentPlanInstanceId() != null
            ? session.getCurrentPlanInstanceId()
            : planInstanceRepository.findActiveBySessionId(session.getId()).map(pi -> pi.getId()).orElse(null);

        return new SessionOverviewResponse(
            session.getId(),
            planInstanceId,
            session.getCourseId(),
            session.getChapterId(),
            session.getGoalText(),
            session.getCurrentNodeId(),
            session.getCurrentStage() == null ? null : session.getCurrentStage().name(),
            session.getStatus() == null ? null : session.getStatus().name(),
            timeline,
            nextTask,
            masterySummary,
            new ProgressResponse(completedTaskCount, totalTaskCount, completionRate),
            summary
        );
    }

    private SessionOverviewSummaryResponse buildSummary(
        Long sessionId,
        SessionStatus sessionStatus,
        Stage currentStage,
        NextTaskResponse nextTask,
        List<Task> timelineTasks
    ) {
        SessionStatus effectiveStatus = sessionStatus == null ? SessionStatus.LEARNING : sessionStatus;

        String currentTaskTitle = switch (effectiveStatus) {
            case ANALYZING -> "Complete diagnosis";
            case PLANNING -> "Review learning plan";
            case PRACTICING -> "Continue training";
            case REPORT_READY -> "Review the latest report";
            case COMPLETED -> "Session completed";
            case FAILED -> "Session needs recovery";
            case LEARNING -> stageTitle(currentStage);
        };

        String currentTaskDescription = switch (effectiveStatus) {
            case ANALYZING -> "The system is collecting diagnosis results before planning the next learning steps.";
            case PLANNING -> "The learning path is being assembled from the latest diagnosis and task state.";
            case PRACTICING -> "Finish the current training step so the report can reflect the real weak points.";
            case REPORT_READY -> "The latest training report is ready. Review it before deciding what to do next.";
            case COMPLETED -> "This session has ended. You can review the result or return to the home page.";
            case FAILED -> "This session stopped unexpectedly. Go back to the last valid entry and retry from there.";
            case LEARNING -> stageDescription(currentStage);
        };

        String primaryActionLabel;
        String primaryActionPath;
        if (effectiveStatus == SessionStatus.REPORT_READY) {
            primaryActionLabel = "Open report";
            primaryActionPath = "/sessions/" + sessionId + "/report";
        } else if (nextTask != null) {
            primaryActionLabel = "Open current task";
            primaryActionPath = "/tasks/" + nextTask.taskId() + "/run";
        } else if (effectiveStatus == SessionStatus.PRACTICING || currentStage == Stage.TRAINING) {
            primaryActionLabel = "Open quiz";
            primaryActionPath = "/sessions/" + sessionId + "/quiz";
        } else {
            primaryActionLabel = "Refresh session";
            primaryActionPath = "/sessions/" + sessionId;
        }

        String nextStepHint = switch (effectiveStatus) {
            case ANALYZING -> "After diagnosis, the session will move into learning plan generation.";
            case PLANNING -> "After planning, you can enter the first real learning task.";
            case PRACTICING -> "After training, a report will summarize weak points and next action.";
            case REPORT_READY -> "Use the report to decide whether to review, reinforce, or advance.";
            case COMPLETED -> "You can return home and start a new session when needed.";
            case FAILED -> "Retry from the last stable page instead of continuing from a broken state.";
            case LEARNING -> "Finish the current learning task to unlock the next stage.";
        };

        String recentReportSummary = findRecentReportSummary(sessionId, timelineTasks)
            .orElse("No report is available yet for this session.");

        return new SessionOverviewSummaryResponse(
            currentTaskTitle,
            currentTaskDescription,
            nextStepHint,
            primaryActionLabel,
            primaryActionPath,
            recentReportSummary
        );
    }

    private Optional<String> findRecentReportSummary(Long sessionId, List<Task> timelineTasks) {
        Optional<Task> latestTrainingTask = timelineTasks.stream()
            .filter(task -> task.getStage() == Stage.TRAINING)
            .filter(task -> task.getStatus() == TaskStatus.SUCCEEDED)
            .findFirst();
        if (latestTrainingTask.isEmpty()) {
            return Optional.empty();
        }
        Optional<PracticeQuiz> quiz = practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(
            sessionId,
            latestTrainingTask.get().getId(),
            UserContextHolder.getRequiredUserId()
        );
        if (quiz.isEmpty()) {
            return Optional.empty();
        }
        return practiceFeedbackReportRepository.findByQuizId(quiz.get().getId())
            .map(PracticeFeedbackReport::getDiagnosisSummary)
            .filter(text -> text != null && !text.isBlank());
    }

    private String stageTitle(Stage stage) {
        if (stage == null) {
            return "Continue current session";
        }
        return switch (stage) {
            case STRUCTURE -> "Build the knowledge structure";
            case UNDERSTANDING -> "Deepen understanding";
            case TRAINING -> "Continue training";
            case REFLECTION -> "Review and reflect";
        };
    }

    private String stageDescription(Stage stage) {
        if (stage == null) {
            return "The session is preparing the next valid learning step.";
        }
        return switch (stage) {
            case STRUCTURE -> "Focus on the chapter structure first so the next tasks have a stable knowledge map.";
            case UNDERSTANDING -> "Clarify the core ideas before moving into more difficult practice.";
            case TRAINING -> "Use the current training task to confirm mastery and expose weak points.";
            case REFLECTION -> "Review the latest results and stabilize the concepts that are still weak.";
        };
    }
}


