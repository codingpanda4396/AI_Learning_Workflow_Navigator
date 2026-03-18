package navigator.application;

import navigator.api.dto.NextActionConfirmData;
import navigator.api.dto.ReportData;
import navigator.application.guard.SessionStateGuard;
import navigator.application.report.SessionEvidenceAggregator;
import navigator.domain.enums.NextActionType;
import navigator.domain.enums.ResultStatus;
import navigator.application.task.LearningMethodProfileAggregator;
import navigator.domain.model.ExecutionEvidenceSummary;
import navigator.domain.model.LearningMethodProfile;
import navigator.domain.model.LearningReport;
import navigator.domain.model.NextActionDecision;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

@Service
public class ReportApplicationService {

    private final SessionStateGuard sessionStateGuard;
    private final SessionEvidenceAggregator evidenceAggregator;
    private final InMemoryStore store;

    public ReportApplicationService(SessionStateGuard sessionStateGuard,
                                    SessionEvidenceAggregator evidenceAggregator,
                                    InMemoryStore store) {
        this.sessionStateGuard = sessionStateGuard;
        this.evidenceAggregator = evidenceAggregator;
        this.store = store;
    }

    public ReportData getReport(String sessionId) {
        sessionStateGuard.requireSessionCompletedForReport(sessionId);
        Long sessionDbId = extractNumericId(sessionId);
        SessionEvidenceAggregator.AggregatedSessionEvidence aggregated = evidenceAggregator.aggregate(sessionDbId);
        ExecutionEvidenceSummary execution = aggregated.execution();

        java.util.List<LearningMethodProfile> methodTasks = store.getSessionMethodProfiles().getOrDefault(sessionId, java.util.List.of());
        LearningMethodProfile methodRollup = LearningMethodProfileAggregator.aggregateSession(sessionId, methodTasks);
        LearningReport report = buildLearningReport(sessionId, aggregated, execution, methodRollup);
        NextActionDecision decision = report.getNextAction();
        return ReportData.builder()
                .learningReport(report)
                .nextActionDecision(decision)
                .build();
    }

    public NextActionConfirmData confirmNextAction(String sessionId, navigator.domain.enums.NextActionType actionType) {
        sessionStateGuard.requireSessionCompletedForReport(sessionId);
        Long sessionDbId = extractNumericId(sessionId);
        SessionEvidenceAggregator.AggregatedSessionEvidence aggregated = evidenceAggregator.aggregate(sessionDbId);
        ExecutionEvidenceSummary execution = aggregated.execution();

        boolean hasGap = execution.getAggregatedIssueTags() != null
                && !execution.getAggregatedIssueTags().isEmpty();

        boolean requiresReplan = hasGap && (actionType == NextActionType.REMEDIATE_PREREQUISITE
                || actionType == NextActionType.CHANGE_STRATEGY);
        String nextHint;
        if (!hasGap) {
            nextHint = "本轮完成情况良好，可以继续进入下一个相关目标。";
        } else if (actionType == NextActionType.REINFORCE) {
            nextHint = "检测到概念或练习缺口，本轮建议先做一轮针对性的巩固练习。";
        } else {
            nextHint = "存在学习缺口，且你选择了调整路径，系统下一轮将围绕关键薄弱点重新规划。";
        }
        return NextActionConfirmData.builder()
                .sessionId(sessionId)
                .acceptedAction(actionType)
                .requiresReplan(requiresReplan)
                .nextHint(nextHint)
                .build();
    }

    private LearningReport buildLearningReport(String sessionId,
                                               SessionEvidenceAggregator.AggregatedSessionEvidence aggregated,
                                               ExecutionEvidenceSummary execution,
                                               LearningMethodProfile methodRollup) {
        int totalTasks = execution.getTotalTasks() != null ? execution.getTotalTasks() : 0;
        int completedTasks = execution.getCompletedTasks() != null ? execution.getCompletedTasks() : 0;

        ResultStatus status;
        if (totalTasks == 0) {
            status = ResultStatus.NOT_ACHIEVED;
        } else if (completedTasks >= totalTasks) {
            status = ResultStatus.ACHIEVED;
        } else {
            status = ResultStatus.PARTIALLY_ACHIEVED;
        }

        String goalReview = "本轮目标围绕当前学习计划中的链路执行情况进行回顾。";
        java.util.List<String> completedProgress = java.util.List.of(
                "已完成 " + completedTasks + "/" + totalTasks + " 个任务"
        );

        java.util.List<String> unresolvedIssues = execution.getAggregatedIssueTags() != null
                ? execution.getAggregatedIssueTags()
                : java.util.List.of();

        java.util.List<String> evidenceSummary = execution.getEvidenceHighlights() != null
                ? execution.getEvidenceHighlights()
                : completedProgress;

        String summaryText;
        if (status == ResultStatus.ACHIEVED) {
            summaryText = "你已完成本轮计划中的所有任务，当前目标可以认为基本达成。";
        } else if (status == ResultStatus.PARTIALLY_ACHIEVED) {
            summaryText = "本轮计划已完成大部分任务，但仍有未完成部分或存在概念缺口，建议再做一轮补练。";
        } else {
            summaryText = "本轮完成度较低，建议先集中解决当前链路中的关键薄弱点。";
        }

        NextActionDecision nextAction = buildNextActionDecision(execution, methodRollup);

        return LearningReport.builder()
                .sessionId(sessionId)
                .resultStatus(status)
                .goalReview(goalReview)
                .completedProgress(completedProgress)
                .unresolvedIssues(unresolvedIssues)
                .evidenceSummary(evidenceSummary)
                .summaryText(summaryText)
                .nextAction(nextAction)
                .learningMethodProfile(methodRollup)
                .build();
    }

    private NextActionDecision buildNextActionDecision(ExecutionEvidenceSummary execution,
                                                       LearningMethodProfile methodRollup) {
        int totalTasks = execution.getTotalTasks() != null ? execution.getTotalTasks() : 0;
        int completedTasks = execution.getCompletedTasks() != null ? execution.getCompletedTasks() : 0;
        boolean hasGap = execution.getAggregatedIssueTags() != null
                && !execution.getAggregatedIssueTags().isEmpty();
        boolean methodWeak = methodRollup != null && "LOW".equals(methodRollup.getQuestioningQuality());

        NextActionType actionType;
        boolean requiresReplan;
        String reason;
        String nextEntryPoint;

        if (!hasGap && totalTasks > 0 && completedTasks >= totalTasks && !methodWeak) {
            actionType = NextActionType.CONTINUE;
            requiresReplan = false;
            reason = "任务完成度高且未检测到明显缺口，可以继续推进新的目标。";
            nextEntryPoint = "进入下一个相邻知识点或练习模块。";
        } else if (hasGap && completedTasks > 0) {
            actionType = NextActionType.REINFORCE;
            requiresReplan = false;
            reason = "存在概念或练习层面的薄弱点，更适合在当前目标上做巩固。";
            nextEntryPoint = "围绕未掌握要点安排一轮针对性巩固任务。";
        } else if (methodWeak && completedTasks >= totalTasks) {
            actionType = NextActionType.CHANGE_STRATEGY;
            requiresReplan = false;
            reason = "任务虽完成，但提问与探索偏少，建议下一轮改用更结构化的问法（举例/对比/自解释）。";
            nextEntryPoint = "在新任务中优先使用推荐问法模板。";
        } else {
            actionType = NextActionType.CHANGE_STRATEGY;
            requiresReplan = true;
            reason = "完成度不高且存在不稳定因素，适合调整学习策略重新规划。";
            nextEntryPoint = "通过重新规划调整切入方式或拆分更小粒度任务。";
        }

        java.util.List<String> adjustmentSignals = execution.getAggregatedIssueTags() != null
                ? execution.getAggregatedIssueTags()
                : java.util.List.of();

        return NextActionDecision.builder()
                .actionType(actionType)
                .reason(reason)
                .nextEntryPoint(nextEntryPoint)
                .adjustmentSignals(adjustmentSignals)
                .requiresReplan(requiresReplan)
                .build();
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
