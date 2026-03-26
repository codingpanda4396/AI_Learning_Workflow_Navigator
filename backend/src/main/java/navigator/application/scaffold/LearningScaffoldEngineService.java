package navigator.application.scaffold;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.scaffold.ActionRuntime;
import navigator.api.dto.scaffold.LearningScaffoldActionResult;
import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.api.dto.scaffold.SubmitLearningScaffoldActionRequest;
import navigator.api.dto.scaffold.TrainingFeedback;
import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
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
                                         ReflectionAssembler reflectionAssembler) {
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
    }

    public StageScaffold getStage(String sessionId, String taskId, String stageKeyParam) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.getScaffold(sessionId, taskId);
        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !DfsBfsStructureValidator.PACK_ID.equals(pack.packId())) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "当前任务未启用学习脚手架引擎（需 DFS/BFS 知识点）");
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
        return mergeProgress(buildStageForKey(stageKey), eng);
    }

    public LearningScaffoldActionResult submitAction(String taskId, SubmitLearningScaffoldActionRequest request) {
        String sessionId = request.getSessionId();
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        taskExecutionFlowService.getScaffold(sessionId, taskId);

        KnowledgePackMetadata.PackMeta pack = resolvePackMeta(sessionId);
        if (pack == null || !DfsBfsStructureValidator.PACK_ID.equals(pack.packId())) {
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
     * STRUCTURE → UNDERSTANDING → TRAINING → REFLECTION；REFLECTION 完成后任务 ORIENT→EXPLORE。
     */
    private boolean advanceAfterPass(LearningScaffoldEngineState eng, String stageKey, String actionId) {
        if (DfsBfsStructureValidator.STAGE_KEY.equals(stageKey)) {
            List<String> order = DfsBfsStructureScaffoldDefinition.orderedActionIds();
            int idx = order.indexOf(actionId);
            if (idx >= 0 && idx < order.size() - 1) {
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

        // Phase 2 遗留：UNDERSTANDING 已完成但仍停在 UNDERSTANDING → 迁到 TRAINING
        if (eng.isUnderstandingStageComplete()
                && list.contains(DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY)
                && !list.contains(DfsBfsTrainingScaffoldDefinition.STAGE_KEY)
                && DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY.equals(eng.getCurrentStageKey())) {
            eng.setCurrentStageKey(DfsBfsTrainingScaffoldDefinition.STAGE_KEY);
            eng.setCurrentActionId(DfsBfsTrainingScaffoldDefinition.orderedActionIds().get(0));
        }
    }

    private void maybeAdvanceTaskStateAfterScaffoldBlock(String sessionId, String taskId, TaskExecutionRuntime rt) {
        if (rt.getState() == TaskExecutionState.ORIENT) {
            TaskExecutionState from = rt.getState();
            rt.transitionTo(TaskExecutionState.EXPLORE, "learning_scaffold_reflection_done");
            persistenceService.appendTransition(sessionId, taskId, from, TaskExecutionState.EXPLORE, "learning_scaffold_reflection_done");
            if (rt.getScaffold() != null) {
                rt.getScaffold().setCurrentExecutionState(TaskExecutionState.EXPLORE.name());
            }
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
        if (DfsBfsStructureScaffoldDefinition.ACTION_PROBLEM.equals(actionId)) {
            return "下一张：说明 DFS/BFS 在知识体系中的位置。";
        }
        if (DfsBfsStructureScaffoldDefinition.ACTION_POSITION.equals(actionId)) {
            return "下一张：用对比句说清核心差异。";
        }
        return "进入 UNDERSTANDING：说明 DFS 如何一步步探索。";
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
                .currentActionId(DfsBfsStructureScaffoldDefinition.ACTION_PROBLEM)
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
