# 系统规划页面个性化机制说明

## 1. 结论先说

当前系统规划页面的“个性化”不是大模型自由生成，而是基于**目标解析 + 诊断画像 + 规则选择 + 模板装配 + 前端解释映射**完成的。

也就是说，规划页的个性化本质上是一个**受约束的规则式决策系统**：

1. 用户先输入学习目标，系统推导目标类型、规划模式、风险标签、切入粒度。
2. 用户完成诊断题，系统生成学习者画像、主要缺口、偏好与风险。
3. 规划服务把这些信息组装为 `PlanningContext`。
4. 规则引擎选择最合适的规划策略。
5. 模板工厂按策略生成阶段、任务、推荐起步动作。
6. 前端再把这些结构化结果翻译成更像“为你定制”的规划页文案和展示。

## 2. 个性化输入来自哪里

### 2.1 目标输入层

目标创建后，后端会先从 `StructuredLearningGoal` 推导 `GoalContextSnapshot`。

关键代码：

- [backend/src/main/java/navigator/application/goal/GoalContextDeriver.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/goal/GoalContextDeriver.java)

这一层会产出几类和规划直接相关的信息：

- `planningMode`：例如概念澄清、系统搭建、练习驱动、考前冲刺
- `entryGranularity`：入口粒度是 `MICRO / SMALL / MEDIUM`
- `riskTags`：如时间压力、目标过大、目标过于模糊、过度自信风险
- `strategyHints`：如先补前置、先看例子、一步一步推进、先搭框架
- `explanationFocus`：规划页解释时应该强调什么

当前判定逻辑主要依据：

- `goalType`
- `urgencyLevel`
- `topicScopeType`
- `selfReportedLevel`
- `preferenceTags`
- `timeBudget`

### 2.2 诊断画像层

诊断页不是装饰，它直接喂给规划页个性化。

关键代码：

- [backend/src/main/java/navigator/application/diagnosis/DiagnosisQuestionBank.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/diagnosis/DiagnosisQuestionBank.java)
- [backend/src/main/java/navigator/application/diagnosis/LearnerStrategyProfileDeriver.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/diagnosis/LearnerStrategyProfileDeriver.java)

诊断题当前固定为 6 题，分别影响：

- 目标结果导向
- 基础状态
- 主要缺口类型
- 问题范围
- 偏好的进入方式
- 执行风险

诊断后主要生成两份数据：

1. `LearnerProfileSnapshot`
  - 基础水平
  - 执行稳定性
  - 时间预算等级
  - 学习偏好
  - blockerTags
  - riskTags
2. `LearnerStrategyProfile`
  - `preferredTaskTypes`
  - `scaffoldIntensity`
  - `feedbackStyle`
  - `checkpointFrequency`
  - `riskMitigationTags`

这意味着规划页不只是看“你要学什么”，还看“你现在处于什么状态、适合怎么进入”。

## 3. 规划页个性化是怎么被算出来的

### 3.1 先组装统一规划上下文

关键代码：

- [backend/src/main/java/navigator/application/planning/PlanningContextAssembler.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/planning/PlanningContextAssembler.java)

规划页真正使用的是 `PlanningContext`，它把以下对象组装在一起：

- `goal`
- `goalContextSnapshot`
- `learnerProfileSnapshot`
- `learnerStrategyProfile`
- `diagnosisEvidenceSummary`
- `timeBudgetConstraint`

其中 `timeBudgetConstraint` 也会被个性化推导：

- 不同时间预算，对应不同总时长上限
- 不同入口粒度，对应不同最大任务数
- 某些 showcase 主题会额外保留更多任务，避免展示被压缩

### 3.2 再用规则选择规划策略

关键代码：

- [backend/src/main/java/navigator/application/planning/PlanStrategySelector.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/planning/PlanStrategySelector.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/FrameworkBuildRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/FrameworkBuildRule.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/FoundationPatchRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/FoundationPatchRule.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/SprintCorrectionRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/SprintCorrectionRule.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/DrillStrengthenRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/DrillStrengthenRule.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/LocalRepairRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/LocalRepairRule.java)
- [backend/src/main/java/navigator/application/rule/planning/rules/ConceptClarificationRule.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/rule/planning/rules/ConceptClarificationRule.java)

当前可选策略有 6 种：

- `FOUNDATION_PATCH`
- `FRAMEWORK_BUILD`
- `DRILL_STRENGTHEN`
- `SPRINT_CORRECTION`
- `LOCAL_REPAIR`
- `CONCEPT_CLARIFICATION`

规则判断会综合看：

- 目标类型
- 是否高紧急度
- 是否初学者
- 是否是单点问题
- 是否是章节/课程级范围
- 是否有前置缺口
- 主要 gap 类型是什么

所以规划页中的“推荐策略”其实是明确规则命中的结果，不是随机文案。

### 3.3 用策略生成推荐入口、阶段和任务

关键代码：

- [backend/src/main/java/navigator/application/PlanningApplicationService.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/PlanningApplicationService.java)
- [backend/src/main/java/navigator/application/planning/RecommendedEntryBuilder.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/planning/RecommendedEntryBuilder.java)
- [backend/src/main/java/navigator/application/planning/PlanTemplateFactory.java](/D:/Panda_Code/AI_Learning_Workflow_Navigator/backend/src/main/java/navigator/application/planning/PlanTemplateFactory.java)

后端 `preview()` 的核心流程是：

1. 组装 `PlanningContext`
2. 选择策略
3. 生成 `RecommendedEntry`
4. 生成 `stages + tasks`
5. 生成 `successCriteria / keyEvidence / risks`
6. 返回 `PlanPreviewData`

个性化具体体现在：

#### 1）推荐起步动作会变

`RecommendedEntryBuilder` 会根据策略生成不同入口标题和理由，例如：

- 基础薄弱时，先讲定义、组成、最小示例
- 系统学习时，先搭知识框架
- 单点卡住时，先做定点修补
- 题型识别差时，先做例题驱动

#### 2）任务路径会变

`PlanTemplateFactory` 会根据策略切换不同模板：

- 基础修补：先概念，再最小示例，再自解释，再检查
- 冲刺纠偏：先做关键对比，再做典型题入口
- 练习强化：先识别题型，再微练习
- 系统搭建：先搭框架，再填局部，再连题目

#### 3）任务顺序会变

如果诊断得到 `LearnerStrategyProfile.preferredTaskTypes`，系统还会调整任务顺序。

例如：

- `EXAMPLE_FIRST` 用户更容易先看到例子
- `PRACTICE_FIRST` 用户更容易先进入练习
- `CORE_CONTRAST_FIRST` 用户更容易先看到易混点对比

#### 4）任务数量会变

`TimeBudgetEnforcer` 会根据时间预算和入口粒度压缩或保留任务数。

也就是说，同一主题下：

- 15 分钟用户看到的是压缩版路径
- 长周期用户看到的是更完整的路径

## 4. 前端规划页如何把这些结果“呈现成个性化”

关键代码：

- [frontend/src/views/LearningPlanDecisionView.vue](/D:/Panda_Code/AI_Learning_Workflow_Navigator/frontend/src/views/LearningPlanDecisionView.vue)
- [frontend/src/utils/learningPlanDecisionModel.ts](/D:/Panda_Code/AI_Learning_Workflow_Navigator/frontend/src/utils/learningPlanDecisionModel.ts)

前端并没有再次“重新做决策”，而是把后端结构化结果转译成更强感知的页面模块：

- `PlanDecisionHero`
- `PlanReasonBlock`
- `PlanCausalChain`
- `PlanFirstTaskCard`
- `PlanPathPreview`
- `PlanContrastBlock`

前端个性化主要做了 3 件事：

### 4.1 把标签翻译成用户能看懂的话

例如 blocker tag、risk tag、strategy code，不直接原样显示，而是映射成：

- 当前卡点描述
- 当前状态短标签
- 为什么先做这一步
- 如果走错路径会怎样

这部分主要在 `learningPlanDecisionModel.ts` 里完成。

### 4.2 根据主题包补充更像“该主题专属”的表达

前端会通过 `packId / knowledgePack` 决定一些主题化展示文案，使不同知识点的规划页不完全一样。

也就是说，规划页现在的个性化包含两层：

- 通用规则个性化
- 知识包驱动的主题化展示

### 4.3 把后端证据重新组织成“因果链”

页面不只是告诉用户“给你这个计划”，还会组织成：

- 你现在处于什么状态
- 主要卡点是什么
- 所以先做什么
- 这样做后面能获得什么
- 如果直接走错路会怎样

这一层提升的是“被定制感”和“被理解感”。

## 5. 当前系统规划页个性化的真实边界

从代码看，当前个性化已经具备，但边界也很明确。

### 5.1 它是规则驱动，不是开放式自适应规划

优点：

- 稳定
- 可控
- 可测试
- 容易解释原因

限制：

- 个性化颗粒度还不够细
- 很依赖预设标签和模板
- 面对非常复杂的复合目标时，策略仍较粗

### 5.2 诊断题目前还是固定题库

`DiagnosisQuestionBank` 里已经写了扩展点说明，但现在仍然是固定 6 题，没有针对不同主题动态生成更细的问题。

这会限制个性化深度。

### 5.3 规划结果更多是“策略个性化”，还不是“内容级个性化”

当前主要变化的是：

- 先学什么
- 先做什么类型任务
- 任务多少
- 展示怎么说

但还没有深到：

- 同一任务内部依据用户真实错误历史改写内容
- 根据历史会话持续调整策略
- 基于长期学习记录做跨 session 个性化

## 6. 一句话总结

当前系统规划页面的个性化，本质上是：

**用目标信息和诊断画像生成结构化规划上下文，再通过规则选择策略、模板生成任务路径，最后由前端把这些决策翻译成用户可感知的专属规划说明。**

它已经不是“通用聊天回答”，而是一个有约束、有证据、有解释链条的学习规划决策页。

## 7. 文档输出位置

本文档已输出到：

- [doc/system-planning-personalization.md](/D:/Panda_Code/AI_Learning_Workflow_Navigator/doc/system-planning-personalization.md)

