# Store Analysis

## 1. 当前所有 store

- `authStore` -> `frontend/src/stores/auth.ts`
- `diagnosisStore` -> `frontend/src/stores/diagnosis.ts`
- `feedbackStore` -> `frontend/src/stores/feedback.ts`
- `learningPlanStore` -> `frontend/src/stores/learningPlan.ts`
- `quizStore` -> `frontend/src/stores/quiz.ts`
- `sessionStore` -> `frontend/src/stores/session.ts`
- `taskStore` -> `frontend/src/stores/task.ts`

## 2. 每个 store 的职责

### `authStore`

- 处理登录、注册、登出
- 从本地存储恢复登录态
- 保存当前用户名与 token

### `diagnosisStore`

- 生成诊断问卷
- 管理诊断答题进度与答案
- 提交诊断结果并保存能力画像、下一步动作

### `feedbackStore`

- 获取学习报告
- 提交报告页的下一步动作
- 获取成长看板

### `learningPlanStore`

- 生成学习路径预览
- 保存规划请求上下文与调整项
- 重新生成规划、确认规划并创建正式 session

### `quizStore`

- 生成训练题
- 轮询训练题生成状态
- 获取题目详情并提交答案

### `sessionStore`

- 创建学习 session
- 获取首页“当前学习会话”
- 获取 session 总览
- 记录最近一次 sessionId

### `taskStore`

- 获取任务详情
- 执行任务
- 保存当前任务执行结果

## 3. store 中的 state 字段

### `authStore`

- `token`
- `username`
- `loading`
- `error`

### `diagnosisStore`

- `diagnosisId`
- `sessionId`
- `questions`
- `currentQuestionIndex`
- `answers`
- `capabilityProfile`
- `nextAction`
- `loading`
- `submitting`
- `error`

### `feedbackStore`

- `report`
- `growthDashboard`
- `loading`
- `error`

### `learningPlanStore`

- `preview`
- `request`
- `adjustments`
- `loading`
- `regenerating`
- `confirming`
- `error`

### `quizStore`

- `quiz`
- `status`
- `loading`
- `submitting`
- `error`

### `sessionStore`

- `currentSessionId`
- `overview`
- `currentSession`
- `loading`
- `error`

### `taskStore`

- `currentTaskDetail`
- `currentTaskResult`
- `loading`
- `error`

## 4. 哪些 store 职责重叠

### 明显重叠

- `sessionStore` 与 `learningPlanStore`
  - 都在承载“学习流程上下文 / session 上下文”
  - `learningPlanStore.request.sessionId` 与 `sessionStore.currentSessionId` 都在表达当前流程关联的 session

- `sessionStore` 与 `taskStore`
  - `sessionStore.overview.nextTask` 提供“当前该做什么”
  - `taskStore` 再保存“当前任务详情 / 执行结果”
  - 两者都在描述当前学习流程的执行节点

- `sessionStore` 与 `feedbackStore`
  - `sessionStore.overview` 是 session 总览
  - `feedbackStore.report` 是 session 阶段性反馈
  - 都服务于 session 进度判断与下一步导航

### 轻度重叠

- `diagnosisStore` 与 `learningPlanStore`
  - 两者都在围绕“从目标 -> 诊断 -> 规划”的前置流程保存上下文
  - 目标、课程、章节没有沉淀到统一 store，而是通过路由 query 在页面间传递

- `authStore` 与 `utils/storage`
  - 登录态同时存在 Pinia 和 localStorage
  - 路由守卫、API 鉴权直接读 localStorage，不直接依赖 store

## 5. 哪些 state 可能已经失效

### 高概率失效 / 未被页面直接消费

- `sessionStore.currentSessionId`
  - 只在 store 内写入和初始化
  - 页面没有直接读取
  - 当前更多像“本地缓存副本”，不是活跃 UI state

- `diagnosisStore.sessionId`
  - 在生成诊断时写入、reset 时清空
  - 页面和提交流程都没有读它
  - 当前实际 sessionId 来自路由参数

### 低活跃度 / 设计上偏冗余

- `authStore.token`
  - store 中保存 token，但路由守卫和 API 请求都直接从 localStorage 取 token
  - 仍然是有效数据，但已经不是唯一事实来源

- `learningPlanStore.request`
  - 主要被 `regeneratePreview` / `confirmPlan` 内部复用
  - 对页面展示几乎不直接提供价值，更像内部流程缓存
  - 不算失效，但更接近“流程临时上下文”

### 不是 state，但同样疑似闲置

- `authStore.isAuthenticated`
  - 未发现使用

- `authStore.refreshUser()`
  - 未发现调用

- `sessionStore.planSession()`
  - 未发现调用

## 6. 页面 -> store 使用关系

### 页面直接使用

- `HomeView`
  - `sessionStore`

- `LoginView`
  - `authStore`

- `DiagnosisView`
  - `diagnosisStore`

- `LearningPlanView`
  - `learningPlanStore`

- `SessionView`
  - `sessionStore`
  - `feedbackStore`

- `TaskRunView`
  - `taskStore`

- `QuizView`
  - `quizStore`

- `ReportView`
  - `feedbackStore`

- `GrowthDashboardView`
  - `feedbackStore`

### 非页面但会直接使用 store 的全局/公共组件

- `AppHeader`
  - `authStore`

- `main.ts`
  - `authStore`（仅初始化 `hydrateFromLocalStorage`）

## 7. 哪些 store 只被一个页面使用（建议下沉）

### 只被一个页面直接使用

- `diagnosisStore`
  - 仅 `DiagnosisView`
  - 可考虑下沉到页面级 composable / 局部 store

- `learningPlanStore`
  - 仅 `LearningPlanView`
  - 可考虑下沉到页面级 composable / 局部 store

- `quizStore`
  - 仅 `QuizView`
  - 可考虑下沉到页面级 composable / 局部 store

- `taskStore`
  - 仅 `TaskRunView`
  - 可考虑下沉到页面级 composable / 局部 store

### 不建议下沉

- `authStore`
  - 被 `LoginView`、`AppHeader`、`main.ts` 共同使用

- `sessionStore`
  - 被 `HomeView`、`SessionView` 共同使用

- `feedbackStore`
  - 被 `SessionView`、`ReportView`、`GrowthDashboardView` 共同使用

## 附加观察

- 当前 store 划分基本是“一个业务阶段一个 store”，可读性不错，但全局状态和页面状态混放。
- `loading` / `error` 在多个 store 中都是单字段，若未来同页并发请求增多，容易互相覆盖。
- “目标/课程/章节/sessionId” 这类流程上下文目前主要靠路由 query 传递，而不是集中建模，导致 `diagnosis -> plan -> session` 之间存在轻微状态分散。
