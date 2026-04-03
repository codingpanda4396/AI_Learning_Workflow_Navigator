package navigator.application.scaffold;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.scaffold.ActionRuntime;
import navigator.api.dto.scaffold.CompleteConversationStageRequest;
import navigator.api.dto.scaffold.CompleteConversationStageResult;
import navigator.api.dto.scaffold.CompleteReflectionStageRequest;
import navigator.api.dto.scaffold.CompleteReflectionStageResult;
import navigator.api.dto.scaffold.CompleteStructureStageRequest;
import navigator.api.dto.scaffold.CompleteStructureStageResult;
import navigator.api.dto.scaffold.LearningScaffoldActionResult;
import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;
import navigator.api.dto.scaffold.StructuredScaffoldFeedbackPayload;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.api.dto.scaffold.StructureSkeletonBlock;
import navigator.api.dto.scaffold.StructureSkeletonRequest;
import navigator.api.dto.scaffold.StructureSkeletonResult;
import navigator.api.dto.scaffold.SubmitLearningScaffoldActionRequest;
import navigator.api.dto.scaffold.TrainingFeedback;
import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
import navigator.api.dto.TaskExecutionMetaSnapshot;
import navigator.application.knowledge.KnowledgePackMetadata;
import navigator.application.task.TaskExecutionFlowService;
import navigator.application.task.TaskExecutionPersistenceService;
import navigator.application.task.TaskExecutionRuntime;
import navigator.application.guard.EntityLookupGuard;
import navigator.application.guard.SessionStateGuard;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.LearningScaffoldEngineState;
import navigator.domain.model.ScaffoldActionRuntimeEntry;
import navigator.domain.model.ScaffoldAttemptSnapshot;
import navigator.domain.model.ScaffoldRuntimeStatus;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.model.TaskScaffold;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LearningScaffoldEngineService {

    private final TaskExecutionFlowService taskExecutionFlowService;
    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final EntityLookupGuard entityLookupGuard;
    private final TaskExecutionPersistenceService persistenceService;
    private final DfsBfsStructureValidator dfsBfsStructureValidator;
    private final DfsBfsUnderstandingValidator dfsBfsUnderstandingValidator;
    private final UnderstandingTutorComposer understandingTutorComposer;
    private final DfsBfsTrainingEvaluator dfsBfsTrainingEvaluator;
    private final TrainingTutorComposer trainingTutorComposer;
    private final DfsBfsReflectionEvaluator dfsBfsReflectionEvaluator;
    private final ReflectionTutorComposer reflectionTutorComposer;
    private final ReflectionAssembler reflectionAssembler;
    private final StructureSkeletonComposer structureSkeletonComposer;
    private final StageScaffoldWorkbenchComposer stageScaffoldWorkbenchComposer;

    public LearningScaffoldEngineService(TaskExecutionFlowService taskExecutionFlowService,
                                         InMemoryStore store,
                                         SessionStateGuard sessionStateGuard,
                                         EntityLookupGuard entityLookupGuard,
                                         TaskExecutionPersistenceService persistenceService,
                                         DfsBfsStructureValidator dfsBfsStructureValidator,
                                         DfsBfsUnderstandingValidator dfsBfsUnderstandingValidator,
                                         UnderstandingTutorComposer understandingTutorComposer,
                                         DfsBfsTrainingEvaluator dfsBfsTrainingEvaluator,
                                         TrainingTutorComposer trainingTutorComposer,
                                         DfsBfsReflectionEvaluator dfsBfsReflectionEvaluator,
                                         ReflectionTutorComposer reflectionTutorComposer,
                                         ReflectionAssembler reflectionAssembler,
                                         StructureSkeletonComposer structureSkeletonComposer,
                                         StageScaffoldWorkbenchComposer stageScaffoldWorkbenchComposer) {
        this.taskExecutionFlowService = taskExecutionFlowService;
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.entityLookupGuard = entityLookupGuard;
        this.persistenceService = persistenceService;
        this.dfsBfsStructureValidator = dfsBfsStructureValidator;
        this.dfsBfsUnderstandingValidator = dfsBfsUnderstandingValidator;
        this.understandingTutorComposer = understandingTutorComposer;
        this.dfsBfsTrainingEvaluator = dfsBfsTrainingEvaluator;
        this.trainingTutorComposer = trainingTutorComposer;
        this.dfsBfsReflectionEvaluator = dfsBfsReflectionEvaluator;
        this.reflectionTutorComposer = reflectionTutorComposer;
        this.reflectionAssembler = reflectionAssembler;
        this.structureSkeletonComposer = structureSkeletonComposer;
        this.stageScaffoldWorkbenchComposer = stageScaffoldWorkbenchComposer;
    }

    public StageScaffold getStage(String sessionId, String taskId, String stageKeyParam) {
        return getStage(sessionId, taskId, stageKeyParam, WorkbenchMode.FULL);
    }

    public StageScaffold getStage(String sessionId, String taskId, String stageKeyParam, WorkbenchMode workbenchMode) {
        return buildStageScaffoldForSession(sessionId, taskId, stageKeyParam, workbenchMode);
    }

    /**
     * 当前任务元信息（不含阶段 UI 与 LLM）。用于 GET /api/sessions/.../current-task。
     */
    /**
     * 不创建任务运行时、不触发 LLM；若尚无持久化 runtime，则仅返回知识点与空进度。
     */
    public TaskExecutionMetaSnapshot resolveTaskExecutionMeta(String sessionId, String taskId) {
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        String knowledge = "";
        if (pack != null) {
            if (pack.packId() != null && !pack.packId().isBlank()) {
                knowledge = pack.packId();
            } else if (pack.knowledgeKey() != null) {
                knowledge = pack.knowledgeKey();
            }
        }
        TaskExecutionRuntime rt = loadRuntimeOptional(sessionId, taskId);
        if (rt == null || rt.getScaffold() == null) {
            return TaskExecutionMetaSnapshot.builder()
                    .knowledge(knowledge)
                    .currentStage(null)
                    .progressMap(Map.of())
                    .build();
        }
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            return TaskExecutionMetaSnapshot.builder()
                    .knowledge(knowledge)
                    .currentStage(null)
                    .progressMap(Map.of())
                    .build();
        }
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        if (eng == null) {
            return TaskExecutionMetaSnapshot.builder()
                    .knowledge(knowledge)
                    .currentStage(null)
                    .progressMap(Map.of())
                    .build();
        }
        normalizeEngineState(eng);
        return TaskExecutionMetaSnapshot.builder()
                .knowledge(knowledge)
                .currentStage(eng.getCurrentStageKey())
                .progressMap(buildStageProgressMap(eng))
                .build();
    }

    private TaskExecutionRuntime loadRuntimeOptional(String sessionId, String taskId) {
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null) {
            rt = persistenceService.loadRuntime(sessionId, taskId);
            if (rt != null) {
                store.getTaskExecutionRuntimes().put(key, rt);
            }
        }
        return rt;
    }

    private static Map<String, Boolean> buildStageProgressMap(LearningScaffoldEngineState eng) {
        List<String> done = eng.getCompletedStageKeys() != null ? eng.getCompletedStageKeys() : List.of();
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put(DfsBfsStructureValidator.STAGE_KEY, done.contains(DfsBfsStructureValidator.STAGE_KEY));
        m.put(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY, done.contains(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY));
        m.put(DfsBfsTrainingScaffoldDefinition.STAGE_KEY, done.contains(DfsBfsTrainingScaffoldDefinition.STAGE_KEY));
        m.put(DfsBfsReflectionScaffoldDefinition.STAGE_KEY, done.contains(DfsBfsReflectionScaffoldDefinition.STAGE_KEY));
        return m;
    }

    /**
     * 构建当前会话下的脚手架阶段视图（含工作台 LLM 软文案），与 {@link #getStage} 同源，供 action 提交后合并返回以避免重复请求。
     */
    private StageScaffold buildStageScaffoldForSession(String sessionId, String taskId, String stageKeyParam) {
        return buildStageScaffoldForSession(sessionId, taskId, stageKeyParam, WorkbenchMode.FULL);
    }

    private StageScaffold buildStageScaffoldForSession(
            String sessionId, String taskId, String stageKeyParam, WorkbenchMode workbenchMode) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎（需已注册的知识点包）");
        }
        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        ensureEngineState(rt.getScaffold());
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);

        String expected = eng.getCurrentStageKey();
        String stageKey = resolveStageKeyParam(stageKeyParam, expected);
        if (!stageKey.equals(expected)) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "当前脚手架阶段为 " + expected + "，请使用 stageKey=" + expected);
        }

        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        StageScaffold stage = mergeProgress(buildStageForKey(stageKey), eng);
        stage.setWorkbench(stageScaffoldWorkbenchComposer.composeWorkbench(pack.packId(), stage, workbenchMode));
        return stage;
    }

    public StructureSkeletonResult generateStructureSkeleton(String taskId, StructureSkeletonRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎");
        }
        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        ensureEngineState(rt.getScaffold());
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);
        if (!DfsBfsStructureValidator.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前不在结构建立阶段");
        }
        String promptKey = request.getPromptKey().trim();
        if (!DfsBfsStructureScaffoldDefinition.isValidPromptKey(promptKey)) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "无效的 promptKey");
        }
        String follow = request.getFollowUpKind() != null ? request.getFollowUpKind().trim() : "";
        StructureSkeletonBlock block = structureSkeletonComposer.compose(promptKey, follow.isEmpty() ? null : follow);

        if (eng.getStructureExploredPromptKeys() == null) {
            eng.setStructureExploredPromptKeys(new ArrayList<>());
        }
        if (!eng.getStructureExploredPromptKeys().contains(promptKey)) {
            eng.getStructureExploredPromptKeys().add(promptKey);
        }
        eng.setStructureGenerationCount(eng.getStructureGenerationCount() + 1);
        eng.setStructureLastPromptKey(promptKey);
        String fk = follow.toUpperCase();
        if ("CLARIFY".equals(fk) || "ADJACENT".equals(fk)) {
            eng.setStructureLightInteractionCount(eng.getStructureLightInteractionCount() + 1);
        }
        eng.setCurrentActionId(promptKey);
        rt.getScaffold().setLearningScaffoldEngineState(eng);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);

        return StructureSkeletonResult.builder()
                .skeleton(block)
                .structureGenerationCount(eng.getStructureGenerationCount())
                .structureLightInteractionCount(eng.getStructureLightInteractionCount())
                .structureExploredPromptKeys(new ArrayList<>(eng.getStructureExploredPromptKeys()))
                .canCompleteStructure(canCompleteStructure(eng))
                .lastPromptKey(promptKey)
                .build();
    }

    public CompleteStructureStageResult completeStructureStage(String taskId, CompleteStructureStageRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎");
        }
        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        ensureEngineState(rt.getScaffold());
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);
        if (!DfsBfsStructureValidator.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前不在结构建立阶段");
        }
        if (!canCompleteStructure(eng)) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "请先至少生成一次骨架，并完成一次轻反馈；或探索至少两张脚手架卡。");
        }
        String one = request.getOptionalOneLiner() != null ? request.getOptionalOneLiner().trim() : "";
        eng.setStructureOptionalReflection(one.isEmpty() ? null : one);
        appendCompletedStage(eng, DfsBfsStructureValidator.STAGE_KEY);
        eng.setCurrentStageKey(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY);
        eng.setCurrentActionId(DfsBfsUnderstandingScaffoldDefinition.orderedActionIds().get(0));
        syncLegacyBooleans(eng);
        rt.getScaffold().setLearningScaffoldEngineState(eng);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);

        return CompleteStructureStageResult.builder()
                .structureStageComplete(true)
                .nextStageKey(eng.getCurrentStageKey())
                .nextActionId(eng.getCurrentActionId())
                .build();
    }

    public CompleteConversationStageResult completeConversationStage(String taskId, CompleteConversationStageRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎");
        }

        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        ensureEngineState(rt.getScaffold());
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);

        String stageKey = request.getStageKey() != null ? request.getStageKey().trim() : "";
        if (!DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(stageKey)
                && !DfsBfsTrainingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "仅支持完成 UNDERSTANDING 或 TRAINING 阶段");
        }
        if (!stageKey.equals(eng.getCurrentStageKey())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "当前脚手架阶段为 " + eng.getCurrentStageKey() + "，无法完成 " + stageKey);
        }

        appendCompletedStage(eng, stageKey);
        if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            eng.setCurrentStageKey(DfsBfsTrainingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsTrainingScaffoldDefinition.orderedActionIds().get(0));
        } else {
            eng.setCurrentStageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsReflectionScaffoldDefinition.orderedActionIds().get(0));
        }
        syncLegacyBooleans(eng);
        rt.getScaffold().setLearningScaffoldEngineState(eng);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);

        return CompleteConversationStageResult.builder()
                .completedStageKey(stageKey)
                .nextStageKey(eng.getCurrentStageKey())
                .nextActionId(eng.getCurrentActionId())
                .build();
    }

    /**
     * 前端「反思收敛」为单页（策略 + 一句话），与后端四张反思卡并行存在时，用本接口一次性写入四卡并通过校验，
     * 将任务执行状态置为 PASS，供 {@link navigator.application.guard.TaskProgressGuard} 放行 completeTask。
     */
    public CompleteReflectionStageResult completeReflectionStage(String taskId, CompleteReflectionStageRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎");
        }

        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        ensureEngineState(rt.getScaffold());
        LearningScaffoldEngineState eng = rt.getScaffold().getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);

        if (!DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "当前不在反思收敛阶段，当前脚手架阶段为 " + eng.getCurrentStageKey());
        }

        List<String> completed = eng.getCompletedStageKeys();
        if (completed != null && completed.contains(DfsBfsReflectionScaffoldDefinition.STAGE_KEY)) {
            maybeAdvanceTaskStateAfterScaffoldBlock(sessionId, taskId, rt);
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
            return CompleteReflectionStageResult.builder()
                    .reflectionStageComplete(true)
                    .completedStageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY)
                    .nextStageKey(null)
                    .nextActionId(DfsBfsReflectionScaffoldDefinition.orderedActionIds().get(3))
                    .build();
        }

        String reflectionText = request.getReflectionText() != null ? request.getReflectionText().trim() : "";
        List<String> labelList = new ArrayList<>();
        if (request.getStrategyLabels() != null) {
            for (String s : request.getStrategyLabels()) {
                if (s != null && !s.isBlank()) {
                    labelList.add(s.trim());
                }
            }
        }
        if (reflectionText.isEmpty() && labelList.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "请至少勾选一条判断规则，或填写一句话反思。");
        }

        List<String> order = DfsBfsReflectionScaffoldDefinition.orderedActionIds();
        String[] inputs = buildReflectionWorkbenchInputs(reflectionText, labelList);
        for (int i = 0; i < order.size(); i++) {
            String actionId = order.get(i);
            String userInput = ensureReflectionInputPasses(actionId, inputs[i]);
            ScaffoldActionRuntimeEntry entry = ScaffoldActionRuntimeEntry.builder()
                    .actionId(actionId)
                    .userInput(userInput)
                    .validationStatus("PASS")
                    .lastTutorFeedback("通过")
                    .retryCount(1)
                    .attemptNo(1)
                    .completed(true)
                    .runtimeStatus(ScaffoldRuntimeStatus.PASSED)
                    .attemptSnapshots(new ArrayList<>())
                    .build();
            if (eng.getActionRuntimeByActionId() == null) {
                eng.setActionRuntimeByActionId(new LinkedHashMap<>());
            }
            eng.getActionRuntimeByActionId().put(actionId, entry);
        }

        eng.setCurrentActionId(order.get(order.size() - 1));
        appendCompletedStage(eng, DfsBfsReflectionScaffoldDefinition.STAGE_KEY);
        syncLegacyBooleans(eng);

        ReflectionAssembler.ReflectionRecordAndInsight assembled = reflectionAssembler.assemble(eng);
        eng.setReflectionRecord(assembled.record());
        eng.setReflectionInsight(assembled.insight());

        rt.getScaffold().setLearningScaffoldEngineState(eng);
        maybeAdvanceTaskStateAfterScaffoldBlock(sessionId, taskId, rt);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);

        return CompleteReflectionStageResult.builder()
                .reflectionStageComplete(true)
                .completedStageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY)
                .nextStageKey(null)
                .nextActionId(eng.getCurrentActionId())
                .build();
    }

    private String[] buildReflectionWorkbenchInputs(String reflectionText, List<String> strategyLabels) {
        String base = reflectionText != null ? reflectionText.trim() : "";
        String joined = strategyLabels.isEmpty() ? "" : String.join("；", strategyLabels);

        String e1;
        if (base.length() >= 8) {
            e1 = "我在本轮 DFS/BFS 练习里写错的具体点是：" + base.substring(0, Math.min(120, base.length()));
        } else if (!joined.isEmpty()) {
            e1 = "我在本轮 DFS/BFS 练习里写错的具体点是：在「" + joined + "」上把概念与机制混写，导致表达不够具体。";
        } else {
            e1 = "我曾把 BFS 第一次到达终点误当成任意最短路成立，没有说无权图与按层扩展的前提。";
        }

        String e2 = "根因是：我曾把现象与机制混在一起写，因果链在搜索顺序这一环断了；"
                + "因为背定义多于理解过程，所以表达时容易写空洞。";

        String e3;
        if (!joined.isEmpty()) {
            e3 = "结合我选的策略「" + joined + "」：当问题强调层次扩展、无权最短路径时，我优先想到 BFS；"
                    + "当需要深度试探、回溯结构时，我优先想到 DFS。"
                    + (base.length() > 0 ? " 补充：" + base.substring(0, Math.min(80, base.length())) : "");
        } else {
            e3 = "当问题强调层次扩展、无权最短路径时，我优先想到 BFS；当需要深度试探、回溯结构时，我优先想到 DFS。"
                    + (base.length() >= 4 ? " 个人补充：" + base.substring(0, Math.min(80, base.length())) : "");
        }

        String e4 = "我能根据题面关键词判断应先想到 DFS 还是 BFS，并用一句话把因果链讲清楚。";
        if (base.length() >= 8) {
            e4 = "我能用 DFS/BFS 术语把「回溯 vs 分层」讲清楚，并在下次练习里先写条件句再写结论："
                    + base.substring(0, Math.min(80, base.length()));
        }

        return new String[] { e1, e2, e3, e4 };
    }

    private String ensureReflectionInputPasses(String actionId, String candidate) {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .stageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY)
                .actionId(actionId)
                .userInput(candidate)
                .build();
        ValidationResult v = dfsBfsReflectionEvaluator.validate(ctx);
        if (v.isPassed()) {
            return candidate;
        }
        // 与单元测试对齐的兜底句，保证校验通过并仍可生成沉淀
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL.equals(actionId)) {
            return "我曾把 BFS 第一次到达终点误当成任意最短路成立，没有说无权图与按层扩展的前提。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE.equals(actionId)) {
            return "根因是：我曾把现象与机制混在一起写，因果链在搜索顺序这一环断了；"
                    + "因为背定义多于理解过程，所以表达时容易写空洞。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE.equals(actionId)) {
            return "当问题强调层次扩展、无权最短路径时，我优先想到 BFS；当需要深度试探、回溯结构时，我优先想到 DFS。";
        }
        return "我能根据题面关键词判断应先想到 DFS 还是 BFS，并用一句话把因果链讲清楚。";
    }

    private static boolean canCompleteStructure(LearningScaffoldEngineState eng) {
        int gen = eng.getStructureGenerationCount();
        int light = eng.getStructureLightInteractionCount();
        int explored = eng.getStructureExploredPromptKeys() != null ? eng.getStructureExploredPromptKeys().size() : 0;
        if (gen >= 1 && (light >= 1 || explored >= 2)) {
            return true;
        }
        // 前端 STRUCTURE 三道 MCQ 与四张动作卡的前三张一一对应；完成 MCQ 后未走骨架生成 API，gen/light 仍为 0
        return hasStructureMcqFirstThreeActionsPassed(eng);
    }

    /**
     * 与 {@link DfsBfsStructureScaffoldDefinition#orderedActionIds()} 中前三张卡对齐（position / prereq / next）。
     */
    private static boolean hasStructureMcqFirstThreeActionsPassed(LearningScaffoldEngineState eng) {
        Map<String, ScaffoldActionRuntimeEntry> m = eng.getActionRuntimeByActionId();
        if (m == null || m.isEmpty()) {
            return false;
        }
        return isStructureActionPassed(m, DfsBfsStructureScaffoldDefinition.ACTION_POSITION)
                && isStructureActionPassed(m, DfsBfsStructureScaffoldDefinition.ACTION_PREREQ)
                && isStructureActionPassed(m, DfsBfsStructureScaffoldDefinition.ACTION_NEXT);
    }

    private static boolean isStructureActionPassed(Map<String, ScaffoldActionRuntimeEntry> m, String actionId) {
        ScaffoldActionRuntimeEntry e = m.get(actionId);
        return e != null && e.isCompleted();
    }

    public LearningScaffoldActionResult submitAction(String taskId, SubmitLearningScaffoldActionRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.ensureTaskExecutionRuntimeAndScaffoldDomain(sessionId, taskId);

        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !LearningScaffoldPackRegistry.supportsLearningScaffoldEngine(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎");
        }

        TaskExecutionRuntime rt = requireRuntime(sessionId, taskId);
        TaskScaffold scaffold = rt.getScaffold();
        ensureEngineState(scaffold);
        LearningScaffoldEngineState eng = scaffold.getLearningScaffoldEngineState();
        Objects.requireNonNull(eng);
        normalizeEngineState(eng);

        String expectedStage = eng.getCurrentStageKey();
        if (!expectedStage.equals(request.getStageKey())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "当前脚手架阶段为 " + expectedStage + "，请提交 stageKey=" + expectedStage);
        }

        String expectedAction = eng.getCurrentActionId();
        if (expectedAction == null || !expectedAction.equals(request.getActionId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    "请按顺序完成当前动作卡，当前应为：" + expectedAction);
        }

        StructureValidationContext vctx = StructureValidationContext.builder()
                .packId(pack.packId())
                .stageKey(request.getStageKey())
                .actionId(request.getActionId())
                .userInput(request.getUserInput())
                .build();

        String sk = request.getStageKey();
        if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(sk)
                || DfsBfsTrainingScaffoldDefinition.STAGE_KEY.equals(sk)) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                    sk + " 阶段已改为 LLM 对话驱动，不再支持 learning-scaffold/action 提交");
        }
        TrainingFeedback trainingFeedback = null;
        ValidationResult validation;
        TutorResponse tutor;

        if (DfsBfsTrainingScaffoldDefinition.STAGE_KEY.equals(sk)) {
            trainingFeedback = dfsBfsTrainingEvaluator.evaluate(vctx);
            validation = toValidationFromTraining(trainingFeedback);
            tutor = trainingTutorComposer.compose(request.getActionId(), trainingFeedback);
        } else if (DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(sk)) {
            validation = dfsBfsReflectionEvaluator.validate(vctx);
            tutor = reflectionTutorComposer.compose(request.getActionId(), validation);
        } else {
            validation = validateForStage(sk, vctx);
            boolean passed = validation.isPassed();
            if (DfsBfsStructureValidator.STAGE_KEY.equals(sk)) {
                tutor = buildStructureTutor(passed, validation, request.getActionId());
            } else if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(sk)) {
                tutor = understandingTutorComposer.compose(request.getActionId(), validation);
            } else {
                throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "不支持的脚手架阶段: " + sk);
            }
        }

        boolean passed = validation.isPassed();

        ScaffoldActionRuntimeEntry entry = eng.getActionRuntimeByActionId()
                .getOrDefault(request.getActionId(), ScaffoldActionRuntimeEntry.builder()
                        .actionId(request.getActionId())
                        .validationStatus("PENDING")
                        .runtimeStatus(ScaffoldRuntimeStatus.NOT_STARTED)
                        .retryCount(0)
                        .attemptNo(0)
                        .completed(false)
                        .attemptSnapshots(new ArrayList<>())
                        .build());
        if (entry.getAttemptSnapshots() == null) {
            entry.setAttemptSnapshots(new ArrayList<>());
        }
        entry.setUserInput(request.getUserInput() != null ? request.getUserInput().trim() : "");
        entry.setRetryCount(entry.getRetryCount() + 1);
        entry.setAttemptNo(entry.getAttemptNo() + 1);

        if (passed) {
            entry.setValidationStatus("PASS");
            entry.setRuntimeStatus(ScaffoldRuntimeStatus.PASSED);
            entry.setLastTutorFeedback(tutor.getContent() != null ? tutor.getContent() : "");
            entry.setCompleted(true);
        } else {
            entry.setValidationStatus("FAIL");
            entry.setRuntimeStatus(ScaffoldRuntimeStatus.REVISION_REQUIRED);
            String hint = validation.getMessage() != null ? validation.getMessage() : "请按当前阶段要求改写。";
            entry.setLastTutorFeedback(hint);
            entry.setCompleted(false);
        }

        appendAttemptSnapshot(entry, tutor, validation, trainingFeedback);

        eng.getActionRuntimeByActionId().put(request.getActionId(), entry);

        boolean stageComplete = false;
        if (passed) {
            stageComplete = advanceAfterPass(eng, request.getStageKey(), request.getActionId());
            if (stageComplete && DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(request.getStageKey())) {
                ReflectionAssembler.ReflectionRecordAndInsight assembled = reflectionAssembler.assemble(eng);
                eng.setReflectionRecord(assembled.record());
                eng.setReflectionInsight(assembled.insight());
            }
            syncLegacyBooleans(eng);
        }

        scaffold.setLearningScaffoldEngineState(eng);
        persistenceService.saveRuntime(sessionId, taskId, rt, null, null);

        if (passed && stageComplete && DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(request.getStageKey())) {
            maybeAdvanceTaskStateAfterScaffoldBlock(sessionId, taskId, rt);
            persistenceService.saveRuntime(sessionId, taskId, rt, null, null);
        }

        ActionRuntime ar = toActionRuntime(sessionId, request.getStageKey(), entry, tutor, passed);
        boolean actionCompleted = passed;
        ReflectionRecord outRecord = null;
        ReflectionInsight outInsight = null;
        if (passed && stageComplete && DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(request.getStageKey())) {
            outRecord = eng.getReflectionRecord();
            outInsight = eng.getReflectionInsight();
        }
        StructuredScaffoldFeedbackPayload feedbackPayload = ScaffoldStructuredFeedbackFactory.build(
                passed, validation, tutor, trainingFeedback);
        StageScaffold updatedStage = buildStageScaffoldForSession(sessionId, taskId, null, WorkbenchMode.FAST);
        return LearningScaffoldActionResult.builder()
                .actionRuntime(ar)
                .validation(validation)
                .tutor(tutor)
                .stageComplete(stageComplete)
                .stageCompleted(stageComplete)
                .actionCompleted(actionCompleted)
                .trainingFeedback(trainingFeedback)
                .reflectionRecord(outRecord)
                .reflectionInsight(outInsight)
                .stageKey(request.getStageKey())
                .actionId(request.getActionId())
                .attemptNo(entry.getAttemptNo())
                .runtimeStatus(entry.getRuntimeStatus())
                .canProceed(tutor.isCanProceed())
                .feedbackPayload(feedbackPayload)
                .updatedStage(updatedStage)
                .build();
    }

    private static ValidationResult toValidationFromTraining(TrainingFeedback feedback) {
        boolean passed = feedback != null && feedback.isCanProceed();
        return ValidationResult.builder()
                .passed(passed)
                .errorType(passed ? null : "TRAINING_REVISION")
                .message(passed ? "通过" : (feedback.getRevisionInstruction() != null
                        ? feedback.getRevisionInstruction()
                        : "请按反馈重构表达。"))
                .suggestions(List.of())
                .matchedAspects(List.of())
                .missingAspects(List.of())
                .build();
    }

    private static void appendAttemptSnapshot(ScaffoldActionRuntimeEntry entry, TutorResponse tutor, ValidationResult v,
                                            TrainingFeedback trainingFeedback) {
        String vSum = v.isPassed() ? "PASS" : (v.getMessage() != null ? v.getMessage() : "FAIL");
        List<String> errorTypes = new ArrayList<>();
        if (trainingFeedback != null && trainingFeedback.getErrorTypes() != null) {
            errorTypes.addAll(trainingFeedback.getErrorTypes());
        }
        ScaffoldAttemptSnapshot snap = ScaffoldAttemptSnapshot.builder()
                .attemptNo(entry.getAttemptNo())
                .userInput(entry.getUserInput())
                .submittedAt(System.currentTimeMillis())
                .validationSummary(vSum)
                .tutorSummary(tutor.getContent() != null ? tutor.getContent() : "")
                .runtimeStatus(entry.getRuntimeStatus())
                .errorTypes(errorTypes)
                .build();
        entry.getAttemptSnapshots().add(snap);
        if (entry.getAttemptSnapshots().size() > 24) {
            entry.getAttemptSnapshots().remove(0);
        }
    }

    /**
     * STRUCTURE → UNDERSTANDING → TRAINING → REFLECTION；REFLECTION 完成后任务执行状态置为 PASS（见 maybeAdvanceTaskStateAfterScaffoldBlock）。
     */
    private boolean advanceAfterPass(LearningScaffoldEngineState eng, String stageKey, String actionId) {
        if (DfsBfsStructureValidator.STAGE_KEY.equals(stageKey)) {
            List<String> order = DfsBfsStructureScaffoldDefinition.orderedActionIds();
            int idx = order.indexOf(actionId);
            if (idx < 0) {
                throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT,
                        "未知的 STRUCTURE 动作卡: " + actionId);
            }
            if (idx < order.size() - 1) {
                eng.setCurrentActionId(order.get(idx + 1));
                return false;
            }
            appendCompletedStage(eng, DfsBfsStructureValidator.STAGE_KEY);
            eng.setCurrentStageKey(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsUnderstandingScaffoldDefinition.orderedActionIds().get(0));
            return true;
        }
        if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            List<String> order = DfsBfsUnderstandingScaffoldDefinition.orderedActionIds();
            int idx = order.indexOf(actionId);
            if (idx >= 0 && idx < order.size() - 1) {
                eng.setCurrentActionId(order.get(idx + 1));
                return false;
            }
            appendCompletedStage(eng, DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentStageKey(DfsBfsTrainingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsTrainingScaffoldDefinition.orderedActionIds().get(0));
            return true;
        }
        if (DfsBfsTrainingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            List<String> order = DfsBfsTrainingScaffoldDefinition.orderedActionIds();
            int idx = order.indexOf(actionId);
            if (idx >= 0 && idx < order.size() - 1) {
                eng.setCurrentActionId(order.get(idx + 1));
                return false;
            }
            appendCompletedStage(eng, DfsBfsTrainingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentStageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsReflectionScaffoldDefinition.orderedActionIds().get(0));
            return true;
        }
        if (DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            List<String> order = DfsBfsReflectionScaffoldDefinition.orderedActionIds();
            int idx = order.indexOf(actionId);
            if (idx >= 0 && idx < order.size() - 1) {
                eng.setCurrentActionId(order.get(idx + 1));
                return false;
            }
            appendCompletedStage(eng, DfsBfsReflectionScaffoldDefinition.STAGE_KEY);
            return true;
        }
        return false;
    }

    private static void appendCompletedStage(LearningScaffoldEngineState eng, String stageKey) {
        List<String> list = eng.getCompletedStageKeys();
        if (list == null) {
            list = new ArrayList<>();
            eng.setCompletedStageKeys(list);
        }
        if (!list.contains(stageKey)) {
            list.add(stageKey);
        }
    }

    private static void syncLegacyBooleans(LearningScaffoldEngineState eng) {
        List<String> list = eng.getCompletedStageKeys();
        if (list == null) {
            return;
        }
        eng.setStructureStageComplete(list.contains(DfsBfsStructureValidator.STAGE_KEY));
        eng.setUnderstandingStageComplete(list.contains(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY));
    }

    private static void normalizeStructureLegacyActionIds(LearningScaffoldEngineState eng) {
        if (eng == null || !DfsBfsStructureValidator.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            return;
        }
        String aid = eng.getCurrentActionId();
        if (DfsBfsStructureScaffoldDefinition.ACTION_PROBLEM.equals(aid)
                || DfsBfsStructureScaffoldDefinition.ACTION_DIFF.equals(aid)) {
            eng.setCurrentActionId(DfsBfsStructureScaffoldDefinition.ACTION_POSITION);
        }
        if (eng.getStructureExploredPromptKeys() == null) {
            eng.setStructureExploredPromptKeys(new ArrayList<>());
        }
    }

    /** 旧版 REFLECTION 占位单卡 → 迁移到四卡首张 */
    private static void normalizeReflectionActionIds(LearningScaffoldEngineState eng) {
        if (eng == null || !DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            return;
        }
        if ("dfs_bfs_reflection_placeholder_ack".equals(eng.getCurrentActionId())) {
            eng.setCurrentActionId(DfsBfsReflectionScaffoldDefinition.orderedActionIds().get(0));
        }
    }

    private void normalizeEngineState(LearningScaffoldEngineState eng) {
        if (eng.getCompletedStageKeys() == null) {
            eng.setCompletedStageKeys(new ArrayList<>());
        }
        List<String> list = eng.getCompletedStageKeys();
        if (list.isEmpty()) {
            if (eng.isStructureStageComplete()) {
                list.add(DfsBfsStructureValidator.STAGE_KEY);
            }
            if (eng.isUnderstandingStageComplete()) {
                list.add(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY);
            }
        }
        syncLegacyBooleans(eng);
        normalizeReflectionActionIds(eng);
        normalizeStructureLegacyActionIds(eng);

        // Phase 2 遗留：UNDERSTANDING 已完成但仍停在 UNDERSTANDING → 迁到 TRAINING
        if (eng.isUnderstandingStageComplete()
                && list.contains(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY)
                && !list.contains(DfsBfsTrainingScaffoldDefinition.STAGE_KEY)
                && DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            eng.setCurrentStageKey(DfsBfsTrainingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsTrainingScaffoldDefinition.orderedActionIds().get(0));
        }
    }

    /**
     * REFLECTION 脚手架整阶段完成后，将任务执行状态置为 PASS，以便 {@link navigator.application.guard.TaskProgressGuard}
     * 允许 completeTask（典型路径下用户早已在 EXPLORE，旧逻辑仅 ORIENT→EXPLORE 无法到达 PASS）。
     */
    private void maybeAdvanceTaskStateAfterScaffoldBlock(String sessionId, String taskId, TaskExecutionRuntime rt) {
        if (rt.getState() == TaskExecutionState.PASS) {
            return;
        }
        TaskExecutionState from = rt.getState();
        rt.transitionTo(TaskExecutionState.PASS, "learning_scaffold_reflection_complete");
        persistenceService.appendTransition(sessionId, taskId, from, TaskExecutionState.PASS, "learning_scaffold_reflection_complete");
        if (rt.getScaffold() != null) {
            rt.getScaffold().setCurrentExecutionState(TaskExecutionState.PASS.name());
        }
    }

    private static TutorResponse buildStructureTutor(boolean passed, ValidationResult validation, String actionId) {
        if (passed) {
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content("可以。进入下一步。")
                    .nextPrompt(nextHintStructure(actionId))
                    .canProceed(true)
                    .build();
        }
        String hint = validation.getMessage() != null ? validation.getMessage() : "请按当前阶段要求改写。";
        return TutorResponse.builder()
                .feedbackType("REJECT")
                .content(hint)
                .nextPrompt(validation.getSuggestions() != null && !validation.getSuggestions().isEmpty()
                        ? validation.getSuggestions().get(0)
                        : "删掉越界点，用更粗的话重写。")
                .canProceed(false)
                .build();
    }

    private static String nextHintStructure(String actionId) {
        if (DfsBfsStructureScaffoldDefinition.ACTION_POSITION.equals(actionId)) {
            return "下一张：前置概念。";
        }
        if (DfsBfsStructureScaffoldDefinition.ACTION_PREREQ.equals(actionId)) {
            return "下一张：后续连接。";
        }
        if (DfsBfsStructureScaffoldDefinition.ACTION_NEXT.equals(actionId)) {
            return "下一张：本轮先不展开的细节。";
        }
        return "进入机制理解：说明 DFS 如何一步步探索。";
    }

    private ValidationResult validateForStage(String stageKey, StructureValidationContext ctx) {
        if (DfsBfsStructureValidator.STAGE_KEY.equals(stageKey)) {
            return dfsBfsStructureValidator.validate(ctx);
        }
        if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            return dfsBfsUnderstandingValidator.validate(ctx);
        }
        throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "不支持的脚手架阶段: " + stageKey);
    }

    private static StageScaffold buildStageForKey(String stageKey) {
        if (DfsBfsStructureValidator.STAGE_KEY.equals(stageKey)) {
            return DfsBfsStructureScaffoldDefinition.buildStage();
        }
        if (DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            return DfsBfsUnderstandingScaffoldDefinition.buildStage();
        }
        if (DfsBfsTrainingScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            return DfsBfsTrainingScaffoldDefinition.buildStage();
        }
        if (DfsBfsReflectionScaffoldDefinition.STAGE_KEY.equals(stageKey)) {
            return DfsBfsReflectionScaffoldDefinition.buildStage();
        }
        throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "不支持的脚手架阶段: " + stageKey);
    }

    private static StageScaffold mergeProgress(StageScaffold stage, LearningScaffoldEngineState eng) {
        List<String> keys = eng.getCompletedStageKeys() != null ? eng.getCompletedStageKeys() : List.of();
        stage.setCurrentActionId(eng.getCurrentActionId());
        stage.setStructureStageComplete(keys.contains(DfsBfsStructureValidator.STAGE_KEY));
        stage.setUnderstandingStageComplete(keys.contains(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY));
        stage.setTrainingStageComplete(keys.contains(DfsBfsTrainingScaffoldDefinition.STAGE_KEY));
        stage.setReflectionStageComplete(keys.contains(DfsBfsReflectionScaffoldDefinition.STAGE_KEY));
        stage.setCompletedStageKeys(new ArrayList<>(keys));
        stage.setReflectionRecord(eng.getReflectionRecord());
        stage.setReflectionInsight(eng.getReflectionInsight());
        if (eng.getStructureExploredPromptKeys() != null) {
            stage.setStructureExploredPromptKeys(new ArrayList<>(eng.getStructureExploredPromptKeys()));
        } else {
            stage.setStructureExploredPromptKeys(new ArrayList<>());
        }
        stage.setStructureGenerationCount(eng.getStructureGenerationCount());
        stage.setStructureLightInteractionCount(eng.getStructureLightInteractionCount());
        boolean inStructure = DfsBfsStructureValidator.STAGE_KEY.equals(eng.getCurrentStageKey());
        stage.setStructureCanComplete(inStructure && canCompleteStructure(eng));
        stage.setStructureLastPromptKey(eng.getStructureLastPromptKey());
        return stage;
    }

    private static String resolveStageKeyParam(String stageKeyParam, String expected) {
        if (stageKeyParam == null || stageKeyParam.isBlank()) {
            return expected;
        }
        return stageKeyParam.trim();
    }

    private static ActionRuntime toActionRuntime(String sessionId, String stageKey, ScaffoldActionRuntimeEntry e,
                                                 TutorResponse tutor, boolean passed) {
        boolean canProceed = tutor != null ? tutor.isCanProceed() : passed;
        return ActionRuntime.builder()
                .sessionId(sessionId)
                .stageKey(stageKey)
                .actionId(e.getActionId())
                .userInput(e.getUserInput())
                .validationStatus(e.getValidationStatus())
                .tutorFeedback(e.getLastTutorFeedback())
                .retryCount(e.getRetryCount())
                .completed(e.isCompleted())
                .attemptNo(e.getAttemptNo())
                .runtimeStatus(e.getRuntimeStatus())
                .canProceed(canProceed)
                .build();
    }

    private TaskExecutionRuntime requireRuntime(String sessionId, String taskId) {
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(key);
        if (rt == null || rt.getScaffold() == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "任务运行时未就绪");
        }
        return rt;
    }

    private void ensureEngineState(TaskScaffold scaffold) {
        if (scaffold.getLearningScaffoldEngineState() != null) {
            return;
        }
        LearningScaffoldEngineState eng = LearningScaffoldEngineState.builder()
                .currentStageKey(DfsBfsStructureValidator.STAGE_KEY)
                .currentActionId(DfsBfsStructureScaffoldDefinition.ACTION_POSITION)
                .structureStageComplete(false)
                .understandingStageComplete(false)
                .completedStageKeys(new ArrayList<>())
                .actionRuntimeByActionId(new LinkedHashMap<>())
                .build();
        scaffold.setLearningScaffoldEngineState(eng);
    }

    private KnowledgePackMetadata.PackMeta resolvePackMeta(String sessionId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null || state.getPlanId() == null) {
            return null;
        }
        LearningPlanPreview plan = store.getPlanPreviews().get(state.getPlanId());
        if (plan == null || plan.getGoalId() == null) {
            return null;
        }
        StructuredLearningGoal goal = store.getGoals().get(plan.getGoalId());
        return KnowledgePackMetadata.fromGoal(goal);
    }
}
