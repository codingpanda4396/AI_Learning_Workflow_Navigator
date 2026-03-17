package navigator.infrastructure.memory;

import navigator.domain.enums.PlanStatus;
import navigator.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprint 0 内存存储，单例式使用。Sprint 1 增加诊断会话状态与计划状态。
 */
@Component
public class InMemoryStore {

    private final Map<String, StructuredLearningGoal> goals = new ConcurrentHashMap<>();
    private final Map<String, GoalContextSnapshot> goalContextSnapshots = new ConcurrentHashMap<>();
    private final Map<String, LearnerProfileSnapshot> learnerProfiles = new ConcurrentHashMap<>();
    private final Map<String, DiagnosisEvidenceSummary> diagnosisEvidenceSummaries = new ConcurrentHashMap<>();
    private final Map<String, LearningPlanPreview> planPreviews = new ConcurrentHashMap<>();
    private final Map<String, LearningSessionState> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<TaskExecutionRecord>> sessionTaskRecords = new ConcurrentHashMap<>();

    /** Sprint 1: diagnosisId -> status (READY, COMPLETED) */
    private final Map<String, String> diagnosisSessionStatuses = new ConcurrentHashMap<>();
    /** Sprint 1: diagnosisId -> goalId (for submit to load goal context) */
    private final Map<String, String> diagnosisToGoal = new ConcurrentHashMap<>();
    /** diagnosisId -> sessionId (for planning to resolve session from diagnosis) */
    private final Map<String, String> diagnosisToSession = new ConcurrentHashMap<>();
    /** Sprint 1: planId -> PlanStatus */
    private final Map<String, PlanStatus> planStatuses = new ConcurrentHashMap<>();

    public Map<String, StructuredLearningGoal> getGoals() {
        return goals;
    }

    public Map<String, GoalContextSnapshot> getGoalContextSnapshots() {
        return goalContextSnapshots;
    }

    public Map<String, LearnerProfileSnapshot> getLearnerProfiles() {
        return learnerProfiles;
    }

    public Map<String, DiagnosisEvidenceSummary> getDiagnosisEvidenceSummaries() {
        return diagnosisEvidenceSummaries;
    }

    public Map<String, LearningPlanPreview> getPlanPreviews() {
        return planPreviews;
    }

    public Map<String, LearningSessionState> getSessions() {
        return sessions;
    }

    public Map<String, List<TaskExecutionRecord>> getSessionTaskRecords() {
        return sessionTaskRecords;
    }

    public List<TaskExecutionRecord> getOrCreateTaskRecords(String sessionId) {
        return sessionTaskRecords.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public Map<String, String> getDiagnosisSessionStatuses() {
        return diagnosisSessionStatuses;
    }

    public Map<String, String> getDiagnosisToGoal() {
        return diagnosisToGoal;
    }

    public Map<String, String> getDiagnosisToSession() {
        return diagnosisToSession;
    }

    public Map<String, PlanStatus> getPlanStatuses() {
        return planStatuses;
    }

    /**
     * 学习会话内存状态
     */
    public static class LearningSessionState {
        private String sessionId;
        private String planId;
        private List<String> taskSequence;
        private int currentTaskIndex;
        private String status; // IN_PROGRESS, COMPLETED

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public List<String> getTaskSequence() { return taskSequence; }
        public void setTaskSequence(List<String> taskSequence) { this.taskSequence = taskSequence; }
        public int getCurrentTaskIndex() { return currentTaskIndex; }
        public void setCurrentTaskIndex(int currentTaskIndex) { this.currentTaskIndex = currentTaskIndex; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
