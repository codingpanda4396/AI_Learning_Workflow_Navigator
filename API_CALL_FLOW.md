# 前端 5 页与 Sprint 0 接口调用顺序

本文档说明前端五个页面如何按主链路顺序调用 10 个 API，以及各步需要保存/展示的关键字段。

---

## 主链路概览

```
目标输入页 → 诊断页 → 计划预览页 → 任务执行页 → 报告页
```

| 页面 | 调用接口 | 入参要点 | 需保存/展示的数据 |
|------|----------|----------|-------------------|
| 目标输入页 | `POST /api/goals` | 表单对应 LearningGoalInput | `goalId`、可选展示 structuredGoal / goalContextSnapshot |
| 诊断页 | `POST /api/diagnosis/sessions`<br>`POST /api/diagnosis/submissions` | goalId；diagnosisId + answers | `diagnosisId`、questions 渲染；提交后展示 learnerProfileSnapshot、diagnosisEvidenceSummary |
| 计划预览页 | `POST /api/learning-plans/preview`<br>`POST /api/learning-plans/commit` | goalId + diagnosisId；planId | 展示 recommendedEntry、recommendedStrategy、stages、tasks；保存 `sessionId` |
| 任务执行页 | `GET /api/sessions/{sessionId}/current-task`<br>`POST /api/tasks/{taskId}/interactions`<br>`POST /api/tasks/{taskId}/complete` | sessionId；taskId + body；taskId + body | 当前任务信息、进度；完成后再拉 current-task 或根据 nextTaskId 跳转 |
| 报告页 | `GET /api/sessions/{sessionId}/report`<br>`POST /api/sessions/{sessionId}/next-action` | sessionId；actionType | learningReport、nextActionDecision；确认后展示 nextHint |

---

## 1. 目标输入页

- **调用**：`POST /api/goals`
- **请求体**：与 `LearningGoalInput` 对应，例如：
  - `rawGoalText`, `timeBudget`, `selfReportedLevel`, `preferenceTags`, `goalTypeHint`, `subjectHint`, `topicHints`, `sourceContext`
- **响应 data**：`goalId`, `structuredGoal`, `goalContextSnapshot`
- **前端**：提交后保存 `goalId`（必存），可简要展示 `structuredGoal.normalizedGoalText` 或 `goalContextSnapshot.planningMode`，然后跳转诊断页。

---

## 2. 诊断页

- **进入页时**：`POST /api/diagnosis/sessions`
  - 请求体：`{ "goalId": "<上一步保存的 goalId>" }`
  - 响应 data：`diagnosisId`, `sessionId`, `status`, `generationMode`, `questions`
  - 用 `questions` 渲染题目（title, description, options 等），保存 `diagnosisId`（和可选 sessionId）。
- **用户提交答案后**：`POST /api/diagnosis/submissions`
  - 请求体：`{ "diagnosisId": "<diagnosisId>", "answers": [ { "questionId", "selectedOptions" }, ... ] }`
  - 响应 data：`diagnosisId`, `learnerProfileSnapshot`, `diagnosisEvidenceSummary`
  - 可展示摘要、证据或规划提示，然后跳转计划预览页。

---

## 3. 计划预览页

- **进入页时**：`POST /api/learning-plans/preview`
  - 请求体：`{ "goalId": "<goalId>", "diagnosisId": "<diagnosisId>" }`
  - 响应 data：`planId`, `status`, `previewOnly`, `committed`, `goal`, `recommendedEntry`, `recommendedStrategy`, `stages`, `tasks`, `successCriteria`, `keyEvidence`, `risks`
  - 展示推荐入口、策略、阶段、任务列表等。
- **用户点击「开始学习」**：`POST /api/learning-plans/commit`
  - 请求体：`{ "planId": "<planId>" }`
  - 响应 data：`sessionId`, `planId`, `taskSequence`, `currentTaskId`, `status`
  - **必须保存 `sessionId`**，供任务执行页与报告页使用；然后跳转任务执行页。

---

## 4. 任务执行页

- **进入/刷新当前任务**：`GET /api/sessions/{sessionId}/current-task`
  - 响应 data：`sessionId`, `currentTask`（含 taskId, title, taskType, goal, whyThisTask, estimatedMinutes, promptScaffold, completionCriteria, fallbackAction）, `progress`（currentIndex, totalTasks）
  - 用 `currentTask` 与 `progress` 渲染当前任务与进度。
- **学习过程中的关键交互**：`POST /api/tasks/{taskId}/interactions`
  - 请求体：`sessionId`, `interactionType`, `interactionCountDelta`, `userSummarySubmitted`, `behaviorSignals`, `detectedIssueTags`, `contentSummary` 等（按需传）。
  - 用于记录行为信号，Sprint 0 仅需能调通即可。
- **用户标记当前任务完成**：`POST /api/tasks/{taskId}/complete`
  - 请求体：`sessionId`, `completionStatus`, `durationMinutes`, `interactionCount`, `userSummarySubmitted`, `microPracticeResult`, `detectedIssueTags`, `behaviorSignals`, `learnerReflection`
  - 响应 data：`taskExecutionRecord`, `nextTaskAvailable`, `nextTaskId`, `sessionProgress`
  - 若 `nextTaskAvailable === true`：可继续拉取 `GET /api/sessions/{sessionId}/current-task` 显示下一任务，或根据 `nextTaskId` 刷新。
  - 若 `nextTaskAvailable === false`：跳转报告页。

---

## 5. 报告页

- **进入页时**：`GET /api/sessions/{sessionId}/report`
  - 响应 data：`learningReport`（resultStatus, goalReview, completedProgress, unresolvedIssues, evidenceSummary, summaryText, nextAction）, `nextActionDecision`（actionType, reason, nextEntryPoint, adjustmentSignals, requiresReplan）
  - 展示学习报告与下一步建议。
- **用户确认下一步**：`POST /api/sessions/{sessionId}/next-action`
  - 请求体：`{ "actionType": "REINFORCE" }`（或 CONTINUE、REMEDIATE_PREREQUISITE 等）
  - 响应 data：`sessionId`, `acceptedAction`, `requiresReplan`, `nextHint`
  - 展示 `nextHint`，Sprint 0 不要求真正生成下一轮计划。

---

## 统一响应格式

- 成功：`{ "code": "OK", "message": "success", "data": { ... } }`
- 失败：`{ "code": "BAD_REQUEST" | "NOT_FOUND" | "CONFLICT" | "INTERNAL_ERROR", "message": "...", "data": null }`

前端可根据 `code` 判断成功与否，并从 `data` 取业务数据。

---

## 跨页需要持久的数据（建议）

- 目标输入页 → 诊断页：`goalId`
- 诊断页 → 计划预览页：`goalId`, `diagnosisId`
- 计划预览页 → 任务执行页：`sessionId`（以及可选 `goalId`, `planId`）
- 任务执行页 → 报告页：`sessionId`
- 报告页：仅需 `sessionId` 拉报告与提交 next-action

可使用前端路由参数、全局状态或本地存储保存上述 id，保证刷新或跳转后仍可继续主链路。
