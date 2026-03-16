package navigator.application;

import navigator.api.dto.NextActionConfirmData;
import navigator.api.dto.ReportData;
import navigator.application.guard.SessionStateGuard;
import navigator.domain.model.LearningReport;
import navigator.domain.model.NextActionDecision;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

@Service
public class ReportApplicationService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;

    public ReportApplicationService(InMemoryStore store, SessionStateGuard sessionStateGuard) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
    }

    public ReportData getReport(String sessionId) {
        sessionStateGuard.requireSessionCompletedForReport(sessionId);
        LearningReport report = FixedSampleData.learningReport(sessionId);
        NextActionDecision decision = FixedSampleData.nextActionDecision();
        return ReportData.builder()
                .learningReport(report)
                .nextActionDecision(decision)
                .build();
    }

    public NextActionConfirmData confirmNextAction(String sessionId, navigator.domain.enums.NextActionType actionType) {
        boolean requiresReplan = false;
        String nextHint = "下一轮建议进入链表基本操作与简单练习。";
        return NextActionConfirmData.builder()
                .sessionId(sessionId)
                .acceptedAction(actionType)
                .requiresReplan(requiresReplan)
                .nextHint(nextHint)
                .build();
    }
}
