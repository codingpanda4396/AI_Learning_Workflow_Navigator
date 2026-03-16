# MVP 开发冻结清单

## 1. 文档目的

本文档用于对现有 5 份背景信息进行一次系统性收敛，把“目标输入 → 用户诊断 → 学习规划 → 任务执行 → 反馈与 Next Action”这条链路从**方案级描述**压缩为**可以直接进入 MVP 开发的冻结版本**。

它不是新的愿景文档，而是开发前的统一口径文档，核心作用只有三个：

1. 明确 **MVP 到底做什么，不做什么**
2. 明确 **各模块之间流转哪些对象、哪些枚举、哪些关键字段**
3. 明确 **前后端应按什么顺序开发，做到什么程度算打通最小闭环**

---

## 2. 总体结论

**结论：当前已经可以开始 MVP 开发。**

原因不是“设计已经完美”，而是：

- 主链路已经完整
- 模块职责已经清晰
- 核心技术路线已经收敛
- 每个模块都已经具备 MVP 级裁剪空间

当前最大的风险已经不是“想不清楚”，而是：

> **设计继续发散，导致开发迟迟不开始，或者开发中不断改口径。**

因此现在最合理的动作不是继续扩展设计，而是：

> **冻结 MVP 范围，固定主数据对象与枚举，按闭环优先级开始实现。**

---

## 3. 产品一句话定义（冻结版）

本项目的 MVP 不是通用 AI 学习平台，也不是自由聊天问答工具，而是：

> **一个面向计算机学习场景的、具备结构化目标输入、轻量用户诊断、受限个性化规划、任务化执行脚手架与证据驱动 Next Action 的 AI 学习导航原型。**

它解决的核心问题不是“用户没有大模型”，而是：

> **用户不会把 LLM 用成真正有效的学习工具。**

---

## 4. MVP 的唯一主线

MVP 必须只证明一件事：

> **系统能够把用户模糊的学习目标，转化为一条可执行、可解释、可反馈、可调整的学习推进闭环。**

因此 MVP 的唯一主线是：

1. 用户输入当前学习目标与基本约束
2. 系统用少量结构化问题识别本轮状态
3. 系统基于目标 + 状态生成一份受约束的学习计划草案
4. 系统把计划落实为当前任务，并通过内嵌导师脚手架引导学习
5. 系统根据任务执行证据生成学习报告与下一步动作

这条主线一旦跑通，产品价值就成立。

---

## 5. MVP 范围冻结

### 5.1 要做的

MVP 只做以下能力：

- 单轮学习目标输入
- 单轮目标下的轻量诊断
- 基于规则主导、LLM 辅助表达的计划预览
- 单会话任务执行
- 最小执行证据采集
- 单轮学习报告
- 下一步动作决策

### 5.2 暂时不做的

以下内容明确排除在 MVP 之外：

- 长周期课程运营与学期级追踪
- 复杂知识图谱自动构建
- 多学科通用最优规划系统
- 高自由度多轮诊断访谈
- 端到端 LLM 自由生成整套规划与教学内容
- 复杂学习风格人格分析
- 强依赖分数模型的精细掌握度估计
- 大规模行为埋点分析系统
- 社区、排行榜、学习社交等外围能力

### 5.3 约束原则

所有功能设计都必须通过下面这个判断：

> **它是否直接服务于“打通目标输入—诊断—规划—执行—反馈”这条主链路？**

如果不能直接服务，就不进入 MVP。

---

## 6. 目标用户与场景冻结

### 6.1 目标用户

MVP 面向的不是所有学习者，而是：

- 有明确短期学习目标的学生
- 具备使用 LLM 的基础入口，但不会高质量学习
- 在计算机 / 408 / 数据结构等知识学习中容易出现“会问答案，不会系统推进”的用户

### 6.2 目标场景

MVP 重点覆盖以下场景：

- 想快速搞懂某个概念
- 学过但不牢，想补基础
- 会概念但不会做题，想推进到应用
- 考前时间有限，想压缩复习
- 学习范围较大，需要系统给出起点和下一步

### 6.3 学科范围

MVP 默认收敛到：

- 计算机相关知识学习
- 重点示例可围绕 408 / 数据结构 / 算法基础展开

这是为了保证：

- 规划策略可控
- 任务模板可定义
- 演示案例聚焦

---

## 7. 系统角色分工冻结

### 7.1 规则系统做什么

规则系统负责：

- 结构化字段校验
- 目标裁剪
- 诊断结果归因
- 风险标签识别
- 规划候选生成
- 规划策略选择边界控制
- Next Action 决策主逻辑

### 7.2 LLM 做什么

LLM 在 MVP 中只承担以下角色：

- 对原始目标做轻量理解与补充解释
- 把规则产物转成更自然的规划说明
- 在执行阶段作为受任务约束的导师
- 在报告阶段把结构化结论转成可读反馈文案

### 7.3 LLM 不做什么

LLM 不直接承担：

- 诊断主决策
- 规划主决策
- Next Action 主决策
- 长篇自由教学内容生成

冻结原则：

> **LLM 负责增强表达与交互体验，规则负责稳定决策与结构化状态流转。**

---

## 8. 主闭环的数据流冻结

### 8.1 总链路

```text
Goal Input
  -> StructuredLearningGoal + GoalContextSnapshot
Diagnosis
  -> LearnerProfileSnapshot + DiagnosisEvidenceSummary
Planning
  -> LearningPlanPreview + TaskBlueprints
Execution
  -> TaskExecutionRecord + ExecutionEvidenceSummary
Feedback
  -> LearningReport + NextActionDecision
```

### 8.2 主对象流转说明

#### 第一阶段：目标输入产物
输出两个主对象：

- `StructuredLearningGoal`
- `GoalContextSnapshot`

其中：
- `StructuredLearningGoal` 是后续模块的主消费对象
- `GoalContextSnapshot` 是解释与约束补充对象

#### 第二阶段：用户诊断产物
输出两个主对象：

- `LearnerProfileSnapshot`
- `DiagnosisEvidenceSummary`

其中：
- `LearnerProfileSnapshot` 决定规划的起点与推进方式
- `DiagnosisEvidenceSummary` 用于解释“为什么这么判断”

#### 第三阶段：学习规划产物
输出两个主对象：

- `LearningPlanPreview`
- `TaskBlueprint[]`

其中：
- `LearningPlanPreview` 给前端展示，也供用户确认
- `TaskBlueprint[]` 是执行层真正的上游配置

#### 第四阶段：任务执行产物
输出两个主对象：

- `TaskExecutionRecord[]`
- `ExecutionEvidenceSummary`

其中：
- `TaskExecutionRecord[]` 记录具体任务过程
- `ExecutionEvidenceSummary` 汇总本轮证据供反馈层判断

#### 第五阶段：反馈与下一步产物
输出两个主对象：

- `LearningReport`
- `NextActionDecision`

其中：
- `LearningReport` 面向用户
- `NextActionDecision` 面向系统与下一轮链路

---

## 9. 各模块冻结版设计

# 9.1 模块一：目标输入

### 9.1.1 模块目标

把用户当前学习意图转化为后续模块可直接消费的结构化目标对象。

### 9.1.2 MVP 只保留的输入字段

前端只收以下字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| rawGoalText | string | 是 | 用户当前最想解决的学习目标 |
| timeBudget | enum | 否 | 本轮可投入时间 |
| selfReportedLevel | enum | 否 | 自评基础水平 |
| preferenceTags | enum[] | 否 | 学习偏好 |
| goalTypeHint | enum | 否 | 用户显式选择的目标类型提示 |
| subjectHint | string | 否 | 学科或课程提示 |
| topicHints | string[] | 否 | 知识点提示 |
| sourceContext | string | 否 | 材料来源 |

### 9.1.3 MVP 强制解析出的结构化字段

后端必须至少解析出：

| 字段 | 说明 |
|---|---|
| goalId | 本轮目标唯一标识 |
| rawGoalText | 原始目标文本 |
| goalType | 目标类型 |
| subject | 学科 |
| topics | 主题列表 |
| urgencyLevel | 紧迫程度 |
| expectedDepth | 预期深度 |
| planningMode | 建议规划模式 |
| riskTags | 初步目标风险标签 |

### 9.1.4 GoalType 冻结枚举

```text
LEARN_NEW_CONCEPT
REVIEW_FOR_EXAM
FIX_SPECIFIC_BLOCKER
PRACTICE_ENHANCEMENT
BUILD_SYSTEMATIC_UNDERSTANDING
```

### 9.1.5 TimeBudget 冻结枚举

```text
WITHIN_15_MIN
WITHIN_30_MIN
WITHIN_60_MIN
MULTI_DAY
LONG_TERM
```

### 9.1.6 SelfReportedLevel 冻结枚举

```text
BEGINNER
BASIC
PARTIAL_UNDERSTANDING
CAN_EXPLAIN_BUT_NOT_APPLY
SOLID_BUT_WANT_IMPROVE
```

### 9.1.7 PreferenceTag 冻结枚举

```text
CONCEPT_FIRST
EXAMPLE_FIRST
PRACTICE_FIRST
LESS_THEORY
STEP_BY_STEP
EXAM_ORIENTED
```

### 9.1.8 Goal 输入页冻结要求

MVP 页面只保留：

- 一个主输入框
- 四个轻量辅助选项
- 一个提交按钮
- 一个“系统将基于你的目标生成个性化学习路径”的简要说明

不做：

- 多页面目标配置器
- 复杂向导
- 大量解释性提示

### 9.1.9 本模块完成标准

满足以下条件即可视为完成：

- 前端能提交目标表单
- 后端能返回结构化目标对象
- 返回结果足够驱动诊断问题裁剪
- 原始目标与结构化目标都能落库

---

# 9.2 模块二：用户诊断

### 9.2.1 模块目标

在当前目标约束下，用低负担结构化问题识别本轮学习状态、核心阻塞点和执行风险。

### 9.2.2 MVP 只做“本轮画像快照”

用户诊断不做长期人格，不做复杂画像系统，只做：

> **Learner Profile Snapshot：当前目标下的用户状态快照**

### 9.2.3 MVP 题量冻结

默认只做 **4~6 题**，且题目必须能改变后续规划。

### 9.2.4 MVP 诊断维度冻结

只保留以下 6 个维度：

| 维度 | 作用 |
|---|---|
| GOAL_SITUATION | 校验目标情境与成功标准 |
| FOUNDATION | 判断基础层级 |
| BLOCKER_TYPE | 识别主要阻塞点 |
| APPLICATION_STATE | 判断是否停留在“懂概念不会用” |
| EXECUTION_RISK | 判断时间压力、负荷、卡顿风险 |
| CONFIDENCE_ALIGNMENT | 判断自评与真实状态是否可能失配 |

### 9.2.5 BlockerTag 冻结枚举

```text
PREREQUISITE_GAP
CONCEPT_CONFUSION
STRUCTURE_NOT_CLEAR
CANNOT_APPLY
STEP_BREAKDOWN
TIME_PRESSURE
OVERCONFIDENCE_RISK
LOW_EFFICIENCY_RISK
```

### 9.2.6 LearnerProfileSnapshot 冻结字段

```java
public class LearnerProfileSnapshot {
    private String diagnosisId;
    private FoundationLevel foundationLevel;
    private ConfidenceLevel confidenceLevel;
    private ComprehensionPattern comprehensionPattern;
    private ExecutionPattern executionPattern;
    private List<String> blockerTags;
    private List<String> riskTags;
    private String suggestedEntryStrategy;
    private String suggestedGranularity;
    private String suggestedFeedbackFrequency;
    private List<String> planningHints;
}
```

### 9.2.7 MVP 推荐固定题模板

建议第一版固定以下题型：

1. 你目前对这个主题最接近哪种状态？
2. 你现在最卡的是哪一类问题？
3. 如果让你自己解释/做题，你最容易卡在哪一步？
4. 你这次更想先搞懂概念，还是更想快速解决题目/考试问题？
5. 你现在的时间压力大吗？
6. （可选风险校验）你觉得自己已经会了，但经常做不出来吗？

### 9.2.8 诊断页冻结要求

前端只做：

- 单页卡片式问题展示
- 单选/多选为主
- 提交后返回结构化诊断结果

不做：

- 长对话式访谈
- 开放式大段文本回答
- 多轮追问分支树

### 9.2.9 本模块完成标准

满足以下条件即可视为完成：

- 目标输入结果能驱动诊断题裁剪或默认题集生成
- 用户提交诊断答案后，后端能产出 `LearnerProfileSnapshot`
- 诊断结果能解释成规划控制信号

---

# 9.3 模块三：学习规划

### 9.3.1 模块目标

将“目标 + 状态 + 约束”编译为一份可执行、可解释、可调整的学习推进方案。

### 9.3.2 规划模块的本质

规划不是出一段建议，而是输出：

- 从哪里开始
- 采用什么主策略
- 任务怎么排
- 成功标准是什么
- 执行中要收什么证据
- 不顺利时怎么调整

### 9.3.3 MVP 规划模式冻结

```text
STEADY_FOUNDATION
CONCEPT_CLARIFICATION
PRACTICE_DRIVEN
EXAM_CRASH
SYSTEMATIC_BUILD
LOCAL_REPAIR
```

### 9.3.4 MVP 推荐策略 code 冻结

```text
REBUILD_FOUNDATION
CLARIFY_CORE_CONCEPT
PRACTICE_WITH_SCAFFOLD
COMPRESSED_REVIEW
PATCH_PREREQUISITE
PROGRESSIVE_SYSTEMATIC
```

### 9.3.5 PlanningContext 冻结对象

```java
public class PlanningContext {
    private StructuredLearningGoal goal;
    private GoalContextSnapshot goalSnapshot;
    private LearnerProfileSnapshot learnerProfile;
    private DiagnosisEvidenceSummary diagnosisEvidence;
    private String planningMode;
    private List<String> constraints;
    private List<String> strategyHints;
    private List<String> riskTags;
}
```

### 9.3.6 LearningPlanPreview 冻结字段

```java
public class LearningPlanPreview {
    private String planId;
    private String goalId;
    private RecommendedEntry recommendedEntry;
    private RecommendedStrategy recommendedStrategy;
    private List<PlanStage> stages;
    private List<TaskBlueprint> tasks;
    private List<String> successCriteria;
    private List<String> keyEvidence;
    private List<String> risks;
    private boolean previewOnly;
}
```

### 9.3.7 RecommendedEntry 冻结字段

| 字段 | 说明 |
|---|---|
| conceptId | 推荐入口锚点 |
| title | 当前建议从哪里开始 |
| estimatedMinutes | 预计所需时间 |
| reason | 为什么从这里开始 |

### 9.3.8 TaskBlueprint 冻结字段

| 字段 | 说明 |
|---|---|
| taskId | 任务 ID |
| title | 任务标题 |
| taskType | 任务类型 |
| goal | 当前任务目标 |
| estimatedMinutes | 预计时长 |
| promptScaffold | 推荐提问脚手架 |
| completionCriteria | 完成标准 |
| evidenceToCollect | 应收集的证据 |
| fallbackAction | 失败/停滞时的回退动作 |

### 9.3.9 TaskType 冻结枚举

```text
CONCEPT_EXPLAIN
COMPARE_AND_CONNECT
GUIDED_EXAMPLE
SELF_EXPLANATION
MICRO_PRACTICE
CHECKPOINT_REVIEW
```

### 9.3.10 规划页冻结要求

前端只展示：

- 推荐入口点
- 为什么这么安排
- 本轮策略
- 3~5 个任务步骤
- 成功标准
- 开始执行按钮

不做：

- 长篇规划文章
- 大规模课程目录树
- 复杂可视化甘特图

### 9.3.11 本模块完成标准

满足以下条件即可视为完成：

- 后端能根据目标 + 诊断产出计划预览
- 规划结果中包含可供执行层消费的任务蓝图
- 用户可以确认计划并进入执行

---

# 9.4 模块四：任务执行

### 9.4.1 模块目标

把规划结果落实为一组可推进、可对话、可检查、可留痕的学习任务。

### 9.4.2 模块产品定义

执行模块产品化的不是知识正文，而是：

- 学习动作
- 提问方式
- 对话脚手架
- 完成标准
- 自检逻辑
- 执行证据

### 9.4.3 MVP 执行页只做什么

每个任务页面只保留以下区域：

1. 当前任务目标
2. 为什么做这一步
3. 推荐提问模板
4. 与导师对话入口
5. 用户输出区 / 完成记录区
6. 完成标准提示
7. 完成任务按钮

### 9.4.4 内嵌导师冻结要求

导师必须始终绑定：

- 当前任务标题
- 当前任务目标
- 当前任务类型
- 用户当前画像摘要
- 当前推荐交互方式
- 当前禁止行为边界

导师行为要求：

- 优先引导而不是直接给完整答案
- 优先围绕当前任务回答
- 必要时提供最小例子
- 允许追问，但不鼓励大幅偏航

### 9.4.5 ExecutionEvidence 冻结内容

执行阶段至少收集以下证据：

| 证据 | 说明 |
|---|---|
| interactionCount | 是否真的与导师互动 |
| userSummarySubmitted | 是否完成自我解释/总结 |
| microPracticeResult | 微练习结果 |
| stuckTag | 是否出现停滞 |
| skipped | 是否跳过任务 |
| duration | 任务耗时 |
| completionStatus | 是否完成 |
| detectedIssueTags | 执行中暴露的问题标签 |

### 9.4.6 TaskExecutionRecord 冻结字段

```java
public class TaskExecutionRecord {
    private String taskId;
    private String taskType;
    private String completionStatus;
    private Integer durationMinutes;
    private Integer interactionCount;
    private boolean userSummarySubmitted;
    private String microPracticeResult;
    private List<String> detectedIssueTags;
    private List<String> behaviorSignals;
    private String learnerReflection;
}
```

### 9.4.7 执行页不做的事

明确不做：

- 自动生成整章讲义
- 任务内高度自由的开放学习空间
- 复杂富文本知识编辑器
- 完全依赖用户主观点完成

### 9.4.8 本模块完成标准

满足以下条件即可视为完成：

- 用户能从计划进入任务执行
- 每个任务至少有明确目标、提问脚手架、完成标准
- 系统能记录任务完成状态和最小执行证据
- 执行结果可以汇总给反馈模块

---

# 9.5 模块五：反馈与 Next Action

### 9.5.1 模块目标

对整轮学习进行结果归纳、成因解释与下一步调度。

### 9.5.2 模块本质

反馈模块不是总结页，而是：

- 结果编译器
- 原因归因器
- 下一步调度器

### 9.5.3 MVP 结果状态冻结

```text
ACHIEVED
PARTIALLY_ACHIEVED
NOT_ACHIEVED
```

### 9.5.4 MVP NextActionType 冻结

```text
CONTINUE
REINFORCE
REMEDIATE_PREREQUISITE
REDUCE_GRANULARITY
CHANGE_STRATEGY
```

### 9.5.5 LearningReport 冻结结构

报告只保留以下 6 个部分：

1. **本轮目标回顾**
2. **本轮完成情况**
3. **已经推进了什么**
4. **仍卡在哪里**
5. **为什么系统这么判断**
6. **下一步建议动作**

### 9.5.6 LearningReport 冻结字段

```java
public class LearningReport {
    private String sessionId;
    private String resultStatus;
    private String goalReview;
    private List<String> completedProgress;
    private List<String> unresolvedIssues;
    private List<String> evidenceSummary;
    private String summaryText;
    private NextActionDecision nextAction;
}
```

### 9.5.7 NextActionDecision 冻结字段

```java
public class NextActionDecision {
    private String actionType;
    private String reason;
    private String nextEntryPoint;
    private List<String> adjustmentSignals;
    private boolean requiresReplan;
}
```

### 9.5.8 决策逻辑冻结原则

Next Action 主决策优先采用规则：

- 达成成功标准且风险下降：`CONTINUE`
- 基本理解但不稳定：`REINFORCE`
- 明显暴露前置缺口：`REMEDIATE_PREREQUISITE`
- 任务过大或多次卡顿：`REDUCE_GRANULARITY`
- 当前策略明显不匹配：`CHANGE_STRATEGY`

### 9.5.9 反馈页冻结要求

前端只展示：

- 本轮结果状态
- 完成情况总结
- 关键证据
- 下一步建议
- 继续学习按钮

不做：

- 复杂成长仪表盘
- 历史趋势大屏
- 多维雷达图系统

### 9.5.10 本模块完成标准

满足以下条件即可视为完成：

- 系统能基于执行证据给出结果状态
- 系统能给出下一步动作类型
- 用户能从报告页继续进入下一轮推进

---

## 10. 跨模块统一枚举冻结

为避免前后端命名不一致，MVP 阶段建议统一以下关键枚举。

### 10.1 GoalType

```text
LEARN_NEW_CONCEPT
REVIEW_FOR_EXAM
FIX_SPECIFIC_BLOCKER
PRACTICE_ENHANCEMENT
BUILD_SYSTEMATIC_UNDERSTANDING
```

### 10.2 PlanningMode

```text
STEADY_FOUNDATION
CONCEPT_CLARIFICATION
PRACTICE_DRIVEN
EXAM_CRASH
SYSTEMATIC_BUILD
LOCAL_REPAIR
```

### 10.3 StrategyCode

```text
REBUILD_FOUNDATION
CLARIFY_CORE_CONCEPT
PRACTICE_WITH_SCAFFOLD
COMPRESSED_REVIEW
PATCH_PREREQUISITE
PROGRESSIVE_SYSTEMATIC
```

### 10.4 TaskType

```text
CONCEPT_EXPLAIN
COMPARE_AND_CONNECT
GUIDED_EXAMPLE
SELF_EXPLANATION
MICRO_PRACTICE
CHECKPOINT_REVIEW
```

### 10.5 ResultStatus

```text
ACHIEVED
PARTIALLY_ACHIEVED
NOT_ACHIEVED
```

### 10.6 NextActionType

```text
CONTINUE
REINFORCE
REMEDIATE_PREREQUISITE
REDUCE_GRANULARITY
CHANGE_STRATEGY
```

### 10.7 Common Risk Tags

```text
TIME_PRESSURE
OVERCONFIDENCE_RISK
LOW_EFFICIENCY_RISK
PREREQUISITE_GAP
GOAL_TOO_BROAD
GOAL_TOO_VAGUE
```

---

## 11. 前后端接口层冻结建议

下面不是最终 API 文档，而是 MVP 阶段建议固定的最小接口集合。

### 11.1 目标输入

#### `POST /api/goals`

作用：创建目标输入并返回结构化目标结果。

返回核心：

- `goalId`
- `structuredGoal`
- `goalContextSnapshot`

### 11.2 诊断会话

#### `POST /api/diagnosis/sessions`

作用：基于目标生成诊断题集。

返回核心：

- `diagnosisId`
- `sessionId`
- `questions[]`

#### `POST /api/diagnosis/submissions`

作用：提交诊断答案并返回画像快照。

返回核心：

- `learnerProfileSnapshot`
- `diagnosisEvidenceSummary`

### 11.3 计划预览

#### `POST /api/learning-plans/preview`

作用：生成学习计划预览。

返回核心：

- `planId`
- `recommendedEntry`
- `recommendedStrategy`
- `tasks[]`
- `successCriteria[]`

#### `POST /api/learning-plans/commit`

作用：确认计划，进入执行会话。

返回核心：

- `sessionId`
- `taskSequence`

### 11.4 任务执行

#### `GET /api/sessions/{sessionId}/current-task`

作用：获取当前任务。

#### `POST /api/tasks/{taskId}/interactions`

作用：记录执行交互摘要或用户行为信号。

#### `POST /api/tasks/{taskId}/complete`

作用：提交任务完成结果。

返回核心：

- `taskExecutionRecord`
- `nextTaskAvailable`

### 11.5 报告与下一步

#### `GET /api/sessions/{sessionId}/report`

作用：获取学习报告。

返回核心：

- `learningReport`
- `nextActionDecision`

#### `POST /api/sessions/{sessionId}/next-action`

作用：确认下一步动作，继续链路。

---

## 12. 数据库/落库对象冻结建议

MVP 不要求数据库设计极度完美，但至少要保证“每一环的关键对象可回溯”。

### 12.1 最小表集合建议

| 表名 | 作用 |
|---|---|
| learning_goal | 保存原始目标输入与结构化目标 |
| diagnosis_session | 保存诊断会话与题目快照 |
| diagnosis_submission | 保存诊断答案 |
| learner_profile_snapshot | 保存本轮画像快照 |
| learning_plan | 保存规划预览与确认结果 |
| learning_task | 保存任务蓝图与任务实例 |
| task_execution_record | 保存任务执行记录 |
| learning_report | 保存本轮学习报告 |
| next_action_decision | 保存下一步动作 |

### 12.2 落库原则

必须保证以下信息可追溯：

- 用户原始目标是什么
- 系统如何理解这个目标
- 系统问了哪些诊断问题
- 用户如何回答
- 系统因此做出了什么画像判断
- 系统基于此生成了什么计划
- 用户执行了哪些任务
- 任务中暴露了哪些证据
- 最终结果状态是什么
- 为什么建议下一步这样做

---

## 13. 前端页面冻结建议

MVP 前端页面只保留以下页面：

### 13.1 目标输入页

作用：收集目标与约束。

### 13.2 诊断页

作用：展示 4~6 个结构化问题并提交。

### 13.3 计划预览页

作用：展示推荐入口、策略、任务步骤并确认开始。

### 13.4 任务执行页

作用：执行当前任务并记录结果。

### 13.5 学习报告页

作用：展示本轮反馈与下一步动作。

### 13.6 页面原则

每一页都必须只回答一个问题：

- 目标输入页：你想学什么？
- 诊断页：你现在以什么状态去学？
- 计划预览页：系统准备怎么带你学？
- 任务执行页：你当前这一步具体怎么学？
- 报告页：你这一轮到底学到了什么，下一步是什么？

---

## 14. 最小演示链路冻结

为了比赛演示清晰，建议固定一条标准演示链路：

### 14.1 示例场景

用户输入：

> 我想搞懂链表，但我会一点概念，做题总是不会。

### 14.2 系统演示流程

1. 用户填写目标输入
2. 系统发起 4~6 个诊断题
3. 系统判断：概念有基础，但应用存在断裂，建议从“链表节点与指针变化示例”开始
4. 系统展示计划预览：
   - 推荐入口：单链表插入与删除中的指针变化
   - 策略：`PRACTICE_WITH_SCAFFOLD`
   - 任务：概念澄清 → 最小例子 → 自我解释 → 微练习
5. 用户进入任务执行页，与导师互动并提交自己的解释
6. 系统生成报告：本轮部分达成，建议补一轮“插入/删除变式练习”
7. 系统给出 `REINFORCE` 的 Next Action

这条演示链路足以证明整个系统闭环成立。

---

## 15. 开发优先级冻结

### P0：先打通主链路

必须优先完成：

- 目标输入提交
- 诊断题生成与提交
- 计划预览生成
- 任务执行与完成提交
- 学习报告生成

### P1：再提升结构化程度

包括：

- 风险标签更稳定
- 规划解释更自然
- 执行证据更完整
- Next Action 更一致

### P2：最后再优化体验

包括：

- 页面视觉优化
- 更自然的文案
- 更好的导师提示
- 更丰富的报告呈现

冻结原则：

> **先跑通，再变强；先结构化，再美化。**

---

## 16. 推荐开发顺序

### 第一步：冻结对象与枚举

先把：

- GoalType
- StrategyCode
- TaskType
- ResultStatus
- NextActionType

以及主对象 DTO 统一下来。

### 第二步：打通后端主链路

先不追求界面漂亮，确保以下链路可跑：

```text
create goal
-> create diagnosis session
-> submit diagnosis
-> preview plan
-> commit plan
-> run task
-> complete task
-> generate report
```

### 第三步：补前端最小页面

只做能串起来的 5 个页面。

### 第四步：补导师上下文与文案增强

当结构化链路稳定后，再增加更好的导师 prompt 和展示文案。

---

## 17. 风险提示与收敛建议

### 17.1 当前最容易失控的地方

- 诊断维度越做越多
- 规划解释越写越大
- 执行模块重新变成聊天页
- 反馈模块被做成花哨仪表盘
- LLM 被放权过多导致结果不稳定

### 17.2 收敛建议

如果开发过程中出现分歧，一律回到这个判断：

> **它是否让系统更稳定地完成“目标—诊断—规划—执行—反馈”这一闭环？**

如果答案不是明确的“是”，就不进 MVP。

---

## 18. 最终冻结结论

现有 5 份背景信息已经足够支撑 MVP 开发启动。

当前真正需要冻结的不是大方向，而是：

- 哪些字段必须实现
- 哪些枚举必须统一
- 哪些对象必须能流转
- 哪些页面必须能打通
- 哪些能力明确不做

因此，MVP 的开发目标不应再表述为“做一个完整 AI 学习平台”，而应表述为：

> **做一个可以稳定演示“结构化诊断 + 受限规划 + 任务脚手架 + 证据驱动 Next Action”的 AI 学习导航闭环原型。**

这就是当前最合理、最可落地、最适合比赛阶段的开发冻结版本。
