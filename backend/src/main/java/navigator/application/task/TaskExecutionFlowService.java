package navigator.application.task;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CheckpointResponse;
import navigator.api.dto.CompletionRequirementItem;
import navigator.api.dto.CurrentGuidanceBlock;
import navigator.api.dto.CurrentTaskGuidanceData;
import navigator.api.dto.ExecutionFeedbackActionItem;
import navigator.api.dto.ExecutionFeedbackBoard;
import navigator.api.dto.RecommendedUserActionItem;
import navigator.api.dto.SelfExplanationResponse;
import navigator.api.dto.TaskExecutionSummaryData;
import navigator.api.dto.TaskMessageRequest;
import navigator.api.dto.TaskMessageResponse;
import navigator.api.dto.TaskScaffoldResponse;
import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;
import navigator.api.dto.scaffold.ReflectionSummary;
import navigator.application.FixedSampleData;
import navigator.application.guard.EntityLookupGuard;
import navigator.application.guard.SessionStateGuard;
import navigator.application.learning.LearningActionDetector;
import navigator.application.knowledge.KnowledgePackMetadata;
import navigator.application.llm.TaskTutorOrchestrator;
import navigator.application.rule.engine.RuleResult;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.application.task.guidance.TaskGuidanceEngine;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.CognitiveUnit;
import navigator.domain.model.LearningScaffoldEngineState;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.TaskMessageMetadata;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.ScaffoldPrompt;
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
        ensureScaffoldCognitiveUnits(sessionId, taskId, rt);
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
        ensureScaffoldCognitiveUnits(sessionId, taskId, rt);
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
        RuleResult<LearningGuidancePhase> phaseSelection = taskGuidanceEngine.syncGuidancePhase(rt, action, content);

        GuidanceDecision gd = taskGuidanceEngine.decideForExploreTurn(rt, action, rt.getScaffold(), resolveStrategyProfile(sessionId));
        gd.setPhaseRuleId(phaseSelection.getRuleId());
        gd.setPhaseReason(phaseSelection.getReason());

        boolean userAsked = isActiveQuestionType(action);
        boolean vague = content != null && !content.isBlank() && content.trim().length() < 12;
        boolean dar = action == LearningActionType.SEEK_DIRECT_ANSWER;
        TaskMessageMetadata userMeta = TaskMessageMetadata.builder()
                .messageType("USER")
                .guidancePhase(rt.getGuidancePhase())
                .guidanceIntent(gd.getIntent())
                .guidancePhaseRuleId(gd.getPhaseRuleId())
                .guidancePhaseReason(gd.getPhaseReason())
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
                .guidancePhaseRuleId(gd.getPhaseRuleId())
                .guidancePhaseReason(gd.getPhaseReason())
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
                .feedbackBoard(buildExploreFeedbackBoard(content, turn.getAssistantReply(), turn.getSuggestedFollowups()))
                .guidanceIntent(gd.getIntent() != null ? gd.getIntent().name() : null)
                .guidancePhase(rt.getGuidancePhase() != null ? rt.getGuidancePhase().name() : null)
                .guidancePhaseRuleId(gd.getPhaseRuleId())
                .guidancePhaseReason(gd.getPhaseReason())
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
        ensureScaffoldCognitiveUnits(sessionId, taskId, rt);
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
                    .feedbackBoard(ExecutionFeedbackBoard.builder()
                            .correct("你已经把这一步的主要关系讲出来了。")
                            .missing("当前没有关键缺口。")
                            .confused("如果还不稳，就用微检查题确认判断依据。")
                            .nextFix("进入检查题，用一两句话独立作答。")
                            .actions(List.of())
                            .build())
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
                .feedbackBoard(ExecutionFeedbackBoard.builder()
                        .correct("主线方向是对的，先别整段重写。")
                        .missing(firstOf(missingPoints, "还缺一句关键判断或因果。"))
                        .confused("你现在最容易把方向对和表达完整混在一起。")
                        .nextFix("只补当前暴露出的那个缺口，再重新表达。")
                        .actions(defaultFixActions())
                        .build())
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
        ensureScaffoldCognitiveUnits(sessionId, taskId, rt);
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
                    .feedbackBoard(ExecutionFeedbackBoard.builder()
                            .correct("你已经能独立做出判断，并给出依据。")
                            .missing("当前没有新的缺口。")
                            .confused("接下来不需要继续发散，直接收束本任务。")
                            .nextFix("进入本轮总结，写下一句总结和两个带走要点。")
                            .actions(List.of())
                            .build())
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
                .feedbackBoard(ExecutionFeedbackBoard.builder()
                        .correct("前面的准备已经够了，现在只差判断依据。")
                        .missing(remedialHint)
                        .confused("你可能把结论和支持结论的依据混在了一起。")
                        .nextFix("回到当前主卡，把判断依据补成一句完整的话。")
                        .actions(defaultFixActions())
                        .build())
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
        KnowledgePackMetadata.PackMeta packMeta = resolvePackMeta(sessionId);
        List<TaskMessageEntity> recent = persistenceService.findRecentMessagesAscending(sessionId, taskId, 20);
        return TaskScaffoldResponse.builder()
                .taskId(s.getTaskId())
                .taskType(s.getTaskType())
                .knowledgeKey(packMeta != null ? packMeta.knowledgeKey() : null)
                .packId(packMeta != null ? packMeta.packId() : null)
                .knowledgeType(packMeta != null ? packMeta.knowledgeType() : null)
                .scaffoldType(packMeta != null ? packMeta.scaffoldType() : null)
                .starterPrompts(packMeta != null ? packMeta.starterPrompts() : null)
                .checkpointMode(packMeta != null ? packMeta.checkpointMode() : null)
                .visualHintType(packMeta != null ? packMeta.visualHintType() : null)
                .taskLevelLearningIntent(s.getTaskLevelLearningIntent())
                .learningObjective(s.getLearningObjective())
                .whyThisTask(s.getWhyThisTask())
                .cognitiveUnits(mapCognitiveUnits(s.getCognitiveUnits()))
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
                .reflectionSummary(mapReflectionSummary(s))
                .phaseProgress(buildPhaseProgress(sessionId, rt))
                .currentTaskCard(buildCurrentTaskCard(rt, s))
                .scaffoldGuide(buildScaffoldGuide(s, packMeta))
                .expressionLayout(buildExpressionLayout(rt.getState()))
                .feedbackSchema(TaskScaffoldResponse.FeedbackSchema.builder()
                        .correctTitle("你已经说对了什么")
                        .missingTitle("你漏了什么")
                        .confusedTitle("你混淆了什么")
                        .nextFixTitle("下一步该怎么修")
                        .build())
                .actionBar(TaskScaffoldResponse.ActionBar.builder()
                        .hintActionLabel("查看提示")
                        .submitActionLabel("提交本轮表达")
                        .nextActionLabel(rt.getState() == TaskExecutionState.PASS ? "完成本任务" : "进入下一步")
                        .build())
                .tutorAssist(buildTutorAssist(s, rt))
                .build();
    }

    private TaskScaffoldResponse.WorkbenchPhaseProgress buildPhaseProgress(String sessionId, TaskExecutionRuntime rt) {
        InMemoryStore.LearningSessionState session = store.getSessions().get(sessionId);
        int totalTasks = 1;
        int currentIndex = 1;
        if (session != null && session.getTaskSequence() != null && !session.getTaskSequence().isEmpty()) {
            totalTasks = session.getTaskSequence().size();
            currentIndex = Math.min(session.getCurrentTaskIndex() + 1, totalTasks);
        }
        String phase = toWorkbenchPhase(rt.getState());
        double phaseRatio = switch (rt.getState()) {
            case ORIENT -> 0.18d;
            case EXPLORE -> 0.38d;
            case SELF_EXPLAIN, REMEDIAL -> 0.64d;
            case CHECK -> 0.82d;
            case PASS -> 1.0d;
            default -> 0.18d;
        };
        double overall = Math.min(1.0d, ((currentIndex - 1) + phaseRatio) / totalTasks);
        String stepLabel = switch (rt.getState()) {
            case ORIENT -> "定位动作";
            case EXPLORE -> "机制展开";
            case SELF_EXPLAIN -> "自我解释";
            case CHECK -> "微检查";
            case REMEDIAL -> "针对修正";
            case PASS -> "总结收束";
            default -> "当前动作";
        };
        return TaskScaffoldResponse.WorkbenchPhaseProgress.builder()
                .phases(List.of("STRUCTURE", "UNDERSTANDING", "TRAINING", "REFLECTION"))
                .currentPhase(phase)
                .overallRatio(overall)
                .taskIndexLabel("任务 " + currentIndex + " / " + totalTasks)
                .stepLabel(stepLabel)
                .build();
    }

    private TaskScaffoldResponse.CurrentTaskCard buildCurrentTaskCard(TaskExecutionRuntime rt, TaskScaffold scaffold) {
        String phaseCode = toWorkbenchPhase(rt.getState());
        String phaseDisplay = toPhaseDisplay(phaseCode);
        String action = switch (rt.getState()) {
            case ORIENT -> "先定位它是什么";
            case EXPLORE -> "先把关键机制讲清";
            case SELF_EXPLAIN -> "用自己的话完整表达";
            case CHECK -> "独立作答并写清依据";
            case REMEDIAL -> "对准缺口补一句";
            case PASS -> "收束成可带走的规则";
            default -> "完成当前动作";
        };
        List<String> outputs = switch (phaseCode) {
            case "STRUCTURE" -> List.of("写清它属于什么", "写清它在解决什么问题", "补一句和相邻概念的关系");
            case "UNDERSTANDING" -> List.of("写出问题", "写出机制或过程", "写出会导致什么结果");
            case "TRAINING" -> List.of("先用自己的话解释", "再用一个场景验证", "暴露自己还不稳的点");
            default -> List.of("指出错因", "写下规则", "写出下次如何检查");
        };
        List<String> completion = scaffold.getCompletionSignals() != null && !scaffold.getCompletionSignals().isEmpty()
                ? scaffold.getCompletionSignals().stream().limit(3).toList()
                : outputs;
        return TaskScaffoldResponse.CurrentTaskCard.builder()
                .phaseCode(phaseCode)
                .phaseDisplay(phaseDisplay)
                .currentAction(action)
                .taskTitle(scaffold.getLearningObjective())
                .objective(firstOf(outputs, "先完成这一轮结构化表达。"))
                .whyNow(defaultIfBlank(scaffold.getWhyThisTask(), "这是当前任务最关键的一步，先把这一点说稳。"))
                .outputRequirements(outputs)
                .completionCriteria(completion)
                .build();
    }

    private TaskScaffoldResponse.ScaffoldGuide buildScaffoldGuide(TaskScaffold scaffold,
                                                                 KnowledgePackMetadata.PackMeta packMeta) {
        List<String> prompts = safeList(scaffold.getRecommendedAskTemplates());
        List<TaskScaffoldResponse.GuideSection> sections = List.of(
                TaskScaffoldResponse.GuideSection.builder()
                        .id("think-first")
                        .title("先想什么")
                        .description(firstOf(prompts, "先确认它在解决什么问题。"))
                        .lightHint("先别求全，只抓住定位。")
                        .standardHint(firstOf(scaffold.getFallbackHints(), "先写一句它是什么、为什么需要它。"))
                        .strongHint(firstOf(scaffold.getAntiPatterns(), "不要直接跳到长篇答案。"))
                        .build(),
                TaskScaffoldResponse.GuideSection.builder()
                        .id("fill-gap")
                        .title("再补什么")
                        .description(firstOfSecond(prompts, "再补一条关键因果或边界。"))
                        .lightHint("补最关键的一处，不要散。")
                        .standardHint(firstOfSecond(scaffold.getRecommendedFollowupTemplates(), "想一想：少了它会怎样？"))
                        .strongHint("用一个最小例子验证刚才的说法。")
                        .build(),
                TaskScaffoldResponse.GuideSection.builder()
                        .id("land-output")
                        .title("最后落到哪里")
                        .description("把这一轮收束成可提交的结构化表达。")
                        .lightHint("写完后回看：是否真的回答了本步目标。")
                        .standardHint(firstOf(scaffold.getCompletionSignals(), "对照完成标准，补齐缺口。"))
                        .strongHint("如果还不稳，就用导师抽屉追问当前动作，而不是发散聊天。")
                        .build()
        );
        return TaskScaffoldResponse.ScaffoldGuide.builder()
                .sections(sections)
                .observationBullets(buildObservationBullets(packMeta))
                .build();
    }

    private TaskScaffoldResponse.ExpressionLayout buildExpressionLayout(TaskExecutionState state) {
        return switch (toWorkbenchPhase(state)) {
            case "STRUCTURE" -> TaskScaffoldResponse.ExpressionLayout.builder()
                    .helperText("先归位，再区分，再给一句最小定义。")
                    .fields(List.of(
                            field("position", "它属于什么", "先用一句话归类", false),
                            field("problem", "它在解决什么问题", "写它在帮什么忙", true),
                            field("compare", "它和谁最容易混", "补一句相邻概念对比", true)
                    ))
                    .lowFrictionPrompt("不确定也没关系，先写你目前最稳的一版。")
                    .build();
            case "UNDERSTANDING" -> TaskScaffoldResponse.ExpressionLayout.builder()
                    .helperText("围绕问题、机制、结果来展开。")
                    .fields(List.of(
                            field("question", "问题", "它在处理什么问题", false),
                            field("mechanism", "机制", "核心过程或因果链", true),
                            field("result", "结果", "少了它会怎样", true)
                    ))
                    .lowFrictionPrompt("先写主线因果，不用一上来追求完整。")
                    .build();
            case "TRAINING" -> TaskScaffoldResponse.ExpressionLayout.builder()
                    .helperText("先自己解释，再用场景验证。")
                    .fields(List.of(
                            field("explain", "用自己的话解释", "别照抄定义", true),
                            field("example", "举一个场景", "用最小例子验证", true),
                            field("unstable", "我还不稳的点", "暴露这一步最容易错的地方", true)
                    ))
                    .lowFrictionPrompt("能暴露缺口比写得漂亮更重要。")
                    .build();
            default -> TaskScaffoldResponse.ExpressionLayout.builder()
                    .helperText("从错因里提炼规则。")
                    .fields(List.of(
                            field("mistake", "错因", "这一步最容易错在哪", true),
                            field("rule", "规则", "下次如何判断", true),
                            field("check", "下次检查", "再遇到时先看什么", true)
                    ))
                    .lowFrictionPrompt("收束成以后还能直接复用的判断句。")
                    .build();
        };
    }

    private TaskScaffoldResponse.ExpressionField field(String id, String label, String placeholder, boolean multiline) {
        return TaskScaffoldResponse.ExpressionField.builder()
                .id(id)
                .label(label)
                .placeholder(placeholder)
                .multiline(multiline)
                .build();
    }

    private TaskScaffoldResponse.TutorAssist buildTutorAssist(TaskScaffold scaffold, TaskExecutionRuntime rt) {
        String phase = toPhaseDisplay(toWorkbenchPhase(rt.getState()));
        String target = defaultIfBlank(scaffold.getLearningObjective(), "当前这一步");
        return TaskScaffoldResponse.TutorAssist.builder()
                .floatingLabel("不懂这一步？")
                .panelTitle("导师辅助")
                .quickQuestions(List.of(
                        "帮我解释这一步要求",
                        "给我一个更容易理解的提示",
                        target + " 这个术语到底是什么意思"
                ))
                .build();
    }

    private ExecutionFeedbackBoard buildExploreFeedbackBoard(String userInput,
                                                             String assistantReply,
                                                             List<String> nextSuggestedPrompts) {
        return ExecutionFeedbackBoard.builder()
                .correct("你已经开始围绕当前任务输出，而不是停在空白状态。")
                .missing(firstOf(nextSuggestedPrompts, "再补一句关键因果或判断依据。"))
                .confused(shortAssistantLine(assistantReply))
                .nextFix(firstOf(nextSuggestedPrompts, "顺着当前提示继续补一小段。"))
                .actions(defaultFixActions())
                .build();
    }

    private static List<ExecutionFeedbackActionItem> defaultFixActions() {
        return List.of(
                ExecutionFeedbackActionItem.builder().id("apply_suggestion").label("补这一处").build(),
                ExecutionFeedbackActionItem.builder().id("restate").label("重新表达").build(),
                ExecutionFeedbackActionItem.builder().id("show_example").label("看例子").build()
        );
    }

    private static String toWorkbenchPhase(TaskExecutionState state) {
        return switch (state) {
            case ORIENT -> "STRUCTURE";
            case EXPLORE -> "UNDERSTANDING";
            case SELF_EXPLAIN, REMEDIAL -> "TRAINING";
            case CHECK, PASS -> "REFLECTION";
            default -> "STRUCTURE";
        };
    }

    private static String toPhaseDisplay(String phaseCode) {
        return switch (phaseCode) {
            case "STRUCTURE" -> "结构建立";
            case "UNDERSTANDING" -> "机制理解";
            case "TRAINING" -> "表达训练";
            case "REFLECTION" -> "反思收束";
            default -> "结构建立";
        };
    }

    private static List<String> buildObservationBullets(KnowledgePackMetadata.PackMeta packMeta) {
        String key = packMeta != null ? packMeta.packId() : null;
        if ("ds_dfs_bfs".equals(key)) {
            return List.of("先分清搜索顺序", "再区分栈、队列与递归直觉", "最后再谈典型场景");
        }
        if ("net_tcp_handshake".equals(key)) {
            return List.of("先看每一步在确认什么", "再看为什么不能少", "别和可靠传输混在一起");
        }
        if ("os_process_thread".equals(key)) {
            return List.of("先看资源边界", "再看调度单位", "最后再比较切换成本");
        }
        if ("arch_cache_locality".equals(key)) {
            return List.of("先看快慢矛盾", "再看局部性", "最后看为什么会命中或失效");
        }
        return List.of("先定位当前概念", "再补一条关键机制", "最后写成能提交的一版");
    }

    private static List<String> safeList(List<String> items) {
        return items == null ? List.of() : items.stream().filter(s -> s != null && !s.isBlank()).toList();
    }

    private static String shortAssistantLine(String text) {
        if (text == null || text.isBlank()) {
            return "先对准当前动作，不要散到别的知识点。";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 72 ? normalized : normalized.substring(0, 72) + "…";
    }

    private static String firstOf(List<String> items, String fallback) {
        if (items == null) return fallback;
        return items.stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse(fallback);
    }

    private static String firstOfSecond(List<String> items, String fallback) {
        if (items == null || items.size() < 2) return fallback;
        String value = items.get(1);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static ReflectionSummary mapReflectionSummary(TaskScaffold s) {
        if (s == null) {
            return null;
        }
        LearningScaffoldEngineState eng = s.getLearningScaffoldEngineState();
        if (eng == null || eng.getReflectionRecord() == null) {
            return null;
        }
        ReflectionRecord rec = eng.getReflectionRecord();
        ReflectionInsight ins = eng.getReflectionInsight();
        return ReflectionSummary.builder()
                .record(rec)
                .insight(ins)
                .systemObservation(buildReflectionObservation(ins, rec))
                .build();
    }

    private static String buildReflectionObservation(ReflectionInsight ins, ReflectionRecord rec) {
        if (ins == null) {
            return rec != null && rec.getFutureStrategy() != null
                    ? "下一步建议：" + rec.getFutureStrategy()
                    : "反思沉淀已就绪。";
        }
        StringBuilder sb = new StringBuilder();
        if (ins.getMostDifficultActionId() != null && !ins.getMostDifficultActionId().isBlank()) {
            sb.append("训练中耗时较多的是：").append(ins.getMostDifficultActionId()).append("。");
        }
        if (ins.getRepeatedErrorTypes() != null && !ins.getRepeatedErrorTypes().isEmpty()) {
            sb.append(" 反复出现的问题类型：").append(String.join("、", ins.getRepeatedErrorTypes())).append("。");
        }
        if (sb.isEmpty()) {
            return "本轮训练合计 " + ins.getTotalAttempts() + " 次提交，已形成可迁移规则与能力命名。";
        }
        return sb.toString();
    }

    private KnowledgePackMetadata.PackMeta resolvePackMeta(String sessionId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null || state.getPlanId() == null) return null;
        LearningPlanPreview plan = store.getPlanPreviews().get(state.getPlanId());
        if (plan == null || plan.getGoalId() == null) return null;
        var goal = store.getGoals().get(plan.getGoalId());
        return KnowledgePackMetadata.fromGoal(goal);
    }

    private void ensureScaffoldCognitiveUnits(String sessionId, String taskId, TaskExecutionRuntime rt) {
        if (rt == null || rt.getScaffold() == null) {
            return;
        }
        TaskBlueprint bp = resolveBlueprint(sessionId, taskId);
        if (bp == null) {
            return;
        }
        TaskScaffoldFactory.ensureCognitiveUnits(rt.getScaffold(), bp, resolveStrategyProfile(sessionId));
    }

    private List<TaskScaffoldResponse.CognitiveUnitItem> mapCognitiveUnits(List<CognitiveUnit> units) {
        if (units == null || units.isEmpty()) {
            return null;
        }
        return units.stream().map(this::mapCognitiveUnit).toList();
    }

    private TaskScaffoldResponse.CognitiveUnitItem mapCognitiveUnit(CognitiveUnit u) {
        return TaskScaffoldResponse.CognitiveUnitItem.builder()
                .unitId(u.getUnitId())
                .order(u.getOrder())
                .label(u.getLabel())
                .learningObjective(u.getLearningObjective())
                .targetOutcome(u.getTargetOutcome())
                .failureSignal(u.getFailureSignal())
                .actionBullets(u.getActionBullets())
                .prompts(mapScaffoldPromptItems(u.getPrompts()))
                .build();
    }

    private List<TaskScaffoldResponse.ScaffoldPromptItem> mapScaffoldPromptItems(List<ScaffoldPrompt> prompts) {
        if (prompts == null) {
            return List.of();
        }
        return prompts.stream()
                .map(p -> TaskScaffoldResponse.ScaffoldPromptItem.builder()
                        .promptId(p.getPromptId())
                        .prompt(p.getPrompt())
                        .intent(p.getIntent() != null ? p.getIntent().name() : null)
                        .required(p.isRequired())
                        .build())
                .toList();
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
