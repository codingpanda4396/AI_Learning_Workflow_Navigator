# 页面结构

## 路由内页面

| 页面 | 页面文件路径 | 路由 | 页面主要组件 |
| --- | --- | --- | --- |
| Login | `frontend/src/views/LoginView.vue` | `/login` | 无页面级子组件；使用 `APP_TITLE` 展示登录/注册表单 |
| Home | `frontend/src/views/HomeView.vue` | `/` | `AppShell`、`StartLearningEntry`、`CurrentSessionPanel`、`WorkflowPipeline`、`ModuleNavPanel`、`GrowthSummaryPanel` |
| Diagnosis | `frontend/src/views/DiagnosisView.vue` | `/diagnosis/:sessionId?` | `AppShell`、`DiagnosisGoalSummaryCard`、`DiagnosisQuestionCard`、`DiagnosisProgressCard`、`CapabilityProfileCard`、`LoadingState`、`ErrorState` |
| LearningPlan | `frontend/src/views/LearningPlanView.vue` | `/plan` | `AppShell`、`PageSection`、`PlanSummaryPanel`、`PlanReasonPanel`、`PlanPathPreviewPanel`、`PlanTaskPreviewPanel`、`PlanAdjustPanel`、`PlanActionBar`、`ErrorState` |
| Session | `frontend/src/views/SessionView.vue` | `/sessions/:sessionId` | `AppShell`、`PageSection`、`PrimaryActionCard`、`ProgressSummary`、`SecondaryInfoCard`、`StagePill`、`LoadingState`、`ErrorState` |
| TaskRun | `frontend/src/views/TaskRunView.vue` | `/tasks/:taskId/run` | `AppShell`、`PageSection`、`LearningContentSection`、`SecondaryInfoCard`、`StagePill`、`LoadingState`、`ErrorState` |
| Quiz | `frontend/src/views/QuizView.vue` | `/sessions/:sessionId/quiz` | `AppShell`、`PageSection`、`QuizQuestionCard`、`EmptyState`、`LoadingState`、`ErrorState` |
| Report | `frontend/src/views/ReportView.vue` | `/sessions/:sessionId/report` | `AppShell`、`PageSection`、`ReportBlock`、`NextStepCard`、`LoadingState`、`ErrorState` |
| GrowthDashboard | `frontend/src/views/GrowthDashboardView.vue` | `/sessions/:sessionId/growth` | `AppShell`、`InfoCard`、`LoadingState`、`ErrorState` |

## 未挂载页面

| 页面 | 页面文件路径 | 当前状态 | 页面主要组件 |
| --- | --- | --- | --- |
| Placeholder | `frontend/src/views/PlaceholderView.vue` | 未注册到 `frontend/src/router/index.ts` | 仅自身占位卡片模板 |

# 页面跳转图

## 主流程

`Login -> Home -> Diagnosis -> LearningPlan -> Session -> TaskRun -> Quiz -> Report -> Session`

## 报告后的分支

- `Report -> Quiz`
- `Report -> GrowthDashboard`
- `Report -> Session`

## 会话内分支

- `Session -> TaskRun`
- `Session -> Quiz`
- `Session -> Report`
- `TaskRun -> Session`
- `TaskRun -> Quiz`

## 返回关系

- `Diagnosis -> Home`（缺少 `sessionId` 时）
- `LearningPlan -> Home`
- `LearningPlan -> Diagnosis`

## Home 内当前实现的入口/跳转

- `Home -> Diagnosis/:sessionId`：`StartLearningEntry` 提交后创建 session 再跳转
- `Home -> /session/:id`：`CurrentSessionPanel` 当前写死为单数路径，和现有路由 `/sessions/:sessionId` 不一致
- `Home -> /diagnosis`、`/plan`、`/task`、`/evaluation`、`/knowledge`、`/growth`：来自 `homeModuleEntries` mock 导航；其中只有 `/diagnosis`、`/plan` 与当前路由匹配
- `Home -> /growth`：`GrowthSummaryPanel` 当前跳到 `/growth`，和现有路由 `/sessions/:sessionId/growth` 不一致

# 页面 -> API 映射

| 页面 | 通过 store 间接调用的 API | 对应接口路径 |
| --- | --- | --- |
| Login | `registerApi`、`loginApi` | `POST /api/auth/register`；`POST /api/auth/login` |
| Home | `createSessionApi`、`fetchCurrentSessionApi` | `POST /api/session/create`；`GET /api/session/current` |
| Diagnosis | `generateDiagnosisApi`、`submitDiagnosisApi` | `POST /api/diagnosis/generate`；`POST /api/diagnosis/submit` |
| LearningPlan | `fetchLearningPlanPreviewApi`、`regenerateLearningPlanApi`、`confirmLearningPlanApi` | `POST /api/learning-plan/preview`；`POST /api/learning-plan/regenerate`；`POST /api/learning-plan/confirm` |
| LearningPlan | `confirmLearningPlanApi` fallback 依赖 | `POST /api/session/{sessionId}/plan?mode=auto`；`POST /api/session/create` |
| Session | `fetchOverviewApi`、`fetchReportApi` | `GET /api/session/{sessionId}/overview`；`GET /api/sessions/{sessionId}/feedback`；`GET /api/session/{sessionId}/learning-feedback/report`；`GET /api/session/{sessionId}/learning-feedback/weak-points` |
| TaskRun | `fetchTaskDetailApi`、`runTaskApi` | `GET /api/task/{taskId}`；`POST /api/task/{taskId}/run` |
| Quiz | `fetchQuizStatusApi`、`generateQuizApi`、`fetchQuizApi`、`submitQuizApi` | `GET /api/sessions/{sessionId}/quiz/status`；`POST /api/sessions/{sessionId}/quiz/generate`；`GET /api/sessions/{sessionId}/quiz`；`POST /api/sessions/{sessionId}/quiz/submit` |
| Report | `fetchReportApi`、`submitNextActionApi` | `GET /api/sessions/{sessionId}/feedback`；`GET /api/session/{sessionId}/learning-feedback/report`；`GET /api/session/{sessionId}/learning-feedback/weak-points`；`POST /api/sessions/{sessionId}/next-action` |
| GrowthDashboard | `fetchGrowthDashboardApi` | `GET /api/session/{sessionId}/growth-dashboard` |
| Placeholder | 无 | 无 |

# 页面 -> store 映射

| 页面 | 使用的 Pinia store |
| --- | --- |
| Login | `useAuthStore` |
| Home | `useSessionStore` |
| Diagnosis | `useDiagnosisStore` |
| LearningPlan | `useLearningPlanStore` |
| Session | `useSessionStore`、`useFeedbackStore` |
| TaskRun | `useTaskStore` |
| Quiz | `useQuizStore` |
| Report | `useFeedbackStore` |
| GrowthDashboard | `useFeedbackStore` |
| Placeholder | 无 |

# mock 数据位置

## 页面直接使用 mock 的位置

| 页面 | mock 文件 | 用途 |
| --- | --- | --- |
| Home | `frontend/src/mocks/home.ts` | 首页 workflow、模块导航、成长摘要数据；同时提供 `ActiveSession`、`StartLearningForm` 等类型 |
| LearningPlan | `frontend/src/mocks/learningPlan.ts` | 不直接 import，但其 API 层 fallback 会生成预览 mock 数据 |
| Diagnosis | 无独立 mock 文件 | mock 逻辑内嵌在 `frontend/src/api/modules/diagnosis.ts` |

## mock 文件清单

- `frontend/src/mocks/home.ts`
- `frontend/src/mocks/learningPlan.ts`

## 当前使用 mock 的页面结论

- `Home`：明确使用 `home.ts` 的展示型 mock 数据
- `LearningPlan`：当 `/api/learning-plan/preview` 或 `/api/learning-plan/regenerate` 不可用时，回退到 `createMockLearningPlanPreview`
- `Diagnosis`：当 `VITE_ENABLE_DIAGNOSIS_API !== 'true'`，或诊断接口调用失败时，回退到内嵌 mock 题目与能力画像
- 其他页面当前未发现直接引用 `src/mocks` 的实现
