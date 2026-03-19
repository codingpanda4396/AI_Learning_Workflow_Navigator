package navigator.application.planning;

import navigator.domain.enums.TaskType;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.PlanStage;
import navigator.domain.model.TaskBlueprint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sprint 1: 按 strategy 生成 stages + tasks 骨架，successCriteria 按 taskType 模板。
 */
@Component
public class PlanTemplateFactory {

    private final TimeBudgetEnforcer timeBudgetEnforcer;

    public PlanTemplateFactory(TimeBudgetEnforcer timeBudgetEnforcer) {
        this.timeBudgetEnforcer = timeBudgetEnforcer;
    }

    /**
     * 构建 stages + tasks，若 ctx 含 LearnerStrategyProfile 则按 preferredTaskTypes 调整任务顺序；
     * 若含 TimeBudgetConstraint 则按时间预算压缩。
     */
    public StagesAndTasks build(String strategy, String planId, String topicLabel, PlanningContext ctx) {
        if (topicLabel == null) topicLabel = "当前主题";
        StagesAndTasks result;
        switch (strategy) {
            case PlanStrategySelector.FOUNDATION_PATCH:
                result = foundationRebuild(planId, topicLabel);
                break;
            case PlanStrategySelector.SPRINT_CORRECTION:
                result = compressedReview(planId, topicLabel);
                break;
            case PlanStrategySelector.DRILL_STRENGTHEN:
                result = practiceDriven(planId, topicLabel);
                break;
            case PlanStrategySelector.FRAMEWORK_BUILD:
                result = systematicProgressive(planId, topicLabel);
                break;
            case PlanStrategySelector.LOCAL_REPAIR:
                result = localRepair(planId, topicLabel);
                break;
            case PlanStrategySelector.CONCEPT_CLARIFICATION:
            default:
                result = conceptClarification(planId, topicLabel);
                break;
        }
        if (ctx != null) {
            result = adjustTaskMix(result, ctx.getLearnerStrategyProfile());
            if (ctx.getTimeBudgetConstraint() != null) {
                result = timeBudgetEnforcer.applyConstraint(result, ctx.getTimeBudgetConstraint());
            }
        }
        return result;
    }

    private StagesAndTasks adjustTaskMix(StagesAndTasks original, LearnerStrategyProfile profile) {
        if (profile == null || profile.getPreferredTaskTypes() == null || profile.getPreferredTaskTypes().isEmpty()) {
            return original;
        }
        List<TaskType> preferred = profile.getPreferredTaskTypes();
        List<TaskBlueprint> sorted = new ArrayList<>(original.tasks);
        sorted.sort(Comparator.comparingInt(t -> {
            int idx = preferred.indexOf(t.getTaskType());
            return idx >= 0 ? idx : Integer.MAX_VALUE;
        }));
        return new StagesAndTasks(original.stages, sorted);
    }

    public List<String> successCriteriaForTaskType(TaskType type) {
        switch (type) {
            case CONCEPT_EXPLAIN:
                return List.of("能用自己的话说出定义 + 1 个关键特征");
            case COMPARE_AND_CONNECT:
                return List.of("能说出至少 2 个差异/联系");
            case GUIDED_EXAMPLE:
                return List.of("能跟着例子讲清关键步骤");
            case SELF_EXPLANATION:
                return List.of("能独立复述，不依赖原文案");
            case MICRO_PRACTICE:
                return List.of("能完成 1 个小题/小问并说明原因");
            case CHECKPOINT_REVIEW:
                return List.of("能通过本阶段最关键的检查点");
            default:
                return List.of("完成本任务目标");
        }
    }

    public List<String> risks(PlanningContext ctx, String strategy) {
        List<String> list = new ArrayList<>();
        if (ctx.getGoalContextSnapshot() != null && ctx.getGoalContextSnapshot().getRiskTags() != null) {
            list.addAll(ctx.getGoalContextSnapshot().getRiskTags());
        }
        if (ctx.getLearnerProfileSnapshot() != null && ctx.getLearnerProfileSnapshot().getRiskTags() != null) {
            list.addAll(ctx.getLearnerProfileSnapshot().getRiskTags());
        }
        switch (strategy) {
            case PlanStrategySelector.FOUNDATION_PATCH:
                list.add("PREREQUISITE_GAP");
                break;
            case PlanStrategySelector.SPRINT_CORRECTION:
                list.add("SHALLOW_UNDERSTANDING_RISK");
                break;
            case PlanStrategySelector.FRAMEWORK_BUILD:
                list.add("GOAL_TOO_BROAD");
                break;
            default:
                break;
        }
        return list;
    }

    public List<String> keyEvidence(PlanningContext ctx) {
        List<String> list = new ArrayList<>();
        if (ctx.getGoal() != null) {
            list.add("当前目标是 " + (ctx.getGoal().getGoalType() != null ? ctx.getGoal().getGoalType().name() : "未指定") + "，主题范围为 " + (ctx.getGoal().getTopicScopeType() != null ? ctx.getGoal().getTopicScopeType() : "未指定"));
        }
        if (ctx.getGoal() != null && ctx.getGoal().getTimeBudget() != null) {
            list.add("当前时间预算为 " + ctx.getGoal().getTimeBudget().name() + "，紧迫程度为 " + (ctx.getGoal().getUrgencyLevel() != null ? ctx.getGoal().getUrgencyLevel().name() : "MEDIUM"));
        }
        if (ctx.getLearnerProfileSnapshot() != null && ctx.getLearnerProfileSnapshot().getFoundationLevel() != null) {
            list.add("当前基础判断为 " + ctx.getLearnerProfileSnapshot().getFoundationLevel().name());
        }
        if (ctx.getLearnerProfileSnapshot() != null && ctx.getLearnerProfileSnapshot().getBlockerTags() != null && !ctx.getLearnerProfileSnapshot().getBlockerTags().isEmpty()) {
            list.add("当前主要断层为 " + String.join("、", ctx.getLearnerProfileSnapshot().getBlockerTags()));
        }
        return list.isEmpty() ? List.of("根据目标与诊断生成") : list;
    }

    public static class StagesAndTasks {
        public final List<PlanStage> stages;
        public final List<TaskBlueprint> tasks;

        public StagesAndTasks(List<PlanStage> stages, List<TaskBlueprint> tasks) {
            this.stages = stages;
            this.tasks = tasks;
        }
    }

    private StagesAndTasks foundationRebuild(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("基础澄清").objective("理解" + topic + "的定义与最小示例").estimatedMinutes(10).build(),
                PlanStage.builder().stageCode("STAGE_2").title("自解释校准").objective("用户用自己的话复述并检查前置断层").estimatedMinutes(10).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "解释核心定义与必要术语", TaskType.CONCEPT_EXPLAIN, 10),
                task(planId, 2, "用最小例子演示结构/流程", TaskType.GUIDED_EXAMPLE, 10),
                task(planId, 3, "用户用自己的话复述", TaskType.SELF_EXPLANATION, 8),
                task(planId, 4, "检查是否还存在前置断层", TaskType.CHECKPOINT_REVIEW, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private StagesAndTasks compressedReview(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("核心对比").objective("对比关键区别与典型题入口").estimatedMinutes(8).build(),
                PlanStage.builder().stageCode("STAGE_2").title("典型题快练").objective("1 道典型题快速应用").estimatedMinutes(8).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "对比相邻概念的关键区别", TaskType.COMPARE_AND_CONNECT, 8),
                task(planId, 2, "用一题带出判断入口", TaskType.GUIDED_EXAMPLE, 8),
                task(planId, 3, "1 道典型题快速应用", TaskType.MICRO_PRACTICE, 8),
                task(planId, 4, "检查做题判断是否建立", TaskType.CHECKPOINT_REVIEW, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private StagesAndTasks practiceDriven(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("题型识别").objective("识别题型特征与易混方法").estimatedMinutes(10).build(),
                PlanStage.builder().stageCode("STAGE_2").title("微练习纠偏").objective("做 1~2 个微练习并解释").estimatedMinutes(10).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "看例题识别题型特征", TaskType.GUIDED_EXAMPLE, 10),
                task(planId, 2, "比较容易混淆的方法/结构", TaskType.COMPARE_AND_CONNECT, 8),
                task(planId, 3, "做 1~2 个微练习", TaskType.MICRO_PRACTICE, 10),
                task(planId, 4, "解释为什么这样做", TaskType.SELF_EXPLANATION, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private StagesAndTasks systematicProgressive(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("框架搭建").objective("建立主题关系框架与核心概念").estimatedMinutes(15).build(),
                PlanStage.builder().stageCode("STAGE_2").title("局部填充").objective("每次补一个关键主题并复述").estimatedMinutes(12).build(),
                PlanStage.builder().stageCode("STAGE_3").title("典型题连接").objective("用题目连接框架与应用").estimatedMinutes(12).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "建立主题关系框架", TaskType.COMPARE_AND_CONNECT, 12),
                task(planId, 2, "讲清核心节点概念", TaskType.CONCEPT_EXPLAIN, 10),
                task(planId, 3, "每次补一个关键主题", TaskType.GUIDED_EXAMPLE, 10),
                task(planId, 4, "用户复述局部结构", TaskType.SELF_EXPLANATION, 8),
                task(planId, 5, "用题目连接框架与应用", TaskType.MICRO_PRACTICE, 10),
                task(planId, 6, "判断是否可继续推进", TaskType.CHECKPOINT_REVIEW, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private StagesAndTasks localRepair(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("卡点定位").objective("暴露具体不会的点并讲清").estimatedMinutes(10).build(),
                PlanStage.builder().stageCode("STAGE_2").title("定点修补").objective("针对性例子与检查").estimatedMinutes(10).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "先暴露具体不会的点", TaskType.SELF_EXPLANATION, 8),
                task(planId, 2, "讲清局部卡点", TaskType.CONCEPT_EXPLAIN, 10),
                task(planId, 3, "给一个针对性例子", TaskType.GUIDED_EXAMPLE, 10),
                task(planId, 4, "检查卡点是否解除", TaskType.CHECKPOINT_REVIEW, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private StagesAndTasks conceptClarification(String planId, String topic) {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("核心概念澄清").objective("澄清定义、边界、与易混概念对照").estimatedMinutes(12).build(),
                PlanStage.builder().stageCode("STAGE_2").title("最小应用").objective("用最小例子验证理解").estimatedMinutes(8).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task(planId, 1, "澄清定义、边界、作用", TaskType.CONCEPT_EXPLAIN, 10),
                task(planId, 2, "与易混概念做对照", TaskType.COMPARE_AND_CONNECT, 8),
                task(planId, 3, "用最小例子验证理解", TaskType.GUIDED_EXAMPLE, 8),
                task(planId, 4, "用户用自己的话解释", TaskType.SELF_EXPLANATION, 6)
        );
        return new StagesAndTasks(stages, tasks);
    }

    private TaskBlueprint task(String planId, int index, String title, TaskType type, int minutes) {
        String taskId = planId + "_task_" + index;
        return TaskBlueprint.builder()
                .taskId(taskId)
                .title(title)
                .taskType(type)
                .goal(title)
                .taskMethod(taskMethodForType(type))
                .recommendedPromptTemplate(promptTemplateForType(type))
                .promptScaffold(promptTemplateForType(type))
                .completionCriteria(successCriteriaForTaskType(type))
                .evidenceToCollect(List.of("interactionCount", "userSummarySubmitted"))
                .selfEvaluationQuestions(selfEvaluationForType(type))
                .fallbackAction("如遇困难可请求更小步拆解")
                .estimatedMinutes(minutes)
                .build();
    }

    private String taskMethodForType(TaskType type) {
        return switch (type) {
            case CONCEPT_EXPLAIN -> "先请 AI 解释定义和关键特征，再用自己的话复述确认理解";
            case COMPARE_AND_CONNECT -> "请 AI 列出要点，自己整理成对比表或关系图";
            case GUIDED_EXAMPLE -> "跟着 AI 的步骤走一遍例子，重点理解每一步为什么这样做";
            case SELF_EXPLANATION -> "不看原文，用自己的话讲出来，再对照检查遗漏";
            case MICRO_PRACTICE -> "先明确题型/入口，再做 1～2 个小问，做完解释理由";
            case CHECKPOINT_REVIEW -> "对照完成标准逐条检查，有缺口就补";
            default -> "围绕任务目标与 AI 对话，按完成标准自检";
        };
    }

    private String promptTemplateForType(TaskType type) {
        return switch (type) {
            case CONCEPT_EXPLAIN -> "请用简明语言解释【主题】的定义和 1～2 个关键特征，并举一个最小例子。";
            case COMPARE_AND_CONNECT -> "请从存储方式、插入删除、访问特点等角度对比【主题 A】和【主题 B】的核心区别。";
            case GUIDED_EXAMPLE -> "请用一道典型题，按步骤演示如何判断题型、选用方法、写出关键代码/式子。";
            case SELF_EXPLANATION -> "请先不要讲，让我用自己的话解释【主题】，讲完后你再指出遗漏或误解。";
            case MICRO_PRACTICE -> "请给 1～2 道与【主题】相关的小题，我做完后请你帮我检查思路。";
            case CHECKPOINT_REVIEW -> "请帮我检查：1) 定义是否清楚 2) 与易混概念能否区分 3) 能否做一道最小题。";
            default -> "请完成本任务目标。";
        };
    }

    private List<String> selfEvaluationForType(TaskType type) {
        return switch (type) {
            case CONCEPT_EXPLAIN -> List.of("我能用自己的话说出定义吗？", "我能举出至少一个例子吗？");
            case COMPARE_AND_CONNECT -> List.of("我能说出至少 2 个区别/联系吗？", "遇到易混场景我能区分吗？");
            case GUIDED_EXAMPLE -> List.of("我能否讲清每一步在做什么？", "换一道类似题我能独立开始吗？");
            case SELF_EXPLANATION -> List.of("我的解释是否完整？", "有没有依赖原文才能说出的部分？");
            case MICRO_PRACTICE -> List.of("我能否说明为什么这样做？", "类似题我能举一反三吗？");
            case CHECKPOINT_REVIEW -> List.of("所有检查点都通过了吗？", "还有哪一块不够稳？");
            default -> List.of("完成本任务目标了吗？");
        };
    }
}
