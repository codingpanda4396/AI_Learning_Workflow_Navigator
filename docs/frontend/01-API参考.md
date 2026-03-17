# API 参考

基础 URL：`http://localhost:8080`（开发环境）

所有接口返回 `GlobalResponse<T>` 格式：

```ts
interface GlobalResponse<T> {
  code: string;
  message: string;
  data: T | null;
}
```

---

## 1. 目标输入

### POST /api/goals

创建学习目标，返回结构化目标与上下文快照。

**请求体：** `CreateGoalRequest`（JSON）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rawGoalText | string | 是 | 用户当前学习目标 |
| timeBudget | TimeBudget | 否 | 时间预算 |
| selfReportedLevel | SelfReportedLevel | 否 | 自评基础水平 |
| preferenceTags | PreferenceTag[] | 否 | 学习偏好标签 |
| goalTypeHint | GoalType | 否 | 目标类型提示 |
| subjectHint | string | 否 | 学科提示 |
| topicHints | string[] | 否 | 主题提示 |
| sourceContext | string | 否 | 来源上下文 |
| priorityModule | string | 否 | 优先模块/主题 |

**响应 data：** `CreateGoalData`

| 字段 | 类型 | 说明 |
|------|------|------|
| goalId | string | 目标 ID |
| structuredGoal | StructuredLearningGoal | 结构化目标 |
| goalContextSnapshot | GoalContextSnapshot | 目标上下文快照 |

---

## 2. 用户诊断

### POST /api/diagnosis/sessions

基于目标创建诊断会话，返回诊断题集。

**请求体：** `CreateDiagnosisSessionRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| goalId | string | 是 | 目标 ID（来自 create goal） |

**响应 data：** `DiagnosisSessionData`

| 字段 | 类型 | 说明 |
|------|------|------|
| diagnosisId | string | 诊断会话 ID |
| sessionId | string | 学习会话 ID |
| status | string | `READY` / `COMPLETED` |
| generationMode | string | 生成模式 |
| questions | DiagnosisQuestion[] | 诊断题目列表 |

### POST /api/diagnosis/submissions

提交诊断答案，返回用户画像与证据摘要。

**请求体：** `SubmitDiagnosisRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| diagnosisId | string | 是 | 诊断会话 ID |
| answers | DiagnosisAnswer[] | 是 | 答案列表 |

**响应 data：** `SubmitDiagnosisData`

| 字段 | 类型 | 说明 |
|------|------|------|
| diagnosisId | string | 诊断 ID |
| learnerProfileSnapshot | LearnerProfileSnapshot | 用户画像快照 |
| diagnosisEvidenceSummary | DiagnosisEvidenceSummary | 诊断证据摘要 |

---

## 3. 学习规划

### POST /api/learning-plans/preview

生成学习计划预览。

**请求体：** `PreviewLearningPlanRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| goalId | string | 是 | 目标 ID |
| diagnosisId | string | 是 | 诊断 ID（完成提交后） |

**响应 data：** `PlanPreviewData`

| 字段 | 类型 | 说明 |
|------|------|------|
| planId | string | 计划 ID |
| status | string | `PREVIEW_READY` / `COMMITTED` |
| previewOnly | boolean | 是否仅预览 |
| committed | boolean | 是否已确认 |
| goal | string | 目标简述 |
| recommendedEntry | RecommendedEntry | 推荐入口 |
| recommendedStrategy | RecommendedStrategy | 推荐策略 |
| stages | PlanStage[] | 阶段列表 |
| tasks | TaskBlueprint[] | 任务蓝图列表 |
| successCriteria | string[] | 成功标准 |
| keyEvidence | string[] | 关键证据 |
| risks | string[] | 风险提示 |

### POST /api/learning-plans/commit

确认学习计划，进入执行会话。

**请求体：** `CommitLearningPlanRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | string | 是 | 计划 ID |

**响应 data：** `CommitPlanData`

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 学习会话 ID |
| planId | string | 计划 ID |
| taskSequence | string[] | 任务 ID 顺序 |
| currentTaskId | string | 当前任务 ID |
| status | string | `IN_PROGRESS` |

---

## 4. 任务执行

### GET /api/sessions/{sessionId}/current-task

获取当前学习会话的当前任务。

**路径参数：** `sessionId` — 学习会话 ID

**响应 data：** `CurrentTaskData`

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |
| currentTask | CurrentTaskItem | 当前任务 |
| progress | ProgressItem | 进度 |

### POST /api/tasks/{taskId}/interactions

记录任务执行中的交互（可选，用于记录用户行为信号）。

**路径参数：** `taskId` — 任务 ID

**请求体：** `TaskInteractionRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话 ID |
| interactionType | string | 否 | 交互类型 |
| interactionCountDelta | number | 否 | 交互次数增量 |
| userSummarySubmitted | boolean | 否 | 是否提交用户总结 |
| behaviorSignals | string[] | 否 | 行为信号 |
| detectedIssueTags | string[] | 否 | 检测到的问题标签 |
| contentSummary | string | 否 | 内容摘要 |

**响应 data：** `TaskInteractionData`

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务 ID |
| accepted | boolean | 是否接受 |

### POST /api/tasks/{taskId}/complete

提交任务完成。

**路径参数：** `taskId` — 任务 ID

**请求体：** `CompleteTaskRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话 ID |
| completionStatus | TaskCompletionStatus | 是 | 完成状态 |
| durationMinutes | number | 否 | 耗时（分钟） |
| interactionCount | number | 否 | 交互次数 |
| userSummarySubmitted | boolean | 否 | 是否提交总结 |
| microPracticeResult | string | 否 | 微练习结果 |
| detectedIssueTags | string[] | 否 | 问题标签 |
| behaviorSignals | string[] | 否 | 行为信号 |
| learnerReflection | string | 否 | 学习者反思 |

**响应 data：** `CompleteTaskData`

| 字段 | 类型 | 说明 |
|------|------|------|
| taskExecutionRecord | TaskExecutionRecord | 任务执行记录 |
| nextTaskAvailable | boolean | 是否有下一任务 |
| nextTaskId | string | 下一任务 ID（如有） |
| sessionProgress | SessionProgressItem | 会话进度 |

---

## 5. 反馈与 Next Action

### GET /api/sessions/{sessionId}/report

获取学习报告。

**路径参数：** `sessionId` — 学习会话 ID

**响应 data：** `ReportData`

| 字段 | 类型 | 说明 |
|------|------|------|
| learningReport | LearningReport | 学习报告 |
| nextActionDecision | NextActionDecision | 下一步决策 |

### POST /api/sessions/{sessionId}/next-action

确认下一步动作（用户选择继续 / 巩固 / 补救等）。

**路径参数：** `sessionId` — 学习会话 ID

**请求体：** `ConfirmNextActionRequest`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actionType | NextActionType | 是 | 用户选择的动作类型 |

**响应 data：** `NextActionConfirmData`

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |
| acceptedAction | NextActionType | 接受的动作 |
| requiresReplan | boolean | 是否需要重新规划 |
| nextHint | string | 下一步提示 |

---

## 6. 调用顺序（最小闭环）

```
1. POST /api/goals                    → goalId
2. POST /api/diagnosis/sessions       → diagnosisId, sessionId
3. POST /api/diagnosis/submissions    → learnerProfileSnapshot
4. POST /api/learning-plans/preview   → planId
5. POST /api/learning-plans/commit    → sessionId, currentTaskId
6. GET  /api/sessions/{sessionId}/current-task  → currentTask
7. POST /api/tasks/{taskId}/complete  → 重复 6–7 直到所有任务完成
8. GET  /api/sessions/{sessionId}/report       → learningReport
9. POST /api/sessions/{sessionId}/next-action  → 用户确认后继续或结束
```
