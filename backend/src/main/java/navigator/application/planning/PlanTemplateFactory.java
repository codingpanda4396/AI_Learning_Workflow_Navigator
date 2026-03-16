package navigator.application.planning;

import navigator.domain.enums.TaskType;
import navigator.domain.model.PlanStage;
import navigator.domain.model.TaskBlueprint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 1: 按 strategy 生成 stages + tasks 骨架，successCriteria 按 taskType 模板。
 */
@Component
public class PlanTemplateFactory {

    public StagesAndTasks build(String strategy, String planId, String topicLabel) {
        if (topicLabel == null) topicLabel = "当前主题";
        switch (strategy) {
            case PlanStrategySelector.FOUNDATION_REBUILD:
                return foundationRebuild(planId, topicLabel);
            case PlanStrategySelector.COMPRESSED_REVIEW:
                return compressedReview(planId, topicLabel);
            case PlanStrategySelector.PRACTICE_DRIVEN:
                return practiceDriven(planId, topicLabel);
            case PlanStrategySelector.SYSTEMATIC_PROGRESSIVE:
                return systematicProgressive(planId, topicLabel);
            case PlanStrategySelector.LOCAL_REPAIR:
                return localRepair(planId, topicLabel);
            case PlanStrategySelector.CONCEPT_CLARIFICATION:
            default:
                return conceptClarification(planId, topicLabel);
        }
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
            case PlanStrategySelector.FOUNDATION_REBUILD:
                list.add("PREREQUISITE_GAP");
                break;
            case PlanStrategySelector.COMPRESSED_REVIEW:
                list.add("SHALLOW_UNDERSTANDING_RISK");
                break;
            case PlanStrategySelector.SYSTEMATIC_PROGRESSIVE:
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
            list.add("当前时间预算为 " + ctx.getGoal().getTimeBudget().name() + "，紧迫程度为 " + (ctx.getGoal().getUrgencyLevel() != null ? ctx.getGoal().getUrgencyLevel() : "MEDIUM"));
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
                .estimatedMinutes(minutes)
                .promptScaffold("请完成本任务目标。")
                .completionCriteria(successCriteriaForTaskType(type))
                .evidenceToCollect(List.of("interactionCount", "userSummarySubmitted"))
                .fallbackAction("如遇困难可请求更小步拆解")
                .build();
    }
}
