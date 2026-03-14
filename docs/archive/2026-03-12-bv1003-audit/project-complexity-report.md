# PROJECT COMPLEXITY REPORT

## 扫描口径

- 页面数量：9
  - 按 `frontend/src/router/index.ts` 中已接入路由的页面统计
  - `PlaceholderView.vue` 未接入路由，未计入
- store 数量：7
  - `frontend/src/stores/*.ts`
- API 数量：39
  - 按后端 `backend/src/main/java/com/pandanav/learning/api/controller` 中 `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping` 统计
  - 补充：前端 `frontend/src/api/modules/*.ts` 封装请求方法 21 个
- 状态枚举数量：5
  - 按后端 `backend/src/main/java/com/pandanav/learning/domain/enums/*Status.java` 统计
  - 包含：`DiagnosisStatus`、`LearningPlanStatus`、`PracticeItemStatus`、`PracticeQuizStatus`、`TaskStatus`

## 模块判断

- 复杂度最高的模块：`session / practice` 会话执行链路
  - 相关接口最多：`session` 14 个，`practice` 8 个
  - 覆盖 session、quiz、report、growth、task 跳转与状态推进
- 耦合最严重的模块：`session`
  - 前端串联 `sessionStore`、`feedbackStore`、路由跳转
  - 后端拆分在 `SessionController`、`SessionHistoryController`、`SessionQuizController`
- 最可能导致 bug 的模块：`quiz / practice`
  - 同时存在生成中、轮询、提交、完成、失败等多状态切换
  - 前端状态来源同时依赖 `generationStatus` 与 `quizStatus`，容易出现边界状态不一致

## 必须重构

- `session`
- `quiz / practice`
- `learningPlan`

## 建议重构

- `diagnosis`
- `task`
- `tutor`

## 暂时稳定

- `auth`
- `feedback / insight / capability-profile`
- `home / growth / report`
