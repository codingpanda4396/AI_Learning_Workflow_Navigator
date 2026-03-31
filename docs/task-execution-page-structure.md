# 任务执行页结构逻辑总结

本文总结当前项目中“任务执行页”的实际结构组织方式，重点描述它现在是如何把学习任务、脚手架、用户表达和反馈串成一个完整执行闭环的。

## 1. 页面定位

任务执行页不是聊天页，也不是普通表单页，而是学习工作流中的核心执行场：

- 上游承接：计划预览页提交后的 `sessionId`
- 当前职责：让用户围绕“本轮唯一任务”完成一次明确表达、获得反馈、继续推进或收口
- 下游流向：当前任务完成后进入下一任务，或跳到报告页

这页的核心产品逻辑不是“自由交流”，而是：

**阶段头部 -> 当前唯一任务 -> 表达区 -> 反馈区 -> 底部动作条**

也就是用固定骨架承载不同阶段的学习动作，而不是每一轮都重新拼装页面。

## 2. 路由与入口逻辑

当前任务执行相关入口有 3 个：

- `/execution`
  - 仅作过渡入口，`ExecutionView.vue` 不承载主执行 UI
- `/task`
  - 任务执行主入口
- `/tasks/:taskId/run`
  - 带任务实例 id 的稳定执行地址

路由守卫的约束是：

- 没有登录，不能进入执行阶段
- 没有 `sessionId`，不能进入任务执行页，会被打回计划页
- 如果已知 `currentTaskId`，访问 `/task` 会自动跳转到 `/tasks/:taskId/run`
- `/execution` 会进一步跳转到 `/task`

因此，真正的任务执行页面入口是 `frontend/src/views/TaskRunView.vue`。

## 3. 页面整体分层

当前任务执行页可以理解为 4 层：

1. 路由与会话入口层
  决定用户能不能进入执行页，以及当前对应哪个 `taskId`
2. 数据与状态层
  从 API 拉当前任务、脚手架、引导状态，并维护页面本地交互状态
3. 视图模型组装层
  `buildExecutionPageModel.ts` 把后端原始数据加工成前端可直接渲染的页面模型
4. 渲染与交互层
  `TaskRunView.vue` 根据页面模型选择具体组件树，并把操作回写到 API

这 4 层的好处是：任务执行页虽然状态复杂，但页面组件大多只消费“整理后的模型”，不直接承受大量业务判断。

## 4. 状态来源：页面依赖哪些数据

任务执行页依赖两类状态。

### 4.1 全局工作流状态

来自 `useWorkflowStore`：

- `sessionId`
- `currentTaskId`
- `currentTask`
- `progress`
- `planPreview`
- `structuredGoal`

这里的职责是保存整个学习闭环里的关键业务 id，让任务执行页知道自己属于哪一次学习 session、当前处于哪一个任务。

### 4.2 页面本地运行时状态

主要在 `TaskRunView.vue` 内维护：

- 页面加载态：`loading`、`error`
- 当前任务数据：`task`、`progress`、`scaffold`
- 执行状态机：`taskState`
- 轮次状态：`exploreRoundCount`
- 当前检查题：`checkpointQuestion`
- 用户输入：`draftInput`、`structuredInputs`
- 最近反馈：`latestFeedback`
- 微检查：`microChecks`
- DFS/BFS 专用工作台状态：`structureWorkbenchUi`
- 收口字段：`closureSummary`、`closurePoint1`、`closurePoint2`、`closureNext`

也就是说，全局 store 只管“流程身份”，页面本地状态才真正承载“这一轮任务是怎么被做出来的”。

## 5. 数据获取链路

页面 mounted 后，主链路是：

1. `fetchTask()`
2. 调 `getCurrentTask(sessionId)`
3. 拿到 `currentTask + progress`
4. 再调用 `loadScaffold(taskId)`
5. 调 `getTaskScaffold(taskId, sessionId)`
6. 同步当前 runtime 信息
7. 调 `getCurrentTaskGuidance(sessionId)` 补引导文案和建议动作

对应的 API 角色如下：

- `GET /api/sessions/{sessionId}/current-task`
  - 确定“现在要做哪一个任务”
- `GET /api/tasks/{taskId}/scaffold`
  - 给当前任务注入执行脚手架、阶段配置、表达布局、反馈 schema 等
- `GET /api/sessions/{sessionId}/current-task-guidance`
  - 给当前任务补充当前引导语和推荐动作

后续用户操作再根据 `taskState` 分发到不同接口：

- 普通表达：`postTaskMessage`
- 自解释：`postSelfExplanation`
- 检查题：`postCheckpoint`
- 完成任务：`completeTask`

因此，这个页面不是一次性拉完数据后静态渲染，而是一个“边执行边刷新阶段状态”的运行时页面。

## 6. 核心结构：TaskRunView 如何组织页面

`TaskRunView.vue` 是真正的编排器。它的模板结构很清晰：

- 外层：`PageContainer + AppTopBar`
- 中间：根据 `loading / error / empty / task` 决定页面主状态
- 成功态：进入任务执行主结构
- 底部：始终由 `TaskRunDualActionBar` 承接主次操作

成功态下的主结构分为两种。

### 6.1 默认主链路

默认情况下，页面结构是：

- `TaskRunPhaseHeader`
- `MainTaskWorkbenchCard`
- `TaskExpressionPanel`
- `TaskFeedbackDeck`
- `ReflectionSummaryCard`（有总结时显示）
- `TaskRunDualActionBar`

这条链路体现的是单主线执行逻辑：

- 先告诉用户“当前在哪个阶段”
- 再告诉用户“当前唯一任务是什么”
- 再让用户写
- 写完后给反馈
- 然后用底部动作条推进

### 6.2 DFS/BFS 的专用分支

当满足下面条件时，会切换到专用工作台：

- `packId === 'ds_dfs_bfs'`
- 还没有进入 legacy complete
- `dfsBfsStructureFlowActive === true`

此时页面不走默认表达区，而改成：

- `DfsBfsStructurePageHeader`
- `DfsBfsStructureWorkbench`
- `TaskRunDualActionBar`

也就是说，DFS/BFS 在 STRUCTURE 阶段被单独做成了“结构化骨架工作台”，优先强调搭骨架，而不是让用户直接进入通用文本表达模式。

这是当前执行页里最明显的一条“主题知识点特化分支”。

## 7. 视图模型层：为什么要 buildExecutionPageModel

`buildExecutionPageModel.ts` 是任务执行页的关键中间层。

它的职责不是简单映射字段，而是把多源数据重新组装成统一页面语义：

- 当前处于哪个学习阶段
- 当前这一轮应该让用户做什么
- 页面头部应该显示什么
- 当前输出物是什么
- 用户卡住时有哪些动作
- 当前反馈应该如何展示
- 当前工作台应该突出什么

它把这些内容统一整理到 `ExecutionPageViewModel` 里，主要包含：

- `header`
- `mainAction`
- `feedback`
- `progressRail`
- `helpSections`
- `scaffoldCards`
- `tutorConsole`
- `workbench`

其中真正服务当前页面主结构的是 `workbench`。

`workbench` 又进一步统一了：

- `phaseProgress`
- `scaffoldProduct`
- `whyThisStep`
- `stageRules`
- `topicHints`
- `stageMini`
- `currentTask`
- `guideSections`
- `expressionLayout`
- `feedbackSchema`
- `tutorAssist`
- `hintReveal`
- `emphasisPhase`

可以把它理解成：**后端返回的是任务 runtime，前端真正渲染的是 workbench model。**

## 8. 当前页面的核心逻辑不是“聊天”，而是“阶段驱动”

虽然执行页里存在消息发送接口，但当前页面的主逻辑是被 `taskState` 驱动的。

主要状态有：

- `ORIENT`
- `EXPLORE`
- `SELF_EXPLAIN`
- `CHECK`
- `REMEDIAL`
- `PASS`

这些状态并不会直接对应不同页面，而是会影响以下内容：

- 页面阶段文案
- 主按钮文案
- 输入区 placeholder
- 表达区是否切成结构化字段
- 反馈区显示什么反馈模型
- 是否出现“进入下一阶段”的微检查
- 当前交互该调用哪个 API

也就是说，当前任务执行页是“同一骨架 + 状态切相位”，不是“每个阶段一个独立页面”。

## 9. 四个最关键的 UI 模块分别负责什么

### 9.1 `TaskRunPhaseHeader`

职责：

- 告诉用户当前在哪个阶段
- 告诉用户这一阶段的目标线索
- 告诉用户当前任务进度

它负责“定位感”，解决“我现在在哪”的问题。

### 9.2 `MainTaskWorkbenchCard`

职责：

- 聚焦本轮唯一任务
- 解释为什么现在先做这个
- 明确本轮交付物
- 给出一个最短起步动作

它负责“现在做什么”，是当前执行页首屏最核心的产品卡片。

### 9.3 `TaskExpressionPanel`

职责：

- 承接用户当前表达
- 根据阶段决定是文本输入还是结构化字段
- 提供 starter chips 降低输入门槛
- 在可推进时展示微检查，决定是否允许进入下一阶段

它负责“把任务真正做出来”。

### 9.4 `TaskFeedbackDeck`

职责：

- 显示当前轮反馈
- 区分说对了什么、漏了什么、混淆了什么、下一步修什么
- 提供反馈动作按钮，如“按建议补一句”“重新表达”“看例子”

它负责“用户提交后怎么继续往前推”。

## 10. 底部动作条为什么是双按钮

`TaskRunDualActionBar` 固定承接两个动作：

- 主动作：提交 / 进入下一阶段 / 完成任务
- 次动作：我还没想清楚 / 给我提示

这样做的结构意义是：

- 页面始终只有一个主推进方向
- 同时保留一个低压力求助出口
- 用户无需在多个卡片里找下一步按钮

因此它不是简单的 footer，而是任务执行页的统一推进控制器。

## 11. 页面里的两条重要分支逻辑

### 11.1 通用执行分支

用户在默认主链路里：

- 看任务
- 写表达
- 获得反馈
- 继续补写 / 自解释 / 过检查 / 收口

这是大多数任务的通用结构。

### 11.2 专题工作台分支

DFS/BFS 结构阶段会进入专用工作台。

这说明当前项目已经形成一个重要模式：

- 默认知识点走统一执行骨架
- 特定知识点可以在某个阶段插入“专用工作台”
- 但仍然挂在同一条任务执行主流程下

这对后续扩展别的知识点专用工作台很重要。

## 12. 当前结构的真实设计意图

从代码来看，当前任务执行页的结构逻辑可以概括为一句话：

**用一个稳定的执行骨架，承载不同阶段的学习动作；必要时允许知识点在局部阶段切入专用工作台。**

再展开一点，就是：

- 路由层保证用户一定是带着 session 进入任务执行
- 页面层只认“当前任务”
- 模型层把复杂状态压缩成稳定的工作台模型
- 组件层围绕“当前唯一任务”来组织首屏
- 状态机决定当前是表达、纠错、检查还是收口
- 专题知识点可以在不打断主链路的前提下切入专用 UI

## 13. 对当前结构的简短结论

当前任务执行页已经不是“聊天壳”，而是一个明确的学习执行工作台。

它的结构特点是：

- 单页单主线
- 当前任务首屏优先
- 阶段状态驱动内容变化
- 页面模型统一组装
- 反馈与推进动作闭环
- 支持知识点级专用工作台插槽

如果后续继续优化，这一页最值得持续强化的不是“再加模块”，而是继续提升：

- 各阶段的视觉差异
- 首屏任务压强
- 专用工作台与通用工作台的切换节奏
- 反馈到下一动作的闭环效率

