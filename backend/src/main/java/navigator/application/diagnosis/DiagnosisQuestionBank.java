package navigator.application.diagnosis;

import navigator.domain.model.DiagnosisOption;
import navigator.domain.model.DiagnosisQuestion;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;

import java.util.List;

/**
 * 固定诊断题库。当前返回 6 道题，供 LearnerProfile 推导和 Plan 决策。
 *
 * <p>扩展点：后续第 4 题（q_scope_of_problem）可根据 structuredGoal.topics 动态生成
 * 「最薄弱知识点」子选项。本次先使用固定选项。
 */
public final class DiagnosisQuestionBank {

    private DiagnosisQuestionBank() {
    }

    /**
     * 返回固定 6 道诊断题。
     *
     * <p>扩展点：后续可根据 structuredGoal.topics 动态生成 q_scope_of_problem 的子选项
     * <pre>
     * List&lt;DiagnosisOption&gt; scopeOptions = goal != null &amp;&amp; goal.getTopics() != null
     *     ? buildScopeOptionsFromTopics(goal.getTopics()) : null;
     * // 若 scopeOptions 非空，可替换第 4 题的 options
     * </pre>
     */
    public static List<DiagnosisQuestion> fixedSixQuestions(
            StructuredLearningGoal goal,
            GoalContextSnapshot goalContext) {
        // 本次使用固定选项；未来可在此根据 goal.getTopics() 生成 q_scope_of_problem 的动态子选项
        return List.of(
                questionGoalOutcome(),
                questionFoundationState(),
                questionPrimaryGap(),
                questionScopeOfProblem(),
                questionPreferredEntryMode(),
                questionExecutionRisk()
        );
    }

    private static DiagnosisQuestion questionGoalOutcome() {
        return DiagnosisQuestion.builder()
                .questionId("q_goal_outcome")
                .dimension("GOAL_SITUATION")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("这轮学习你最希望先达到哪种结果？")
                .whyAsking("判断本轮学习的首要目标，影响规划策略。")
                .impactsPlanning(List.of("ENTRY_STRATEGY", "RECOMMENDED_STRATEGY"))
                .options(List.of(
                        opt("PASS_THE_BASICS", "先过关，保住基础分/完成当前要求", 1),
                        opt("BUILD_FRAMEWORK", "先建立整体框架，知道这一块在讲什么", 2),
                        opt("FILL_A_SPECIFIC_GAP", "先补一个明确卡点", 3),
                        opt("SOLVE_TYPICAL_PROBLEMS", "希望能独立做典型题", 4),
                        opt("DEEP_UNDERSTANDING", "希望真正理解并能迁移应用", 5)
                ))
                .build();
    }

    private static DiagnosisQuestion questionFoundationState() {
        return DiagnosisQuestion.builder()
                .questionId("q_foundation_state")
                .dimension("FOUNDATION")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("你对当前主题的基础更接近哪种状态？")
                .whyAsking("判断基础水平，决定从概念讲解还是练习开始。")
                .impactsPlanning(List.of("ENTRY_STRATEGY", "ENTRY_GRANULARITY"))
                .options(List.of(
                        opt("BEGINNER", "刚开始接触，基本不了解", 1),
                        opt("LEARNED_BUT_FORGOTTEN", "以前学过，但现在忘得比较多", 2),
                        opt("BASIC_BUT_FRAGILE", "知道一点基础，但不稳定、容易混", 3),
                        opt("CAN_EXPLAIN_BUT_NOT_STABLE", "能大概讲清概念，但理解还不扎实", 4),
                        opt("CAN_EXPLAIN_BUT_NOT_APPLY", "概念基本懂，但不会做题或不会用", 5),
                        opt("SOLID_WITH_LOCAL_GAPS", "整体还可以，只是有局部薄弱点", 6)
                ))
                .build();
    }

    private static DiagnosisQuestion questionPrimaryGap() {
        return DiagnosisQuestion.builder()
                .questionId("q_primary_gap")
                .dimension("GAP")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("你现在最大的困难更像是哪一种？")
                .whyAsking("识别主要学习缺口，决定是先讲概念、结构还是例题。")
                .impactsPlanning(List.of("RECOMMENDED_STRATEGY"))
                .options(List.of(
                        opt("CONCEPT_GAP", "概念本身没弄懂，很多定义还是模糊的", 1),
                        opt("RELATIONSHIP_GAP", "单个点能懂，但知识点之间串不起来", 2),
                        opt("PROCEDURE_GAP", "知道原理，但真到题目上不知道怎么下手", 3),
                        opt("QUESTION_TYPE_RECOGNITION_GAP", "看到题时，分不清题型，不知道该用哪套方法", 4),
                        opt("EXPRESSION_GAP", "脑子里大概知道，但说不清、写不清、总结不出来", 5)
                ))
                .build();
    }

    private static DiagnosisQuestion questionScopeOfProblem() {
        // 扩展点：后续可根据 structuredGoal.topics 动态生成「最薄弱知识点」子选项
        return DiagnosisQuestion.builder()
                .questionId("q_scope_of_problem")
                .dimension("WEAKNESS_SCOPE")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("你当前的问题范围更接近哪一种？")
                .whyAsking("判断薄弱点范围，影响任务粒度与阶段划分。")
                .impactsPlanning(List.of("ENTRY_GRANULARITY", "PLAN_STAGES"))
                .options(List.of(
                        opt("SINGLE_POINT", "主要卡在一个明确知识点", 1),
                        opt("MULTI_POINT", "卡在几个零散点，但不是整章崩掉", 2),
                        opt("CHAPTER_LEVEL", "这一整章/这一块都不太稳", 3),
                        opt("COURSE_LEVEL", "不是某一章的问题，而是整体基础都比较乱", 4)
                ))
                .build();
    }

    private static DiagnosisQuestion questionPreferredEntryMode() {
        return DiagnosisQuestion.builder()
                .questionId("q_preferred_entry_mode")
                .dimension("PREFERENCE")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("你更希望系统先怎么带你进入学习？")
                .whyAsking("匹配你的学习偏好，提升学习效率。")
                .impactsPlanning(List.of("ENTRY_STRATEGY", "TASK_METHOD"))
                .options(List.of(
                        opt("CONCEPT_FIRST", "先把核心概念讲清楚，再举例", 1),
                        opt("EXAMPLE_FIRST", "先看具体例子，再回到概念", 2),
                        opt("PRACTICE_FIRST", "先做一道题，在过程中带着学", 3),
                        opt("FRAMEWORK_FIRST", "先给整体框架，再逐步填细节", 4),
                        opt("CORE_CONTRAST_FIRST", "先讲容易混淆的点和关键区别", 5)
                ))
                .build();
    }

    private static DiagnosisQuestion questionExecutionRisk() {
        return DiagnosisQuestion.builder()
                .questionId("q_execution_risk")
                .dimension("EXECUTION_RISK")
                .type("SINGLE_CHOICE")
                .required(true)
                .title("按你当前的时间和状态，这轮学习最可能在哪一步掉链子？")
                .whyAsking("预判执行风险，规划时提前规避。")
                .impactsPlanning(List.of("RISK_TAGS", "ENTRY_GRANULARITY"))
                .options(List.of(
                        opt("TIME_PRESSURE", "时间太紧，学不完整", 1),
                        opt("COGNITIVE_OVERLOAD_RISK", "内容一多就容易乱，吸收不过来", 2),
                        opt("OVERCONFIDENCE_RISK", "以为自己懂了，但一做题就暴露问题", 3),
                        opt("CONTINUITY_RISK", "容易中断，今天学了明天接不上", 4),
                        opt("LOW_RISK", "目前主要不是执行问题，而是知识问题", 5)
                ))
                .build();
    }

    private static DiagnosisOption opt(String code, String label, int order) {
        return DiagnosisOption.builder().code(code).label(label).order(order).build();
    }
}
