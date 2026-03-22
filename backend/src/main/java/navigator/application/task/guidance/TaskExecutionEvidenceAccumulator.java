package navigator.application.task.guidance;

import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.TaskExecutionEvidenceDelta;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;
import navigator.application.task.TaskExecutionRuntime;
import org.springframework.stereotype.Component;

/**
 * 按回合累计执行证据（规则侧）。
 */
@Component
public class TaskExecutionEvidenceAccumulator {

    private static final int VAGUE_ANSWER_MAX_LEN = 12;

    public TaskExecutionEvidenceDelta accumulateExploreTurn(TaskExecutionRuntime rt,
                                                            LearningActionType action,
                                                            GuidanceDecision decision,
                                                            String userContent,
                                                            String assistantReply) {
        ensureSnapshot(rt);
        TaskExecutionEvidenceSnapshot snap = rt.getEvidenceSnapshot();
        TaskExecutionEvidenceDelta.TaskExecutionEvidenceDeltaBuilder b = TaskExecutionEvidenceDelta.builder()
                .totalTurnsDelta(1);

        if (isActiveQuestion(action)) {
            b.userInitiatedQuestionTurnsDelta(1);
        }
        if (action == LearningActionType.SEEK_DIRECT_ANSWER) {
            b.directAnswerSeekCountDelta(1);
        }
        if (action == LearningActionType.CONFUSION_SIGNAL) {
            b.confusionSignalsCountDelta(1);
        }
        if (userContent != null && !userContent.isBlank() && userContent.trim().length() < VAGUE_ANSWER_MAX_LEN) {
            b.vagueUserReplyCountDelta(1);
        }

        GuidanceIntent intent = decision != null ? decision.getIntent() : null;
        if (intent == GuidanceIntent.GIVE_SCAFFOLD_HINT || intent == GuidanceIntent.CORRECT_MISCONCEPTION_LIGHT) {
            b.assistantHintTurnsDelta(1);
        }
        if (intent == GuidanceIntent.REDIRECT_OFF_TASK) {
            b.assistantRedirectTurnsDelta(1);
        }

        TaskExecutionEvidenceDelta delta = b.build();
        applyDelta(snap, delta);
        snap.setDirectAnswerDependencyScore(computeDependencyScore(snap));
        return delta;
    }

    public void markSelfExplanationSubmitted(TaskExecutionRuntime rt) {
        ensureSnapshot(rt);
        rt.getEvidenceSnapshot().setSelfExplanationSubmitted(true);
    }

    public void markCheckpointPassed(TaskExecutionRuntime rt, boolean passed) {
        ensureSnapshot(rt);
        rt.getEvidenceSnapshot().setCheckpointPassed(passed);
    }

    public static TaskExecutionEvidenceSnapshot ensureSnapshot(TaskExecutionRuntime rt) {
        if (rt.getEvidenceSnapshot() == null) {
            rt.setEvidenceSnapshot(TaskExecutionEvidenceSnapshot.builder().build());
        }
        return rt.getEvidenceSnapshot();
    }

    public static TaskExecutionEvidenceSnapshot copySnapshot(TaskExecutionRuntime rt) {
        if (rt == null || rt.getEvidenceSnapshot() == null) {
            return TaskExecutionEvidenceSnapshot.builder().build();
        }
        var s = rt.getEvidenceSnapshot();
        return TaskExecutionEvidenceSnapshot.builder()
                .totalTurns(s.getTotalTurns())
                .userInitiatedQuestionTurns(s.getUserInitiatedQuestionTurns())
                .assistantHintTurns(s.getAssistantHintTurns())
                .assistantRedirectTurns(s.getAssistantRedirectTurns())
                .directAnswerSeekCount(s.getDirectAnswerSeekCount())
                .vagueUserReplyCount(s.getVagueUserReplyCount())
                .confusionSignalsCount(s.getConfusionSignalsCount())
                .completedGuidancePhases(new java.util.LinkedHashSet<>(s.getCompletedGuidancePhases()))
                .frameworkArticulated(s.isFrameworkArticulated())
                .reflectionArticulated(s.isReflectionArticulated())
                .selfExplanationSubmitted(s.isSelfExplanationSubmitted())
                .checkpointPassed(s.isCheckpointPassed())
                .directAnswerDependencyScore(s.getDirectAnswerDependencyScore())
                .build();
    }

    private static void applyDelta(TaskExecutionEvidenceSnapshot snap, TaskExecutionEvidenceDelta d) {
        snap.setTotalTurns(snap.getTotalTurns() + d.getTotalTurnsDelta());
        snap.setUserInitiatedQuestionTurns(snap.getUserInitiatedQuestionTurns() + d.getUserInitiatedQuestionTurnsDelta());
        snap.setAssistantHintTurns(snap.getAssistantHintTurns() + d.getAssistantHintTurnsDelta());
        snap.setAssistantRedirectTurns(snap.getAssistantRedirectTurns() + d.getAssistantRedirectTurnsDelta());
        snap.setDirectAnswerSeekCount(snap.getDirectAnswerSeekCount() + d.getDirectAnswerSeekCountDelta());
        snap.setVagueUserReplyCount(snap.getVagueUserReplyCount() + d.getVagueUserReplyCountDelta());
        snap.setConfusionSignalsCount(snap.getConfusionSignalsCount() + d.getConfusionSignalsCountDelta());
    }

    private static double computeDependencyScore(TaskExecutionEvidenceSnapshot s) {
        double raw = s.getDirectAnswerSeekCount() * 0.35
                + Math.max(0, s.getVagueUserReplyCount() - s.getUserInitiatedQuestionTurns()) * 0.08;
        return Math.min(1.0, raw);
    }

    private static boolean isActiveQuestion(LearningActionType action) {
        return action == LearningActionType.ASK_FOR_EXPLANATION
                || action == LearningActionType.ASK_FOR_EXAMPLE
                || action == LearningActionType.ASK_FOR_COMPARISON
                || action == LearningActionType.ASK_FOR_SIMPLIFICATION;
    }
}
