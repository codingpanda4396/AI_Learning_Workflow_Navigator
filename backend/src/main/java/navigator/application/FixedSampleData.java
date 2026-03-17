package navigator.application;

import navigator.domain.enums.*;
import navigator.domain.model.*;

import java.util.List;

/**
 * Sprint 0 固定样例："理解链表"。与接口文档 JSON 对齐。
 */
public final class FixedSampleData {

    public static final String GOAL_ID = "goal_001";
    public static final String DIAGNOSIS_ID = "diag_001";
    public static final String SESSION_ID = "learn_session_001";
    public static final String PLAN_ID = "plan_001";
    public static final String TASK_001 = "task_001";
    public static final String TASK_002 = "task_002";
    public static final String TASK_003 = "task_003";

    public static StructuredLearningGoal structuredGoal(String rawGoalText) {
        return StructuredLearningGoal.builder()
                .rawGoalText(rawGoalText != null ? rawGoalText : "我想搞懂链表")
                .normalizedGoalText("理解链表的核心概念与基本操作")
                .goalType(GoalType.LEARN_NEW_CONCEPT)
                .subject("数据结构")
                .topicScopeType("SINGLE_TOPIC")
                .topics(List.of("链表"))
                .intentDescription("用户希望理解链表概念，并具备后续进入基本操作学习的准备")
                .timeBudget(TimeBudget.WITHIN_30_MIN)
                .urgencyLevel(UrgencyLevel.MEDIUM)
                .expectedDepth("UNDERSTAND_AND_BASIC_USE")
                .selfReportedLevel(SelfReportedLevel.BASIC)
                .preferenceTags(List.of(PreferenceTag.CONCEPT_FIRST, PreferenceTag.STEP_BY_STEP))
                .constraints(List.of())
                .sourceContext("408复习")
                .build();
    }

    public static GoalContextSnapshot goalContextSnapshot() {
        return GoalContextSnapshot.builder()
                .structuredGoal(null) // 响应中不嵌套，避免重复
                .requiresDiagnosis(true)
                .planningMode(PlanningMode.CONCEPT_CLARIFICATION)
                .entryGranularity(EntryGranularity.SMALL)
                .strategyHints(List.of("CORE_CONCEPT_FIRST", "ONE_STEP_ONE_CHECK"))
                .riskTags(List.of("SHALLOW_UNDERSTANDING_RISK"))
                .explanationFocus(List.of("为什么先补概念", "为什么不直接做题"))
                .createdFrom("USER_INPUT_V1")
                .version(1)
                .build();
    }

    public static LearnerProfileSnapshot learnerProfileSnapshot() {
        return LearnerProfileSnapshot.builder()
                .diagnosisId(DIAGNOSIS_ID)
                .foundationLevel(FoundationLevel.BASIC)
                .executionStability(ExecutionStability.MODERATE)
                .timeBudgetLevel(TimeBudget.WITHIN_30_MIN)
                .learningPreference(LearningPreference.CONCEPT_FIRST)
                .blockingPoint("CONCEPT_GAP")
                .urgencyLevel(UrgencyLevel.MEDIUM)
                .blockerTags(List.of("CONCEPT_GAP"))
                .riskTags(List.of("SHALLOW_UNDERSTANDING_RISK"))
                .build();
    }

    public static DiagnosisEvidenceSummary diagnosisEvidenceSummary() {
        return DiagnosisEvidenceSummary.builder()
                .summary("用户对链表有初步接触，但核心概念稳定性不足，当前主要缺口在概念理解层。")
                .keyEvidence(List.of(
                        "用户自评为学过但不太熟",
                        "主要困难选择为概念本身不清楚"
                ))
                .primaryGapType("CONCEPT_GAP")
                .primaryRiskTags(List.of("SHALLOW_UNDERSTANDING_RISK"))
                .explanationPoints(List.of(
                        "不建议直接进入做题",
                        "更适合先做概念澄清型任务"
                ))
                .build();
    }

    public static RecommendedEntry recommendedEntry() {
        return RecommendedEntry.builder()
                .conceptId("linked_list_foundation")
                .title("先建立链表节点与指针连接的基本理解")
                .estimatedMinutes(8)
                .reason("当前主要缺口在概念层，如果直接做题会放大理解断层。")
                .build();
    }

    public static RecommendedStrategy recommendedStrategy() {
        return RecommendedStrategy.builder()
                .code(RecommendedStrategyCode.CONCEPT_CLARIFICATION)
                .label("先澄清核心概念，再做轻量练习")
                .reason("适合基础不稳但已有接触的用户。")
                .build();
    }

    public static List<PlanStage> planStages() {
        return List.of(
                PlanStage.builder().stageCode("STAGE_1").title("概念澄清").objective("理解链表节点、指针和连接关系").estimatedMinutes(15).build(),
                PlanStage.builder().stageCode("STAGE_2").title("轻量检验").objective("用自己的话解释并完成一个微练习").estimatedMinutes(10).build()
        );
    }

    public static List<TaskBlueprint> taskBlueprints() {
        return List.of(
                TaskBlueprint.builder()
                        .taskId(TASK_001)
                        .title("理解链表的基本结构")
                        .taskType(TaskType.CONCEPT_EXPLAIN)
                        .goal("用自己的话说清链表由什么组成")
                        .taskMethod("先请 AI 解释定义和关键特征，再用自己的话复述确认理解")
                        .recommendedPromptTemplate("请用简明语言解释【主题】的定义和 1～2 个关键特征，并举一个最小例子。")
                        .promptScaffold("请先解释什么是链表、节点、指针，它们之间是什么关系，并给一个最小例子。")
                        .completionCriteria(List.of("能说出节点和指针的作用", "能描述节点之间如何连接"))
                        .evidenceToCollect(List.of("interactionCount", "userSummarySubmitted"))
                        .selfEvaluationQuestions(List.of("我能用自己的话说出定义吗？", "我能举出至少一个例子吗？"))
                        .fallbackAction("如果还是混乱，要求导师只用一个两节点例子重新解释")
                        .estimatedMinutes(8)
                        .build(),
                TaskBlueprint.builder()
                        .taskId(TASK_002)
                        .title("对比链表与顺序表")
                        .taskType(TaskType.COMPARE_AND_CONNECT)
                        .goal("理解链表与数组在存储方式上的核心区别")
                        .taskMethod("请 AI 列出要点，自己整理成对比表或关系图")
                        .recommendedPromptTemplate("请从存储结构、插入删除、访问特点等角度对比【主题 A】和【主题 B】的核心区别。")
                        .promptScaffold("请从存储结构、插入删除、访问方式三个角度对比链表和数组。")
                        .completionCriteria(List.of("至少说出两点区别", "能解释为什么链表不要求连续存储"))
                        .evidenceToCollect(List.of("interactionCount", "userSummarySubmitted"))
                        .selfEvaluationQuestions(List.of("我能说出至少 2 个区别/联系吗？", "遇到易混场景我能区分吗？"))
                        .fallbackAction("如果无法比较，先只比较是否连续存储这一点")
                        .estimatedMinutes(7)
                        .build(),
                TaskBlueprint.builder()
                        .taskId(TASK_003)
                        .title("完成一个最小自我解释")
                        .taskType(TaskType.SELF_EXPLANATION)
                        .goal("用自己的话完整解释链表")
                        .taskMethod("不看原文，用自己的话讲出来，再对照检查遗漏")
                        .recommendedPromptTemplate("请先不要讲，让我用自己的话解释【主题】，讲完后你再指出遗漏或误解。")
                        .promptScaffold("请你不用术语堆砌，像讲给同学一样解释链表是什么。")
                        .completionCriteria(List.of("解释中包含节点、指针、连接关系", "表达连贯，不只是复述定义"))
                        .evidenceToCollect(List.of("userSummarySubmitted", "learnerReflection"))
                        .selfEvaluationQuestions(List.of("我的解释是否完整？", "有没有依赖原文才能说出的部分？"))
                        .fallbackAction("先给一句模板开头，再让用户补全")
                        .estimatedMinutes(5)
                        .build()
        );
    }

    public static TaskBlueprint taskBlueprintWithWhy(String taskId, String whyThisTask) {
        List<TaskBlueprint> all = taskBlueprints();
        for (TaskBlueprint t : all) {
            if (t.getTaskId().equals(taskId)) {
                return TaskBlueprint.builder()
                        .taskId(t.getTaskId())
                        .title(t.getTitle())
                        .taskType(t.getTaskType())
                        .goal(t.getGoal())
                        .taskMethod(t.getTaskMethod())
                        .recommendedPromptTemplate(t.getRecommendedPromptTemplate())
                        .promptScaffold(t.getPromptScaffold())
                        .completionCriteria(t.getCompletionCriteria())
                        .selfEvaluationQuestions(t.getSelfEvaluationQuestions())
                        .estimatedMinutes(t.getEstimatedMinutes())
                        .fallbackAction(t.getFallbackAction())
                        .build();
            }
        }
        return null;
    }

    public static String whyThisTask(String taskId) {
        if (TASK_001.equals(taskId)) return "这是当前最关键的基础点，后续所有操作都建立在这里。";
        if (TASK_002.equals(taskId)) return "对比能巩固概念并区分链表与数组。";
        if (TASK_003.equals(taskId)) return "自我解释是检验是否真正理解的标准。";
        return "";
    }

    public static LearningReport learningReport(String sessionId) {
        return LearningReport.builder()
                .sessionId(sessionId)
                .resultStatus(ResultStatus.PARTIALLY_ACHIEVED)
                .goalReview("本轮目标是理解链表的基本概念与结构。")
                .completedProgress(List.of(
                        "已能解释链表由节点和指针构成",
                        "已初步理解链表与数组的存储差异"
                ))
                .unresolvedIssues(List.of("对链表操作场景的理解还不稳定"))
                .evidenceSummary(List.of(
                        "3 个任务中完成了 3 个",
                        "完成了自我解释",
                        "互动次数表明用户有主动澄清行为"
                ))
                .summaryText("本轮已经完成概念澄清，但距离稳定应用还差一步，建议继续做轻量巩固。")
                .nextAction(nextActionDecision())
                .build();
    }

    public static NextActionDecision nextActionDecision() {
        return NextActionDecision.builder()
                .actionType(NextActionType.REINFORCE)
                .reason("概念已建立，但应用层仍需巩固。")
                .nextEntryPoint("链表的基本操作与简单题")
                .adjustmentSignals(List.of("进入轻量练习", "保留概念回顾检查点"))
                .requiresReplan(false)
                .build();
    }
}
