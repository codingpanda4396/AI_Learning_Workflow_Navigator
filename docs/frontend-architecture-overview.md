# 前端梳理

## 1. 前端定位

当前前端不是通用聊天界面，而是一个按学习闭环推进的单页应用，核心目标是把用户从 `目标 -> 诊断 -> 规划 -> 执行 -> 报告` 一步步带到下一个动作。

技术栈：

- Vue 3
- Pinia
- Vue Router
- Axios
- Tailwind CSS
- TypeScript

入口文件为 `frontend/src/main.ts`，应用壳在 `frontend/src/App.vue`。

## 2. 页面与路由主线

路由定义在 `frontend/src/router/index.ts`，主流程非常明确：

- `/goal`
  目标输入页，对应 `GoalInputView.vue`
- `/diagnosis`
  诊断页，对应 `DiagnosisView.vue`
- `/plan`
  学习规划页，对应 `LearningPlanView.vue`
- `/execution`
  旧执行页入口，会重定向到任务页
- `/tasks/:taskId/run`
  当前核心任务执行页，对应 `TaskRunView.vue`
- `/report`
  报告页，对应 `ReportView.vue`
- `/auth/login` / `/auth/register`
  登录注册页

路由守卫承担了流程控制职责：

- 未登录用户不能进入 `diagnosis/plan/task/report`
- 没有 `goalId` 不能进入诊断
- 没有 `diagnosisId` 不能进入规划
- 没有 `sessionId` 不能进入任务和报告
- 当前任务存在时，`/task` 会自动跳到 `/tasks/:taskId/run`

这意味着前端的页面切换不是自由跳转，而是强依赖后端返回的阶段性 ID。

## 3. 状态管理

### 3.1 业务主 store

`frontend/src/stores/workflow.ts` 是流程主状态中心，维护了整条学习链路上的关键数据：

- `goalId`
- `structuredGoal`
- `goalContextSnapshot`
- `diagnosisId`
- `learnerProfileSnapshot`
- `diagnosisEvidenceSummary`
- `planId`
- `planPreview`
- `sessionId`
- `currentTaskId`
- `taskSequence`
- `currentTask`
- `progress`
- `report`
- `nextActionDecision`

其中 `goalId / diagnosisId / planId / sessionId / structuredGoal` 会写入 `sessionStorage`，用于刷新后保持流程上下文。

### 3.2 认证 store

`frontend/src/stores/auth.ts` 负责：

- 当前用户信息
- 最近一次学习入口 `recentLearningEntry`
- 登录态初始化
- 登录后待跳转地址

首页右侧“继续上次学习”区域就是基于这个 store 构建的。

### 3.3 AI 导师 store

`frontend/src/stores/aiTutor.ts` 负责独立的 AI 导师对话面板，包括：

- 对话消息
- 当前知识点上下文
- 流式输出
- 反馈与提示消息

这个能力是执行页的辅助层，不是主流程本身。

## 4. 页面职责拆解

### 4.1 目标页 `GoalInputView.vue`

职责：

- 让用户快速选学科与主题
- 构造目标创建请求
- 调用 `createGoal`
- 将 `goalId / structuredGoal / goalContextSnapshot` 写入 workflow store
- 成功后跳转诊断页

特点：

- 当前更偏“预设主题驱动”，不是完全自由文本输入
- 强调低认知负担和快速起步
- 支持“继续上次学习”

### 4.2 诊断页 `DiagnosisView.vue`

职责：

- 通过 `createSession(goalId)` 创建诊断会话和学习会话
- 展示轻量化诊断问题
- 将用户回答映射成后端需要的诊断答案
- 调用 `submitDiagnosis`
- 写入 `diagnosisId / sessionId / learnerProfileSnapshot / diagnosisEvidenceSummary`

特点：

- 页面是“短诊断”，不是长问卷
- 对特定知识包可走专题题目映射
- 结果直接服务于后续规划

### 4.3 规划页 `LearningPlanView.vue`

职责：

- 调用 `previewPlan(goalId, diagnosisId)` 拉取计划预览
- 展示推荐策略、阶段面板、时间线
- 点击开始时调用 `commitPlan(planId)`
- 将 session 推进到可执行状态

特点：

- 规划页的核心不是展示很多内容，而是告诉用户“为什么从这里开始”
- `buildPlanActionPanelView` 把后端计划数据转成 UI 表达模型

### 4.4 任务执行页 `TaskRunView.vue`

这是当前前端最核心、最复杂的页面。

它承担三层职责：

- 展示当前任务与进度
- 驱动任务执行状态机
- 承接专题脚手架引擎

普通任务流主要依赖这些接口：

- `getCurrentTask`
- `getCurrentTaskGuidance`
- `getTaskScaffold`
- `postTaskMessage`
- `postSelfExplanation`
- `postCheckpoint`
- `completeTask`

页面内部根据任务状态切换不同交互模式：

- `ORIENT / EXPLORE`
  用户表达、问问题、接收引导
- `SELF_EXPLAIN / REMEDIAL`
  自我解释与补救
- `CHECK`
  微检查
- `PASS`
  准备收束并完成任务

针对 DFS/BFS 知识包，还叠加了脚手架引擎 UI：

- `useLearningScaffoldEngine`
- `useStructureSkeletonFlow`

对应的专用组件集中在 `frontend/src/components/task-run/`，其中结构化学习卡片、骨架面板、反馈条、底部动作条构成了“学习脚手架工作台”。

### 4.5 报告页 `ReportView.vue`

职责：

- 调用 `getReport(sessionId)` 获取学习报告
- 展示结果、证据、未解决问题、学习方法画像
- 调用 `confirmNextAction` 确认下一步

它不是单纯“结果页”，而是闭环中的“决策页”。

## 5. 前端组件结构

组件大致可分四层：

- 布局层：`components/layout`
- 通用 UI 层：`components/ui`
- 页面业务层：`components/plan`、`components/task-run`、`components/workflow`
- AI 导师层：`components/ai-tutor`

当前项目里最重的业务组件群在：

- `components/task-run`
- `components/plan`

说明产品重点已经明显转向“任务执行展示”和“规划表达”，而不是首页营销或聊天组件。

## 6. 接口组织方式

前端 API 按领域拆分在 `frontend/src/api/`：

- `auth.ts`
- `goals.ts`
- `diagnosis.ts`
- `learning-plan.ts`
- `task.ts`
- `learningScaffold.ts`
- `session.ts`
- `tutor.ts`

这套拆分基本与后端控制器一一对应，利于按业务阶段理解与维护。

## 7. 关键表达模型与工具层

前端并不直接把后端 DTO 生硬渲染出来，而是通过一批工具函数做“面向页面的重组”：

- `buildExecutionPageModel.ts`
- `planPresentationModel.ts`
- `buildCompleteTaskPayload.ts`
- `taskGuidedSteps.ts`
- `diagnosisSubmitMapper.ts`
- `workflowTopicLabel.ts`

这说明前端已经形成一层“表现模型层”，它的作用是把后端业务对象翻译成页面需要的视图模型。

## 8. 当前前端架构特点

可以概括为四点：

- 它是流程驱动前端，不是信息聚合前端
- 它以 store 中的阶段 ID 作为流程锚点
- 执行页已经是产品展示中心
- 专题知识包能力正在把执行页从通用任务页推进到“脚手架工作台”

## 9. 当前值得继续关注的前端点

- `TaskRunView.vue` 体量很大，后续可继续拆分状态与动作逻辑
- 旧执行页 `/execution` 仍保留兼容语义，但主入口已转向 `/tasks/:taskId/run`
- AI 导师是辅助能力，不能继续侵占主任务区
- 计划页和报告页已经较稳定，后续迭代重点更可能继续落在任务执行页与知识包扩展上
