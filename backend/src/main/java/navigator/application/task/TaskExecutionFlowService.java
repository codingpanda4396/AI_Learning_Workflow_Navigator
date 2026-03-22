package navigator.application.task;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CheckpointResponse;
import navigator.api.dto.CompletionRequirementItem;
import navigator.api.dto.CurrentGuidanceBlock;
import navigator.api.dto.CurrentTaskGuidanceData;
import navigator.api.dto.RecommendedUserActionItem;
import navigator.api.dto.SelfExplanationResponse;
import navigator.api.dto.TaskExecutionSummaryData;
import navigator.api.dto.TaskMessageRequest;
import navigator.api.dto.TaskMessageResponse;
import navigator.api.dto.TaskScaffoldResponse;
import navigator.application.FixedSampleData;
import navigator.application.guard.EntityLookupGuard;
import navigator.application.guard.SessionStateGuard;
import navigator.application.learning.LearningActionDetector;
import navigator.application.llm.TaskTutorOrchestrator;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.application.task.guidance.TaskGuidanceEngine;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.TaskMessageMetadata;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskScaffold;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.domain.model.TutorTurnResult;
import navigator.domain.policy.tutor.TutorInteractionPolicy;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.TaskMessageEntity;
import navigator.infrastructure.persistence.entity.TaskCheckpointResultEntity;
import navigator.infrastructure.persistence.repository.TaskCheckpointResultRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskExecutionFlowService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final EntityLookupGuard entityLookupGuard;
    private final LearningActionDetector actionDetector;
    private final TaskTutorOrchestrator tutorOrchestrator;
    private final TaskExecutionPersistenceService persistenceService;
    private final TaskCheckpointResultRepository checkpointResultRepository;
    private final TaskExecutionContextAssembler contextAssembler;
    private final CompletionEvaluator completionEvaluator;
    private final TaskGuidanceEngine taskGuidanceEngine;
    private final TaskExecutionEvidenceAccumulator evidenceAccumulator;

    public TaskExecutionFlowService(InMemoryStore store,
                                    SessionStateGuard sessionStateGuard,
                                    EntityLookupGuard entityLookupGuard,
                                    LearningActionDetector actionDetector,
                                    TaskTutorOrchestrator tutorOrchestrator,
                                    TaskExecutionPersistenceService persistenceService,
                                    TaskCheckpointResultRepository checkpointResultRepository,
                                    TaskExecutionContextAssembler contextAssembler,
                                    CompletionEvaluator completionEvaluator,
                                    TaskGuidanceEngine taskGuidanceEngine,
                                    TaskExecutionEvidenceAccumulator evidenceAccumulator) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.entityLookupGuard = entityLookupGuard;
        this.actionDetector = actionDetector;
        this.tutorOrchestrator = tutorOrchestrator;
        this.persistenceService = persistenceService;
        this.checkpointResultRepository = checkpointResultRepository;
        this.contextAssembler = contextAssembler;
        this.completionEvaluator = completionEvaluator;
        this.taskGuidanceEngine = taskGuidanceEngine;
        this.evidenceAccumulator = evidenceAccumulator;
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
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        return toScaffoldResponse(sessionId, taskId, rt);
    }

    public TaskMessageResponse postMessage(String taskId, TaskMessageRequest request) {
        String sessionId = request.getSessionId();
        String content = request.getContent();
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
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
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
        taskGuidanceEngine.syncGuidancePhaseFromExploreCount(rt);

        GuidanceDecision gd = taskGuidanceEngine.decideForExploreTurn(rt, action, rt.getScaffold(), resolveStrategyProfile(sessionId));

        boolean userAsked = isActiveQuestionType(action);
        boolean vague = content != null && !content.isBlank() && content.trim().length() < 12;
        boolean dar = action == LearningActionType.SEEK_DIRECT_ANSWER;
        TaskMessageMetadata userMeta = TaskMessageMetadata.builder()
                .messageType("USER")
                .guidancePhase(rt.getGuidancePhase())
                .guidanceIntent(gd.getIntent())
                .userActionType(action)
                .userAskedActively(userAsked)
                .vagueAnswer(vague)
                .directAnswerRisk(dar)
                .taskExecutionStateBefore(stateBefore.name())
                .taskExecutionStateAfter(stateBefore.name())
                .build();
        persistenceService.appendMessage(sessionId, taskId, "USER", content,
                action != null ? action.name() : null, stateBefore, stateBefore, "NONE", userMeta);

        TaskExecutionContext ctx = contextAssembler.assemble(sessionId, taskId);
        TutorTurnResult turn = tutorOrchestrator.exploreTurn(ctx, rt, content, action, gd);

        boolean hintGiven = gd.getIntent() == GuidanceIntent.GIVE_SCAFFOLD_HINT
                || gd.getIntent() == GuidanceIntent.CORRECT_MISCONCEPTION_LIGHT;
        TaskMessageMetadata asstMeta = TaskMessageMetadata.builder()
                .messageType("ASSISTANT")
                .guidancePhase(rt.getGuidancePhase())
                .guidanceIntent(gd.getIntent())
                .hintGiven(hintGiven)
                .taskExecutionStateBefore(stateBefore.name())
                .taskExecutionStateAfter(rt.getState().name())
                .build();
        persistenceService.appendMessage(sessionId, taskId, "ASSISTANT", turn.getAssistantReply(),
                null, stateBefore, rt.getState(), turn.getFallbackMode(), asstMeta);

        var delta = evidenceAccumulator.accumulateExploreTurn(rt, action, gd, content, turn.getAssistantReply());

        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        rt.getScaffold().setCurrentExecutionState(rt.getState().name());
        return TaskMessageResponse.builder()
                .assistantReply(turn.getAssistantReply())
                .detectedAction(action != null ? action.name() : "GENERIC")
                .taskState(rt.getState().name())
                .nextSuggestedPrompts(turn.getSuggestedFollowups())
                .fallbackMode(turn.getFallbackMode())
                .guidanceIntent(gd.getIntent() != null ? gd.getIntent().name() : null)
                .guidancePhase(rt.getGuidancePhase() != null ? rt.getGuidancePhase().name() : null)
                .evidenceDelta(delta)
                .whetherCanComplete(rt.getState() == TaskExecutionState.PASS)
                .recommendedUserActions(recommendedActionsForPhase(rt.getGuidancePhase()))
                .build();
    }

    public CurrentTaskGuidanceData getCurrentTaskGuidance(String sessionId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null || state.getTaskSequence() == null) {
            return null;
        }
        int idx = state.getCurrentTaskIndex();
        List<String> seq = state.getTaskSequence();
        if (idx >= seq.size()) {
            return null;
        }
        String taskId = seq.get(idx);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskExecutionRuntime rt = loadRuntimeBundled(sessionId, taskId);
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        return CurrentTaskGuidanceData.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .taskExecutionState(rt.getState().name())
                .guidancePhase(rt.getGuidancePhase() != null ? rt.getGuidancePhase().name() : null)
                .currentGuidance(guidanceBlockForPhase(rt.getGuidancePhase()))
                .recommendedUserActions(recommendedActionsForPhase(rt.getGuidancePhase()))
                .completionRequirements(buildCompletionRequirements(rt))
                .policyVersion(TutorInteractionPolicy.POLICY_VERSION)
                .build();
    }

    public TaskExecutionSummaryData getExecutionSummary(String sessionId, String taskId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        TaskExecutionRuntime rt = loadRuntimeBundled(sessionId, taskId);
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        Map<String, Boolean> phaseProgress = new LinkedHashMap<>();
        var visited = rt.getEvidenceSnapshot().getCompletedGuidancePhases();
        for (LearningGuidancePhase p : LearningGuidancePhase.values()) {
            phaseProgress.put(p.name(), visited != null && visited.contains(p));
        }
        return TaskExecutionSummaryData.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .taskExecutionState(rt.getState().name())
                .guidancePhase(rt.getGuidancePhase() != null ? rt.getGuidancePhase().name() : null)
                .phaseProgress(phaseProgress)
                .evidenceSnapshot(TaskExecutionEvidenceAccumulator.copySnapshot(rt))
                .missingCompletionRequirements(missingClosureRequirements(rt))
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
        TaskExecutionContext ctx = contextAssembler.assemble(sessionId, taskId);
        ExecutableTaskSpec spec = ctx != null ? ctx.getExecutableTaskSpec() : null;
        EvaluationResult eval = completionEvaluator.evaluateSelfExplanation(rt, spec, content);
        String goal = bp.getGoal() != null ? bp.getGoal() : bp.getTitle();
        if (eval.isPass()) {
            rt.setSelfExplanationEvaluation("ACCEPTABLE");
            rt.setCheckpointQuestion("请用一两句话概括本任务的核心要点（任务：" + truncate(goal, 50) + "）");
            transition(sessionId, taskId, rt, TaskExecutionState.CHECK, "self_explain_ok");
            evidenceAccumulator.markSelfExplanationSubmitted(rt);
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
        transition(sessionId, taskId, rt, TaskExecutionState.REMEDIAL, "self_explain_fail");
        evidenceAccumulator.markSelfExplanationSubmitted(rt);
        List<String> missingPoints = eval.getMissingDimensions() != null && !eval.getMissingDimensions().isEmpty()
                ? eval.getMissingDimensions().stream().map(d -> "缺少：" + d).toList()
                : List.of("解释偏短，请结合任务目标补充关键概念或步骤");
        persistenceService.appendMessage(sessionId, taskId, "SYSTEM",
                "SELF_EXPLAIN evaluation=WEAK", null, TaskExecutionState.SELF_EXPLAIN, TaskExecutionState.REMEDIAL, "NONE");
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        return SelfExplanationResponse.builder()
                .evaluation("WEAK")
                .missingPoints(missingPoints)
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
        TaskExecutionContext ctx = contextAssembler.assemble(sessionId, taskId);
        ExecutableTaskSpec spec = ctx != null ? ctx.getExecutableTaskSpec() : null;
        EvaluationResult eval = completionEvaluator.evaluateCheckpoint(rt, spec, answer);
        if (eval.isPass()) {
            transition(sessionId, taskId, rt, TaskExecutionState.PASS, "checkpoint_pass");
            rt.getScaffold().setCurrentExecutionState(TaskExecutionState.PASS.name());
            evidenceAccumulator.markCheckpointPassed(rt, true);
            checkpointResultRepository.save(toCheckpointEntity(sessionId, taskId, rt.getCheckpointQuestion(), answer,
                    "PASS", eval.getReason() != null ? eval.getReason() : "回答覆盖基本要点，可标记任务完成", null));
            persistenceService.appendMessage(sessionId, taskId, "SYSTEM", "CHECKPOINT result=PASS",
                    null, TaskExecutionState.CHECK, TaskExecutionState.PASS, "NONE");
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
            return CheckpointResponse.builder()
                    .result("PASS")
                    .reason(eval.getReason() != null ? eval.getReason() : "回答覆盖基本要点，可标记任务完成")
                    .suggestedRemedialAction(null)
                    .taskState(TaskExecutionState.PASS.name())
                    .build();
        }
        transition(sessionId, taskId, rt, TaskExecutionState.REMEDIAL, "checkpoint_fail");
        rt.getScaffold().setCurrentExecutionState(TaskExecutionState.REMEDIAL.name());
        evidenceAccumulator.markCheckpointPassed(rt, false);
        String remedialHint = eval.getMissingDimensions() != null && !eval.getMissingDimensions().isEmpty()
                ? "请补充：" + String.join("、", eval.getMissingDimensions())
                : "请再看一遍任务目标，用一句话说出最关键的术语或关系";
        checkpointResultRepository.save(toCheckpointEntity(sessionId, taskId, rt.getCheckpointQuestion(), answer,
                "FAIL", eval.getReason() != null ? eval.getReason() : "回答过短或未触及核心，建议回到探索再试", remedialHint));
        persistenceService.appendMessage(sessionId, taskId, "SYSTEM", "CHECKPOINT result=FAIL",
                null, TaskExecutionState.CHECK, TaskExecutionState.REMEDIAL, "NONE");
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        return CheckpointResponse.builder()
                .result("FAIL")
                .reason(eval.getReason() != null ? eval.getReason() : "回答过短或未触及核心，建议回到探索再试")
                .suggestedRemedialAction(remedialHint)
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

    private TaskExecutionRuntime loadRuntimeBundled(String sessionId, String taskId) {
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt == null) {
                rt = new TaskExecutionRuntime();
            }
            store.getTaskExecutionRuntimes().put(key, rt);
        }
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        return rt;
    }

    private static boolean isActiveQuestionType(LearningActionType action) {
        return action == LearningActionType.ASK_FOR_EXPLANATION
                || action == LearningActionType.ASK_FOR_EXAMPLE
                || action == LearningActionType.ASK_FOR_COMPARISON
                || action == LearningActionType.ASK_FOR_SIMPLIFICATION;
    }

    private static List<RecommendedUserActionItem> recommendedActionsForPhase(LearningGuidancePhase phase) {
        LearningGuidancePhase p = phase != null ? phase : LearningGuidancePhase.CLARIFY_GOAL;
        return switch (p) {
            case CLARIFY_GOAL -> List.of(
                    RecommendedUserActionItem.builder().code("RESTATE_GOAL").label("用一句话复述本任务要达成什么").build(),
                    RecommendedUserActionItem.builder().code("ASK_WHY").label("提问：完成标准里哪个词还不清楚？").build());
            case BUILD_FRAME -> List.of(
                    RecommendedUserActionItem.builder().code("LIST_CONCEPTS").label("列出 3 个关键词并说明关系").build(),
                    RecommendedUserActionItem.builder().code("ASK_EXAMPLE").label("请求一个最小例子（不要直接要答案）").build());
            case TRY_EXPRESS -> List.of(
                    RecommendedUserActionItem.builder().code("OWN_WORDS").label("用自己的话描述思路，即使不完整").build());
            case PROBE_GAPS -> List.of(
                    RecommendedUserActionItem.builder().code("BOUNDARY").label("追问：反例或边界情况是什么？").build());
            case META_REFLECT -> List.of(
                    RecommendedUserActionItem.builder().code("REFLECT").label("写下仍模糊的一点和下一步练习方式").build());
            case TRANSITION_HINT -> List.of(
                    RecommendedUserActionItem.builder().code("SELF_EXPLAIN_API").label("调用自我解释接口巩固理解").build());
        };
    }

    private static CurrentGuidanceBlock guidanceBlockForPhase(LearningGuidancePhase phase) {
        LearningGuidancePhase p = phase != null ? phase : LearningGuidancePhase.CLARIFY_GOAL;
        return switch (p) {
            case CLARIFY_GOAL -> CurrentGuidanceBlock.builder()
                    .title("对齐目标")
                    .bullets(List.of("先确认任务在解决什么问题", "对照完成标准，标出你不确定的表述"))
                    .build();
            case BUILD_FRAME -> CurrentGuidanceBlock.builder()
                    .title("搭骨架")
                    .bullets(List.of("把概念/步骤拆成小块", "不必一次正确，先求结构完整"))
                    .build();
            case TRY_EXPRESS -> CurrentGuidanceBlock.builder()
                    .title("尝试表达")
                    .bullets(List.of("用你自己的话说出当前理解", "允许错误，重点是暴露思路"))
                    .build();
            case PROBE_GAPS -> CurrentGuidanceBlock.builder()
                    .title("查缺补漏")
                    .bullets(List.of("对模糊点提出具体问题", "尝试举例或对比帮助定位"))
                    .build();
            case META_REFLECT -> CurrentGuidanceBlock.builder()
                    .title("反思")
                    .bullets(List.of("哪里仍不确定？", "下一步打算怎么练？"))
                    .build();
            case TRANSITION_HINT -> CurrentGuidanceBlock.builder()
                    .title("准备收束")
                    .bullets(List.of("探索轮次已较充分", "建议进入自我解释，再完成微检查"))
                    .build();
        };
    }

    private static List<CompletionRequirementItem> buildCompletionRequirements(TaskExecutionRuntime rt) {
        List<CompletionRequirementItem> list = new ArrayList<>();
        list.add(CompletionRequirementItem.builder()
                .code("CHECKPOINT_PASS")
                .satisfied(rt.getState() == TaskExecutionState.PASS)
                .hint("须完成探索、自解释与微检查直至 PASS")
                .build());
        list.add(CompletionRequirementItem.builder()
                .code("CLOSURE_SUMMARY")
                .satisfied(false)
                .hint("complete 时提交 summaryText（≥10 字）")
                .build());
        list.add(CompletionRequirementItem.builder()
                .code("CLOSURE_FRAMEWORK")
                .satisfied(false)
                .hint("complete 时提交至少 2 条 learnedFrameworkPoints")
                .build());
        list.add(CompletionRequirementItem.builder()
                .code("CLOSURE_NEXT_PRACTICE")
                .satisfied(false)
                .hint("complete 时提交 nextPracticeIntent")
                .build());
        return list;
    }

    private static List<CompletionRequirementItem> missingClosureRequirements(TaskExecutionRuntime rt) {
        List<CompletionRequirementItem> miss = new ArrayList<>();
        for (CompletionRequirementItem r : buildCompletionRequirements(rt)) {
            if (!r.isSatisfied()) {
                miss.add(r);
            }
        }
        return miss;
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
