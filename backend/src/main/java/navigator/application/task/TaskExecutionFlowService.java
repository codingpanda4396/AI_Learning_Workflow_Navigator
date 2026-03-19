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
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskScaffold;
import navigator.domain.model.TutorTurnResult;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.TaskMessageEntity;
import navigator.infrastructure.persistence.entity.TaskCheckpointResultEntity;
import navigator.infrastructure.persistence.repository.TaskCheckpointResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskExecutionFlowService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final EntityLookupGuard entityLookupGuard;
    private final LearningActionDetector actionDetector;
    private final TaskTutorOrchestrator tutorOrchestrator;
    private final TaskExecutionPersistenceService persistenceService;
    private final TaskCheckpointResultRepository checkpointResultRepository;

    public TaskExecutionFlowService(InMemoryStore store,
                                    SessionStateGuard sessionStateGuard,
                                    EntityLookupGuard entityLookupGuard,
                                    LearningActionDetector actionDetector,
                                    TaskTutorOrchestrator tutorOrchestrator,
                                    TaskExecutionPersistenceService persistenceService,
                                    TaskCheckpointResultRepository checkpointResultRepository) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.entityLookupGuard = entityLookupGuard;
        this.actionDetector = actionDetector;
        this.tutorOrchestrator = tutorOrchestrator;
        this.persistenceService = persistenceService;
        this.checkpointResultRepository = checkpointResultRepository;
    }

    public TaskScaffoldResponse getScaffold(String sessionId, String taskId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = resolveBlueprint(sessionId, taskId);
        if (bp == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task blueprint not found");
        }
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt != null) {
                store.getTaskExecutionRuntimes().put(key, rt);
            } else {
                rt = new TaskExecutionRuntime();
                store.getTaskExecutionRuntimes().put(key, rt);
            }
        }
        if (rt.getScaffold() == null) {
            TaskScaffold scaffold = TaskScaffoldFactory.build(sessionId, bp, resolveStrategyProfile(sessionId));
            if (scaffold.getScaffoldId() == null || scaffold.getScaffoldId().isBlank()) {
                scaffold.setScaffoldId(TaskExecutionRuntime.newScaffoldId());
            }
            scaffold.setCurrentExecutionState(TaskExecutionState.ORIENT.name());
            rt.setScaffold(scaffold);
            transition(sessionId, taskId, rt, TaskExecutionState.ORIENT, "scaffold_loaded");
        } else {
            rt.getScaffold().setCurrentExecutionState(rt.getState().name());
        }
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        return toScaffoldResponse(sessionId, taskId, rt);
    }

    public TaskMessageResponse postMessage(String taskId, String sessionId, String content) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = requireBlueprint(sessionId, taskId);
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt != null) {
                store.getTaskExecutionRuntimes().put(key, rt);
            } else {
                rt = new TaskExecutionRuntime();
                store.getTaskExecutionRuntimes().put(key, rt);
            }
        }
        if (rt.getScaffold() == null) {
            TaskScaffold scaffold = TaskScaffoldFactory.build(sessionId, bp, resolveStrategyProfile(sessionId));
            scaffold.setCurrentExecutionState(TaskExecutionState.ORIENT.name());
            rt.setScaffold(scaffold);
            if (scaffold.getScaffoldId() == null || scaffold.getScaffoldId().isBlank()) {
                scaffold.setScaffoldId(TaskExecutionRuntime.newScaffoldId());
            }
            transition(sessionId, taskId, rt, TaskExecutionState.ORIENT, "lazy_scaffold");
        }
        LearningActionType action = actionDetector.detect(content);
        rt.recordAction(action);

        TaskExecutionState st = rt.getState();
        if (st == TaskExecutionState.ORIENT || st == TaskExecutionState.INIT) {
            transition(sessionId, taskId, rt, TaskExecutionState.EXPLORE, "first_user_message");
        } else if (st == TaskExecutionState.REMEDIAL) {
            transition(sessionId, taskId, rt, TaskExecutionState.EXPLORE, "remedial_continue");
        } else if (st != TaskExecutionState.EXPLORE) {
            throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE,
                    "当前阶段请使用自我解释或微检查接口，状态=" + st);
        }

        TaskExecutionState stateBefore = rt.getState();
        rt.setExploreTurnCount(rt.getExploreTurnCount() + 1);
        persistenceService.appendMessage(sessionId, taskId, "USER", content,
                action != null ? action.name() : null, stateBefore, stateBefore, "NONE");
        TutorTurnResult turn = tutorOrchestrator.exploreTurn(rt, bp, content, action);
        persistenceService.appendMessage(sessionId, taskId, "ASSISTANT", turn.getAssistantReply(),
                null, stateBefore, rt.getState(), turn.getFallbackMode());
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        rt.getScaffold().setCurrentExecutionState(rt.getState().name());
        return TaskMessageResponse.builder()
                .assistantReply(turn.getAssistantReply())
                .detectedAction(action != null ? action.name() : "GENERIC")
                .taskState(rt.getState().name())
                .nextSuggestedPrompts(turn.getSuggestedFollowups())
                .fallbackMode(turn.getFallbackMode())
                .build();
    }

    public SelfExplanationResponse postSelfExplanation(String taskId, String sessionId, String content) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskBlueprint bp = requireBlueprint(sessionId, taskId);
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt == null) {
                throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE, "请先加载脚手架或发送探索消息");
            }
            store.getTaskExecutionRuntimes().put(key, rt);
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
        transition(sessionId, taskId, rt, TaskExecutionState.SELF_EXPLAIN, "user_self_explain");
        String goal = bp.getGoal() != null ? bp.getGoal() : bp.getTitle();
        if (content != null && content.trim().length() >= 35) {
            rt.setSelfExplanationEvaluation("ACCEPTABLE");
            rt.setCheckpointQuestion("请用一两句话概括本任务的核心要点（任务：" + truncate(goal, 50) + "）");
            transition(sessionId, taskId, rt, TaskExecutionState.CHECK, "self_explain_ok");
            persistenceService.appendMessage(sessionId, taskId, "SYSTEM",
                    "SELF_EXPLAIN evaluation=ACCEPTABLE", null, TaskExecutionState.SELF_EXPLAIN, TaskExecutionState.CHECK, "NONE");
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
            return SelfExplanationResponse.builder()
                    .evaluation("ACCEPTABLE")
                    .missingPoints(List.of())
                    .nextAction("MOVE_TO_CHECK")
                    .taskState(TaskExecutionState.CHECK.name())
                    .checkpointQuestion(rt.getCheckpointQuestion())
                    .build();
        }
        rt.setSelfExplanationEvaluation("WEAK");
        transition(sessionId, taskId, rt, TaskExecutionState.REMEDIAL, "self_explain_too_short");
        persistenceService.appendMessage(sessionId, taskId, "SYSTEM",
                "SELF_EXPLAIN evaluation=WEAK", null, TaskExecutionState.SELF_EXPLAIN, TaskExecutionState.REMEDIAL, "NONE");
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
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
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt == null || rt.getState() != TaskExecutionState.CHECK) {
                throw new BusinessException(BusinessErrorCode.INVALID_TASK_EXECUTION_STATE, "当前不在微检查阶段");
            }
            store.getTaskExecutionRuntimes().put(key, rt);
        }
        rt.recordAction(LearningActionType.ANSWER_CHECK);
        if (answer != null && answer.trim().length() >= 12) {
            transition(sessionId, taskId, rt, TaskExecutionState.PASS, "checkpoint_pass");
            rt.getScaffold().setCurrentExecutionState(TaskExecutionState.PASS.name());
            checkpointResultRepository.save(toCheckpointEntity(sessionId, taskId, rt.getCheckpointQuestion(), answer,
                    "PASS", "回答覆盖基本要点，可标记任务完成", null));
            persistenceService.appendMessage(sessionId, taskId, "SYSTEM", "CHECKPOINT result=PASS",
                    null, TaskExecutionState.CHECK, TaskExecutionState.PASS, "NONE");
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
            return CheckpointResponse.builder()
                    .result("PASS")
                    .reason("回答覆盖基本要点，可标记任务完成")
                    .suggestedRemedialAction(null)
                    .taskState(TaskExecutionState.PASS.name())
                    .build();
        }
        transition(sessionId, taskId, rt, TaskExecutionState.REMEDIAL, "checkpoint_fail");
        rt.getScaffold().setCurrentExecutionState(TaskExecutionState.REMEDIAL.name());
        checkpointResultRepository.save(toCheckpointEntity(sessionId, taskId, rt.getCheckpointQuestion(), answer,
                "FAIL", "回答过短或未触及核心，建议回到探索再试", "请再看一遍任务目标，用一句话说出最关键的术语或关系"));
        persistenceService.appendMessage(sessionId, taskId, "SYSTEM", "CHECKPOINT result=FAIL",
                null, TaskExecutionState.CHECK, TaskExecutionState.REMEDIAL, "NONE");
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
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
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
        }
        return rt != null ? rt.getCheckpointQuestion() : null;
    }

    private TaskScaffoldResponse toScaffoldResponse(String sessionId, String taskId, TaskExecutionRuntime rt) {
        var s = rt.getScaffold();
        List<TaskMessageEntity> recent = persistenceService.findRecentMessagesAscending(sessionId, taskId, 20);
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
                .executionSnapshot(TaskScaffoldResponse.ExecutionSnapshot.builder()
                        .currentState(rt.getState().name())
                        .exploreTurnCount(rt.getExploreTurnCount())
                        .checkpointQuestion(rt.getCheckpointQuestion())
                        .canComplete(rt.getState() == TaskExecutionState.PASS)
                        .build())
                .recentMessages(recent.stream().map(m -> TaskScaffoldResponse.RecentMessageItem.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .detectedAction(m.getDetectedAction())
                        .createdAt(m.getCreatedAt())
                        .build()).toList())
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

    private LearnerStrategyProfile resolveStrategyProfile(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        // diagnosisId -> sessionId 的映射已在 createSession 时写入 store；执行期反向查找即可（数据量小）。
        String diagnosisId = store.getDiagnosisToSession().entrySet().stream()
                .filter(e -> sessionId.equals(e.getValue()))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        return diagnosisId != null ? store.getLearnerStrategyProfiles().get(diagnosisId) : null;
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private void transition(String sessionId, String taskId, TaskExecutionRuntime rt, TaskExecutionState to, String reason) {
        TaskExecutionState from = rt.getState();
        if (from == to) {
            return;
        }
        rt.transitionTo(to, reason);
        persistenceService.appendTransition(sessionId, taskId, from, to, reason);
    }

    private TaskCheckpointResultEntity toCheckpointEntity(String sessionId, String taskId, String question, String answer,
                                                          String result, String reason, String remedial) {
        TaskCheckpointResultEntity e = new TaskCheckpointResultEntity();
        e.setSessionKey(sessionId);
        e.setTaskCode(taskId);
        e.setQuestion(question != null ? question : "");
        e.setAnswer(answer);
        e.setResult(result);
        e.setReason(reason);
        e.setSuggestedRemedialAction(remedial);
        e.setCreatedAt(java.time.LocalDateTime.now());
        return e;
    }
}
