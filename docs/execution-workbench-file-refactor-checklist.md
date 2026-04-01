# 执行页文件改造清单

## 主路径文件职责

- `frontend/src/views/TaskRunView.vue`
  - 保留路由入口身份
  - 负责：数据拉取、phase query 同步、挂载工作台页
  - 不负责：阶段布局、子组件细节交互

- `frontend/src/components/task-run/ExecutionWorkbenchPage.vue`
  - 负责整页骨架编排（顶栏/双栏/反馈位）
  - 不负责业务判定与阶段跳转逻辑

- `frontend/src/components/task-run/PhaseInteractionHost.vue`
  - 负责 `phase -> 子组件` 映射
  - 不负责页面布局与路由切换

- `frontend/src/components/task-run/PromptScaffoldPanel.vue`
  - 负责认知动作列表与 `append-explanation` 事件
  - 不负责输入框写入与主交互流程

## 阶段子组件

- `DfsBfsStructureWorkbench.vue`：结构辨析选择题
- `UnderstandingMcqWorkbench.vue`：机制理解选择题
- `TrainingExpressionWorkbench.vue`：单任务表达输入
- `SystemSummaryCard.vue` / `ReflectionQuestionCard.vue` / `ReflectionStrategyCard.vue`：反思收口三卡

## 类型与常量层

- `frontend/src/types/phaseWorkbench.ts`
  - 定义 `renderState`、阶段 intro、结构题模型、工作台 VM
- `frontend/src/constants/phaseWorkbenchDfsBfs.ts`
  - 固定四阶段文案、默认 explanation、结构题/机制题、动作库、下一步文案

## 旧组件迁移策略

- `MainTaskWorkbenchCard.vue`
  - 退出执行页主结构路径（仅保留为兼容模块）
- `TaskExpressionPanel.vue`
  - 仅允许在 training 阶段内部复用（不再全局通用）
- `ScaffoldPromptPanel.vue`
  - 语义由 Prompt 面板迁移为认知动作库（主路径使用 `PromptScaffoldPanel.vue`）
- `TaskFeedbackDeck.vue`
  - 降级兼容，主路径统一用 `PhaseFeedbackCard.vue`
