# API Usage Report BV1003

说明：本报告基于 `frontend/src` 静态扫描；“调用次数”按源码入口计数，不是运行时埋点。同一 API 如果在不同页面、按钮或生命周期中分别触发，会分别计入。

## 1. 前端实际调用的所有 API

| API 路径 | 调用文件 | 调用次数 | 主要入口 |
| --- | --- | ---: | --- |
| `POST /api/auth/register` | `frontend/src/api/modules/auth.ts` | 1 | `LoginView` 注册模式提交 |
| `POST /api/auth/login` | `frontend/src/api/modules/auth.ts` | 1 | `LoginView` 登录/注册后登录 |
| `GET /api/users/me` | `frontend/src/api/modules/auth.ts` | 1 | `authStore.refreshUser()`，当前未发现页面直接触发 |
| `POST /api/diagnosis/generate` | `frontend/src/api/modules/diagnosis.ts` | 2 | `DiagnosisView` 首次加载、重试生成 |
| `POST /api/diagnosis/submit` | `frontend/src/api/modules/diagnosis.ts` | 1 | `DiagnosisView` 提交诊断 |
| `POST /api/learning-plan/preview` | `frontend/src/api/modules/learningPlan.ts` | 1 | `LearningPlanView` 首次加载/查询参数变化 |
| `POST /api/learning-plan/regenerate` | `frontend/src/api/modules/learningPlan.ts` | 2 | `LearningPlanView` 调整后重生成、底部操作栏重生成 |
| `POST /api/learning-plan/confirm` | `frontend/src/api/modules/learningPlan.ts` | 1 | `LearningPlanView` 确认方案 |
| `POST /api/session/create` | `frontend/src/api/modules/session.ts` | 2 | `HomeView` 创建 session；`LearningPlan` confirm fallback 创建 session |
| `POST /api/session/{sessionId}/plan?mode=auto` | `frontend/src/api/modules/session.ts` | 3 | `sessionStore.planSession()`；`LearningPlan` confirm fallback 复用 |
| `GET /api/session/{sessionId}/overview` | `frontend/src/api/modules/session.ts` | 1 | `SessionView` 加载概览 |
| `GET /api/session/current` | `frontend/src/api/modules/session.ts` | 1 | `HomeView` 加载当前 session |
| `POST /api/sessions/{sessionId}/quiz/generate` | `frontend/src/api/modules/quiz.ts` | 2 | `QuizView` 空态按钮、底部按钮 |
| `GET /api/sessions/{sessionId}/quiz/status` | `frontend/src/api/modules/quiz.ts` | 2 | `QuizView` 初始检查、轮询 |
| `GET /api/sessions/{sessionId}/quiz` | `frontend/src/api/modules/quiz.ts` | 2 | `QuizView` 初始拉取、轮询完成后拉取 |
| `POST /api/sessions/{sessionId}/quiz/submit` | `frontend/src/api/modules/quiz.ts` | 1 | `QuizView` 提交答案 |
| `GET /api/sessions/{sessionId}/feedback` | `frontend/src/api/modules/feedback.ts` | 3 | `SessionView`、`ReportView`、`submitNextActionApi()` 后续刷新 |
| `GET /api/session/{sessionId}/learning-feedback/report` | `frontend/src/api/modules/feedback.ts` | 3 | `SessionView`、`ReportView`、`submitNextActionApi()` 后续刷新 |
| `GET /api/session/{sessionId}/learning-feedback/weak-points` | `frontend/src/api/modules/feedback.ts` | 3 | `SessionView`、`ReportView`、`submitNextActionApi()` 后续刷新 |
| `POST /api/sessions/{sessionId}/next-action` | `frontend/src/api/modules/feedback.ts` | 1 | `ReportView` 提交下一步动作 |
| `GET /api/session/{sessionId}/growth-dashboard` | `frontend/src/api/modules/feedback.ts` | 1 | `GrowthDashboardView` |
| `GET /api/task/{taskId}` | `frontend/src/api/modules/task.ts` | 1 | `TaskRunView` 读取任务详情 |
| `POST /api/task/{taskId}/run` | `frontend/src/api/modules/task.ts` | 1 | `TaskRunView` 执行任务 |

## 2. 按页面分类

### LoginView

- `POST /api/auth/register`
- `POST /api/auth/login`

### HomeView

- `POST /api/session/create`
- `GET /api/session/current`

### DiagnosisView

- `POST /api/diagnosis/generate`
- `POST /api/diagnosis/submit`

### LearningPlanView

- `POST /api/learning-plan/preview`
- `POST /api/learning-plan/regenerate`
- `POST /api/learning-plan/confirm`
- `POST /api/session/{sessionId}/plan?mode=auto`（仅 confirm fallback）
- `POST /api/session/create`（仅 confirm fallback）

### SessionView

- `GET /api/session/{sessionId}/overview`
- `GET /api/sessions/{sessionId}/feedback`
- `GET /api/session/{sessionId}/learning-feedback/report`
- `GET /api/session/{sessionId}/learning-feedback/weak-points`

### TaskRunView

- `GET /api/task/{taskId}`
- `POST /api/task/{taskId}/run`

### QuizView

- `POST /api/sessions/{sessionId}/quiz/generate`
- `GET /api/sessions/{sessionId}/quiz/status`
- `GET /api/sessions/{sessionId}/quiz`
- `POST /api/sessions/{sessionId}/quiz/submit`

### ReportView

- `GET /api/sessions/{sessionId}/feedback`
- `GET /api/session/{sessionId}/learning-feedback/report`
- `GET /api/session/{sessionId}/learning-feedback/weak-points`
- `POST /api/sessions/{sessionId}/next-action`

### GrowthDashboardView

- `GET /api/session/{sessionId}/growth-dashboard`

### 非页面直接触发

- `GET /api/users/me`：仅在 `authStore.refreshUser()` 中定义，当前未发现页面入口
- `POST /api/session/{sessionId}/plan?mode=auto`：`sessionStore.planSession()` 已定义，当前未发现页面直接调用

## 3. 问题扫描

### 页面调用多个接口拼装数据

- `SessionView` 通过 `sessionStore.fetchOverview()` 拉 `1` 个概览接口，同时通过 `feedbackStore.fetchReport()` 再拼 `3` 个反馈接口，页面总计依赖 `4` 个 HTTP 接口。
- `ReportView` 的报告数据不是单接口返回，而是 `fetchReportApi()` 内部并行拼装：
  - `GET /api/sessions/{sessionId}/feedback`
  - `GET /api/session/{sessionId}/learning-feedback/report`
  - `GET /api/session/{sessionId}/learning-feedback/weak-points`
- `submitNextActionApi()` 在 `POST /api/sessions/{sessionId}/next-action` 之后，又会再次调用上述 `3` 个反馈接口刷新页面数据。

### 重复 API

- `SessionView` 与 `ReportView` 都会加载同一组反馈接口，存在明显重复：
  - `GET /api/sessions/{sessionId}/feedback`
  - `GET /api/session/{sessionId}/learning-feedback/report`
  - `GET /api/session/{sessionId}/learning-feedback/weak-points`
- 语义上也有重复迹象：`/api/sessions/{sessionId}/feedback` 与 `/api/session/{sessionId}/learning-feedback/report` 都在表达“反馈/报告”，前端需要再做 merge。

### 已不存在的 API

对照 `backend/src/main/java/.../controller` 后，以下前端接口与后端现状不匹配：

| 前端调用 | 后端现状 | 结论 |
| --- | --- | --- |
| `POST /api/learning-plan/preview` | 后端为 `POST /api/learning-plans/preview` | 路径不存在，少了复数 `s` |
| `POST /api/learning-plan/regenerate` | 后端未找到对应 Controller 映射 | 前端定义存在，后端接口不存在 |
| `POST /api/learning-plan/confirm` | 后端为 `POST /api/learning-plans/{planId}/confirm` | 路径和入参模型都不一致 |

### mock API

- `frontend/src/api/modules/diagnosis.ts`
  - `VITE_ENABLE_DIAGNOSIS_API !== 'true'` 时，`generateDiagnosisApi()` 与 `submitDiagnosisApi()` 直接返回内嵌 mock 数据
  - 即使开启真实接口，请求异常时也会 fallback 到同一套 mock
- `frontend/src/api/modules/learningPlan.ts`
  - `fetchLearningPlanPreviewApi()` 请求失败时回退到 `createMockLearningPlanPreview()`
  - `regenerateLearningPlanApi()` 请求失败时回退到 `createMockLearningPlanPreview()`
- `HomeView` 虽未调用 mock HTTP，但页面展示数据大量来自 `frontend/src/mocks/home.ts`

### 命名不一致的 API

- `session` 与 `sessions` 混用：
  - `/api/session/{id}/overview`
  - `/api/sessions/{id}/quiz`
  - `/api/sessions/{id}/feedback`
- `learning-plan` 与后端真实 `learning-plans` 不一致。
- `task` 使用单数 `/api/task/{taskId}`，而 quiz/feedback 使用复数 `/api/sessions/{sessionId}`；资源命名风格不统一。
- 同一类反馈资源分散在：
  - `/api/sessions/{id}/feedback`
  - `/api/session/{id}/learning-feedback/report`
  - `/api/session/{id}/learning-feedback/weak-points`
  前端需要额外做聚合和兼容。

## 4. 总结表

| API | 使用页面 | 备注 |
| --- | --- | --- |
| `POST /api/auth/register` | `LoginView` | 正常 |
| `POST /api/auth/login` | `LoginView` | 正常 |
| `GET /api/users/me` | 无页面直接入口 | store 中定义但当前未接入页面初始化流程 |
| `POST /api/diagnosis/generate` | `DiagnosisView` | 支持 mock fallback |
| `POST /api/diagnosis/submit` | `DiagnosisView` | 支持 mock fallback |
| `POST /api/learning-plan/preview` | `LearningPlanView` | 前端路径与后端不一致；失败时走 mock |
| `POST /api/learning-plan/regenerate` | `LearningPlanView` | 后端未发现对应接口；失败时走 mock |
| `POST /api/learning-plan/confirm` | `LearningPlanView` | 前端路径与后端 confirm 模型不一致 |
| `POST /api/session/create` | `HomeView`、`LearningPlanView` fallback | 正常 |
| `POST /api/session/{sessionId}/plan?mode=auto` | `LearningPlanView` fallback | 正常；另有 store action 预留但页面未直连 |
| `GET /api/session/{sessionId}/overview` | `SessionView` | 正常 |
| `GET /api/session/current` | `HomeView` | 正常 |
| `POST /api/sessions/{sessionId}/quiz/generate` | `QuizView` | 正常 |
| `GET /api/sessions/{sessionId}/quiz/status` | `QuizView` | 正常；存在轮询 |
| `GET /api/sessions/{sessionId}/quiz` | `QuizView` | 正常 |
| `POST /api/sessions/{sessionId}/quiz/submit` | `QuizView` | 正常 |
| `GET /api/sessions/{sessionId}/feedback` | `SessionView`、`ReportView` | 与 learning-feedback/report 语义重叠 |
| `GET /api/session/{sessionId}/learning-feedback/report` | `SessionView`、`ReportView` | 报告聚合接口之一 |
| `GET /api/session/{sessionId}/learning-feedback/weak-points` | `SessionView`、`ReportView` | 报告聚合接口之一 |
| `POST /api/sessions/{sessionId}/next-action` | `ReportView` | 提交后会再次触发 3 个反馈接口 |
| `GET /api/session/{sessionId}/growth-dashboard` | `GrowthDashboardView` | 正常 |
| `GET /api/task/{taskId}` | `TaskRunView` | 正常 |
| `POST /api/task/{taskId}/run` | `TaskRunView` | 正常 |
