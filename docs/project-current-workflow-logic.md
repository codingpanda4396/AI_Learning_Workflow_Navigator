# 项目当前链路逻辑总结

本文基于当前仓库实现整理项目主链路，目标是快速说明：

- 产品现在是如何从目标输入推进到学习报告的
- 前端页面、路由和状态是如何串起来的
- 后端每个阶段分别承担什么职责
- 执行页为什么会分成“通用执行流”和“脚手架执行流”两套模式

## 1. 项目主链路

当前项目已经形成一条比较清晰的学习闭环：

`目标输入 -> 诊断 -> 学习规划 -> 任务执行 -> 学习报告 -> 下一步建议`

这条链路不是通用聊天，而是明确的流程型学习产品。核心标识有两个：

- 每一页都围绕“当前处于哪一步、现在该做什么、下一步是什么”展开
- 整个流程依赖一组逐步生成的业务 ID 串联，而不是单页临时状态

主链路关键 ID 如下：

- `goalId`：目标创建完成后得到
- `diagnosisId`：诊断会话创建后得到
- `planId`：规划预览生成后得到
- `sessionId`：规划提交、正式开始学习后得到
- `taskId`：当前执行中的任务 ID

## 2. 前端链路结构

### 2.1 路由顺序

前端主路由在 [frontend/src/router/index.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\router\index.ts) 中定义，当前主流程为：

- `/goal`
- `/diagnosis`
- `/plan`
- `/task` 或 `/tasks/:taskId/run`
- `/report`

同时存在两个关键守卫逻辑：

- 未登录用户不能进入 `diagnosis / plan / task / report`
- 没有 `goalId / diagnosisId / sessionId` 时不能跳过前置步骤

这意味着当前前端不是“任意页面独立请求”，而是强依赖流程上下文的串行导航。

### 2.2 工作流状态中心

全局链路状态集中在 [frontend/src/stores/workflow.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\stores\workflow.ts)。

它承担两层职责：

- 保存流程关键 ID：`goalId / diagnosisId / planId / sessionId`
- 保存各阶段关键结果：`structuredGoal / learnerProfileSnapshot / planPreview / currentTask / report`

其中 `goalId / diagnosisId / planId / sessionId / structuredGoal` 会写入 `sessionStorage`，所以刷新页面后主链路还能续上。

## 3. 各阶段当前逻辑

### 3.1 目标输入阶段

前端入口是 [frontend/src/views/GoalInputView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\GoalInputView.vue)。

当前不是自由文本大表单，而是“学科 -> 主题”的半结构化快速启动：

- 用户先选学科
- 再选主题
- 前端用 `buildHomeGoalRequest` 组装目标请求
- 调用 `createGoal`
- 成功后写入 `goalId / structuredGoal / goalContextSnapshot`
- 跳转到诊断页

后端对应服务是 [backend/src/main/java/navigator/application/GoalApplicationService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\GoalApplicationService.java)。

它做了三件事：

- 用 `GoalRuleEngine` 把输入转成结构化目标
- 用 `GoalContextDeriver` 推导目标上下文
- 落库 `learning_goal`，并回填 `goalId`

所以目标阶段的输出不只是“用户想学什么”，而是已经变成后续诊断与规划可消费的结构化对象。

### 3.2 诊断阶段

前端页面是 [frontend/src/views/DiagnosisView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\DiagnosisView.vue)。

当前交互分成两步：

1. 进入页面先调用 `createSession(goalId)`
2. 用户完成 3 个快速诊断问题后调用 `submitDiagnosis`

前端产出：

- `diagnosisId`
- `sessionId`
- `learnerProfileSnapshot`
- `diagnosisEvidenceSummary`

这里有一个值得注意的实现细节：

- `sessionId` 并不是在提交计划时才创建
- 当前项目在诊断会话创建时就已经创建了学习 session

后端对应 [backend/src/main/java/navigator/application/DiagnosisApplicationService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\DiagnosisApplicationService.java)，主要负责：

- 基于目标生成结构化诊断题
- 归一化用户答案
- 生成 `LearnerProfileSnapshot`
- 生成 `DiagnosisEvidenceSummary`
- 派生 `LearnerStrategyProfile`
- 将诊断状态从 `READY` 推进到 `COMPLETED`

因此，诊断阶段本质上是在回答两个问题：

- 用户当前基础和阻塞点是什么
- 接下来适合用什么学习策略推进

### 3.3 规划阶段

前端页面是 [frontend/src/views/LearningPlanDecisionView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\LearningPlanDecisionView.vue)。

这里当前是一个“决策解释页”，而不是传统任务列表页。流程为：

1. 调用 `previewPlan(goalId, diagnosisId)`
2. 展示推荐入口、推荐策略、阶段路径、首个任务
3. 用户点击开始后调用 `commitPlan(planId)`
4. 写入 `sessionId / currentTaskId / taskSequence`
5. 跳转到任务执行页

后端对应 [backend/src/main/java/navigator/application/PlanningApplicationService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\PlanningApplicationService.java)。

它的职责可以概括为：

- 组装规划上下文 `PlanningContext`
- 用规则引擎选策略 `PlanStrategySelector`
- 用模板工厂生成阶段与任务 `PlanTemplateFactory`
- 持久化计划预览
- 在 `commit` 时把任务序列真正落到 session 上
- 生成每个任务的 `ExecutableTaskSpec`

这里的关键点是：

- `preview` 阶段负责解释“为什么这样规划”
- `commit` 阶段负责把规划转成真实可执行 session

### 3.4 执行阶段

执行阶段是当前项目最复杂、也最接近产品核心的一段。

前端主页面是 [frontend/src/views/TaskRunView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\TaskRunView.vue)。

进入执行页后，前端先调用：

- `getCurrentTask(sessionId)`：拿当前任务
- `getTaskScaffold(taskId, sessionId)`：拿任务脚手架和运行时状态
- `getCurrentTaskGuidance(sessionId)`：拿当前阶段引导

随后执行流会分成两条模式。

#### 模式 A：通用执行流

这是较早的一套任务执行状态机，核心由 [backend/src/main/java/navigator/application/task/TaskExecutionFlowService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\task\TaskExecutionFlowService.java) 负责。

运行状态大致是：

`ORIENT -> EXPLORE -> SELF_EXPLAIN / REMEDIAL -> CHECK -> PASS`

主要接口动作包括：

- `postTaskMessage`：探索式对话推进
- `postSelfExplanation`：用户自解释
- `postCheckpoint`：微检查
- `completeTask`：收束并完成任务

这条链路的核心思想不是“问答”，而是把用户行为识别成学习动作，再据此推进状态和反馈：

- 识别用户是在要解释、要例子、求简化，还是直接要答案
- 依据阶段引擎输出当前引导块和推荐动作
- 把探索、自解释、检查过程中的证据持续写入 runtime

#### 模式 B：学习脚手架执行流

这是当前任务页更强调的展示性实现，后端由 [backend/src/main/java/navigator/application/scaffold/LearningScaffoldEngineService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\scaffold\LearningScaffoldEngineService.java) 驱动。

这套模式只在特定知识包下启用，目前明显围绕 `DFS/BFS` 这类主题做了深度定制。

其阶段结构是：

`STRUCTURE -> UNDERSTANDING -> TRAINING -> REFLECTION`

每个阶段都有明确动作卡、验证器和反馈器：

- `STRUCTURE`：搭骨架，先明确知识点位置、前置、后续连接
- `UNDERSTANDING`：做机制判断
- `TRAINING`：用户用自己的话表达
- `REFLECTION`：记录迁移策略与反思结论

前端通过 `useLearningScaffoldEngine` 驱动这套流程，阶段反馈会直接映射到工作台 UI，而不是回落成普通聊天记录。

这说明当前执行页实际上是“双层结构”：

- 底层仍然有任务 runtime、状态持久化、任务完成逻辑
- 上层针对特定知识主题套了一个更强约束、更可展示的脚手架工作台

### 3.5 报告阶段

前端页面是 [frontend/src/views/ReportView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\ReportView.vue)。

进入页面后：

1. 调用 `getReport(sessionId)`
2. 展示学习结果、完成进度、未解决问题、证据总结、方法画像
3. 展示系统建议的下一步动作
4. 用户确认后调用 `confirmNextAction(sessionId, actionType)`

后端对应 [backend/src/main/java/navigator/application/ReportApplicationService.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\ReportApplicationService.java)。

它主要做四件事：

- 汇总 session 级证据
- 聚合各任务的学习方法画像
- 判断本轮结果是 `ACHIEVED / PARTIALLY_ACHIEVED / NOT_ACHIEVED`
- 给出下一步动作建议，如 `CONTINUE / REINFORCE / CHANGE_STRATEGY`

所以报告页不是静态总结，而是当前链路里的闭环出口，用来决定是否继续、巩固还是重规划。

## 4. 当前执行链路的核心设计

### 4.1 主链路是“流程驱动”，不是“消息驱动”

虽然执行页里有消息和 AI 反馈，但项目真正的主结构仍然是流程型产品：

- 先建立目标
- 再诊断当前状态
- 再生成计划
- 再执行任务
- 最后再评价和调整

聊天只是执行阶段的一个交互壳，不是产品本体。

### 4.2 当前任务页是项目核心 showcase

从前端组件规模和后端服务拆分可以看出，任务执行页是当前仓库最重的模块。

它同时承载：

- 当前任务展示
- 阶段进度提示
- 用户表达工作区
- 系统反馈
- 脚手架动作卡
- 反思与收束

这与仓库里的产品约定一致：任务执行页必须让用户感觉自己处在一个真实的引导式学习系统中，而不是聊天窗口。

### 4.3 当前系统依赖“证据累积”

项目不是只看用户有没有点“完成”，而是在多个阶段持续收集证据：

- 诊断答案
- 探索轮次
- 自解释质量
- 检查题结果
- 完成收束文本
- 互动次数和方法画像

这些证据后续会被用于：

- 生成报告
- 生成下一步建议
- 判断是否需要重规划

## 5. 当前项目的隐含分层

按实际代码结构，项目当前可以理解为 4 层：

### 5.1 页面与展示层

负责把每一步渲染成明确的工作流页面，核心集中在：

- `frontend/src/views`
- `frontend/src/components`

### 5.2 前端流程编排层

负责跨页流转、状态保存、接口调用，核心集中在：

- `frontend/src/router`
- `frontend/src/stores`
- `frontend/src/api`
- `frontend/src/composables`

### 5.3 后端应用服务层

负责按业务阶段提供稳定入口，核心服务包括：

- `GoalApplicationService`
- `DiagnosisApplicationService`
- `PlanningApplicationService`
- `ExecutionApplicationService`
- `ReportApplicationService`

### 5.4 执行期引导与脚手架层

负责学习过程中的动态引导、状态机推进和证据累积，核心集中在：

- `navigator.application.task.*`
- `navigator.application.task.guidance.*`
- `navigator.application.scaffold.*`

这一层是当前项目与普通 CRUD 学习平台差异最大的地方。

## 6. 当前链路的简化结论

如果只用一句话总结当前项目逻辑，可以概括为：

> 系统先把“我要学什么”结构化，再判断“我现在卡在哪”，然后给出“当前最适合的进入方式”，接着在任务执行中持续用脚手架和反馈推动用户表达、检查、反思，最后根据证据生成报告和下一步动作。

如果再压缩成工程视角，则是：

- `Goal` 负责定义学习目标
- `Diagnosis` 负责识别学习状态
- `Plan` 负责决定进入路径
- `Execution` 负责推进真实学习动作
- `Report` 负责总结证据并决定后续方向

## 7. 当前值得团队注意的点

### 7.1 `sessionId` 的创建时间比直觉更早

当前 `sessionId` 在诊断会话创建时就存在，不是等计划提交后才存在。计划提交只是让这个 session 绑定 plan 并进入正式执行。

### 7.2 执行页同时存在两套推进机制

当前执行链路不是单一机制，而是：

- 通用任务状态机
- 特定知识包的脚手架引擎

后续如果继续扩主题，需要明确是复用通用状态机，还是继续扩展脚手架引擎。

### 7.3 报告页已经在做“下一步决策”

当前报告页不是终点页，它已经承担了闭环里的“继续 / 巩固 / 调整策略”判断，因此后续二次规划功能很自然可以接在这里继续扩。

## 8. 推荐作为后续文档基线的理解方式

后续讨论项目时，建议统一按下面这套语言描述：

- 目标阶段：把模糊目标结构化
- 诊断阶段：识别基础、阻塞点和节奏
- 规划阶段：决定当前最适合的学习起点
- 执行阶段：通过任务脚手架推进真实学习动作
- 报告阶段：依据证据总结并决定下一步

这样能避免把项目误解成“带规划页的聊天产品”。
