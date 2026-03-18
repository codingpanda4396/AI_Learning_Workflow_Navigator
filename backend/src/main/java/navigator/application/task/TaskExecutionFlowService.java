package navigator.application.task;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CheckpointResponse;
import navigator.api.dto.SelfExplanationResponse;
import navigator.api.dto.TaskMessageResponse;
import navigator.api.dto.TaskScaffoldResponse;
import navigator.application.FixedSampleData;
import navigator.application.guard.EntityLookupGuard;
import navigator.application.guard.SessionStateGuard;
import navigator.application.learning.LearningActionDetector;
import navigator.application.llm.TaskTutorOrchestrator;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskScaffold;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskExecutionFlowService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final EntityLookupGuard entityLookupGuard;
    private final LearningActionDetector actionDetector;
    private final TaskTutorOrchestrator tutorOrchestrator;

    public TaskExecutionFlowService(InMemoryStore store,
                                    SessionStateGuard sessionStateGuard,
                                    EntityLookupGuard entityLookupGuard,
                                    LearningActionDetector actionDetector,
                                    TaskTutorOrchestrator tutorOrchestrator) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.entityLookupGuard = entityLookupGuard;
        this.actionDetector = actionDetector;
        this.tutorOrchestrator = tutorOrchestrator;
    }

    public TaskScaffoldResponse getScaffold(String sessionId, String taskId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = resolveBlueprint(sessionId, taskId);
        if (bp == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task blueprint not found");
        }
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().computeIfAbsent(key, k -> new TaskExecutionRuntime());
        if (rt.getScaffold() == null) {
            TaskScaffold scaffold = TaskScaffoldFactory.build(sessionId, bp);
            scaffold.setCurrentExecutionState(TaskExecutionState.ORIENT.name());
            rt.setScaffold(scaffold);
            rt.transitionTo(TaskExecutionState.ORIENT, "scaffold_loaded");
        } else {
            rt.getScaffold().setCurrentExecutionState(rt.getState().name());
        }
        return toScaffoldResponse(rt);
    }

    public TaskMessageResponse postMessage(String taskId, String sessionId, String content) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = requireBlueprint(sessionId, taskId);
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().computeIfAbsent(key, k -> new TaskExecutionRuntime());
        if (rt.getScaffold() == null) {
            TaskScaffold scaffold = TaskScaffoldFactory.build(sessionId, bp);
            rt.setScaffold(scaffold);
            rt.transitionTo(TaskExecutionState.ORIENT, "lazy_scaffold");
        }
        LearningActionType action = actionDetector.detect(content);
        rt.recordAction(action);

        TaskExecutionState st = rt.getState();
        if (st == TaskExecutionState.ORIENT || st == TaskExecutionState.INIT) {
            rt.transitionTo(TaskExecutionState.EXPLORE, "first_user_message");
        } else if (st == TaskExecutionState.REMEDIAL) {
            rt.transitionTo(TaskExecutionState.EXPLORE, "remedial_continue");
        } else if (st != TaskExecutionState.EXPLORE) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE,
                    "当前阶段请使用自我解释或微检查接口，状态=" + st);
        }

        rt.setExploreTurnCount(rt.getExploreTurnCount() + 1);
        TaskTutorOrchestrator.TutorTurnResult turn = tutorOrchestrator.exploreTurn(rt, bp, content, action);
        rt.getScaffold().setCurrentExecutionState(rt.getState().name());
        return TaskMessageResponse.builder()
                .assistantReply(turn.assistantReply())
                .detectedAction(action.name())
                .taskState(rt.getState().name())
                .nextSuggestedPrompts(turn.suggestedNextPrompts())
                .fallbackMode(turn.fallbackMode())
                .build();
    }

    public SelfExplanationResponse postSelfExplanation(String taskId, String sessionId, String content) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = requireBlueprint(sessionId, taskId);
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE, "请先加载脚手架或发送探索消息");
        }
        TaskExecutionState st = rt.getState();
        if (st != TaskExecutionState.EXPLORE && st != TaskExecutionState.REMEDIAL) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE,
                    "仅在探索或补救后可提交自我解释");
        }
        int minExplore = rt.getScaffold() != null && rt.getScaffold().getSuggestedExploreTurns() != null
                ? rt.getScaffold().getSuggestedExploreTurns() : 1;
        if (rt.getExploreTurnCount() < minExplore) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE,
                    "请先完成至少 " + minExplore + " 轮探索对话");
        }
        rt.recordAction(LearningActionType.SELF_EXPLANATION);
        rt.transitionTo(TaskExecutionState.SELF_EXPLAIN, "user_self_explain");
        String goal = bp.getGoal() != null ? bp.getGoal() : bp.getTitle();
        if (content != null && content.trim().length() >= 35) {
            rt.setSelfExplanationEvaluation("ACCEPTABLE");
            rt.setCheckpointQuestion("请用一两句话概括本任务的核心要点（任务：" + truncate(goal, 50) + "）");
            rt.transitionTo(TaskExecutionState.CHECK, "self_explain_ok");
            return SelfExplanationResponse.builder()
                    .evaluation("ACCEPTABLE")
                    .missingPoints(List.of())
                    .nextAction("MOVE_TO_CHECK")
                    .taskState(TaskExecutionState.CHECK.name())
                    .checkpointQuestion(rt.getCheckpointQuestion())
                    .build();
        }
        rt.setSelfExplanationEvaluation("WEAK");
        rt.transitionTo(TaskExecutionState.REMEDIAL, "self_explain_too_short");
        return SelfExplanationResponse.builder()
                .evaluation("WEAK")
                .missingPoints(List.of("解释偏短，请结合任务目标补充关键概念或步骤"))
                .nextAction("REMEDIAL")
                .taskState(TaskExecutionState.REMEDIAL.name())
                .build();
    }

    public CheckpointResponse postCheckpoint(String taskId, String sessionId, String answer) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null || rt.getState() != TaskExecutionState.CHECK) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE, "当前不在微检查阶段");
        }
        rt.recordAction(LearningActionType.ANSWER_CHECK);
        if (answer != null && answer.trim().length() >= 12) {
            rt.transitionTo(TaskExecutionState.PASS, "checkpoint_pass");
            rt.getScaffold().setCurrentExecutionState(TaskExecutionState.PASS.name());
            return CheckpointResponse.builder()
                    .result("PASS")
                    .reason("回答覆盖基本要点，可标记任务完成")
                    .suggestedRemedialAction(null)
                    .taskState(TaskExecutionState.PASS.name())
                    .build();
        }
        rt.transitionTo(TaskExecutionState.REMEDIAL, "checkpoint_fail");
        rt.getScaffold().setCurrentExecutionState(TaskExecutionState.REMEDIAL.name());
        return CheckpointResponse.builder()
                .result("FAIL")
                .reason("回答过短或未触及核心，建议回到探索再试")
                .suggestedRemedialAction("请再看一遍任务目标，用一句话说出最关键的术语或关系")
                .taskState(TaskExecutionState.REMEDIAL.name())
                .build();
    }

    public String getCheckpointQuestion(String sessionId, String taskId) {
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null || rt.getCheckpointQuestion() == null) {
            return null;
        }
        return rt.getCheckpointQuestion();
    }

    private TaskScaffoldResponse toScaffoldResponse(TaskExecutionRuntime rt) {
        var s = rt.getScaffold();
        return TaskScaffoldResponse.builder()
                .taskId(s.getTaskId())
                .taskType(s.getTaskType())
                .learningObjective(s.getLearningObjective())
                .whyThisTask(s.getWhyThisTask())
                .recommendedAskTemplates(s.getRecommendedAskTemplates())
                .recommendedFollowupTemplates(s.getRecommendedFollowupTemplates())
                .selfCheckTemplates(s.getSelfCheckTemplates())
                .fallbackHints(s.getFallbackHints())
                .completionSignals(s.getCompletionSignals())
                .antiPatterns(s.getAntiPatterns())
                .currentExecutionState(rt.getState().name())
                .build();
    }

    private TaskBlueprint requireBlueprint(String sessionId, String taskId) {
        TaskBlueprint bp = resolveBlueprint(sessionId, taskId);
        if (bp == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task blueprint not found");
        }
        return bp;
    }

    private TaskBlueprint resolveBlueprint(String sessionId, String taskId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state != null && state.getPlanId() != null) {
            LearningPlanPreview plan = store.getPlanPreviews().get(state.getPlanId());
            if (plan != null && plan.getTasks() != null) {
                return plan.getTasks().stream()
                        .filter(t -> t.getTaskId().equals(taskId))
                        .findFirst()
                        .orElse(null);
            }
        }
        return FixedSampleData.taskBlueprints().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
