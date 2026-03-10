# AI 学习流程导航系统 MVP 最小 API 契约

## 1. API 列表

- `POST /api/sessions/{id}/quiz/generate`
- `GET /api/sessions/{id}/quiz/status`
- `GET /api/sessions/{id}/quiz`
- `POST /api/sessions/{id}/quiz/submit`
- `GET /api/sessions/{id}/feedback`
- `POST /api/sessions/{id}/next-action`

说明：
- 当前项目后端真实前缀为 `/api/sessions/{sessionId}`。
- 以上 6 个接口均基于“session 下当前 training task”工作，不要求前端再传 `task_id`。

## 2. 状态命名

以下状态名必须与后端保持一致，前端不要自行改写：

### 2.1 generation_status

来源：`TaskStatus`

```json
["PENDING", "RUNNING", "SUCCEEDED", "FAILED"]
```

### 2.2 quiz_status

来源：`PracticeQuizStatus`

```json
["GENERATING", "QUIZ_READY", "ANSWERED", "FEEDBACK_READY", "REVIEWING", "NEXT_ROUND", "FAILED"]
```

### 2.3 report_status

来源：`TaskStatus`

```json
["PENDING", "RUNNING", "SUCCEEDED", "FAILED"]
```

### 2.4 action

来源：`PracticeFeedbackAction`

```json
["REVIEW", "NEXT_ROUND"]
```

## 3. 接口契约

### 3.1 POST `/api/sessions/{id}/quiz/generate`

用途：
- 触发当前 session 对应训练任务的异步出题。
- 若已有 `PENDING/RUNNING/SUCCEEDED` 的 quiz，直接返回当前状态快照，不重复创建。

request：

```json
{}
```

response `200`：

```json
{
  "session_id": 101,
  "task_id": 1001,
  "quiz_id": 5001,
  "generation_status": "PENDING",
  "quiz_status": "GENERATING",
  "question_count": 0,
  "answered_count": 0,
  "failure_reason": null,
  "retryable": false
}
```

前端可直接渲染：
- `generation_status`
- `quiz_status`
- `question_count`
- `answered_count`
- `failure_reason`
- `retryable`

仅内部诊断：
- 本接口当前无额外诊断字段返回。
- `trace_id/prompt_version/token_input/token_output/latency_ms/last_error_code` 建议继续只留库内，不进 MVP 响应。

### 3.2 GET `/api/sessions/{id}/quiz/status`

用途：
- 轮询异步出题状态。

request：

```json
{}
```

response `200`：

```json
{
  "session_id": 101,
  "task_id": 1001,
  "quiz_id": 5001,
  "generation_status": "SUCCEEDED",
  "quiz_status": "QUIZ_READY",
  "question_count": 3,
  "answered_count": 0,
  "failure_reason": null,
  "retryable": false
}
```

前端可直接渲染：
- 全字段都可直接用在状态条、按钮态、失败重试提示。

仅内部诊断：
- 无。

### 3.3 GET `/api/sessions/{id}/quiz`

用途：
- 获取题目内容。
- 仅当 `generation_status=SUCCEEDED` 时，`questions` 才有内容。

request：

```json
{}
```

response `200`：

```json
{
  "session_id": 101,
  "task_id": 1001,
  "quiz_id": 5001,
  "generation_status": "SUCCEEDED",
  "quiz_status": "QUIZ_READY",
  "question_count": 2,
  "answered_count": 0,
  "failure_reason": null,
  "questions": [
    {
      "question_id": 9001,
      "type": "SINGLE_CHOICE",
      "stem": "下面哪个说法最符合二分查找的前提？",
      "options": ["A. 有序数组", "B. 无序链表", "C. 哈希表", "D. 图"],
      "evaluation_focus": "检查是否理解适用前提",
      "difficulty": "EASY",
      "status": "ACTIVE"
    },
    {
      "question_id": 9002,
      "type": "SHORT_ANSWER",
      "stem": "请说明二分查找为什么能把时间复杂度降到 O(log n)。",
      "options": [],
      "evaluation_focus": "检查是否能解释折半缩小搜索空间",
      "difficulty": "MEDIUM",
      "status": "ACTIVE"
    }
  ]
}
```

前端可直接渲染：
- 顶层：`generation_status`、`quiz_status`、`question_count`、`answered_count`、`failure_reason`
- 题目：`question_id`、`type`、`stem`、`options`、`evaluation_focus`、`difficulty`

仅内部诊断：
- `questions[].status` 建议先不做用户显式展示，只作为本地提交态校验。

### 3.4 POST `/api/sessions/{id}/quiz/submit`

用途：
- 一次性提交整套 quiz 答案。
- 当前后端要求前端至少传 1 条答案，且必须覆盖全部题目，否则报错。
- 若反馈报告已生成，当前实现会直接返回已有 feedback。

request：

```json
{
  "answers": [
    {
      "question_id": 9001,
      "answer": "A"
    },
    {
      "question_id": 9002,
      "answer": "因为每次比较后都会把搜索区间缩小一半。"
    }
  ]
}
```

response `200`：

```json
{
  "report_id": 7001,
  "quiz_id": 5001,
  "session_id": 101,
  "task_id": 1001,
  "report_status": "SUCCEEDED",
  "overall_summary": "当前测验表现基本稳定，但仍有一个知识点需要回看。",
  "question_results": [
    {
      "question_id": 9001,
      "type": "SINGLE_CHOICE",
      "stem": "下面哪个说法最符合二分查找的前提？",
      "user_answer": "A",
      "score": 100,
      "correct": true,
      "feedback": "Correct choice.",
      "error_tags": []
    },
    {
      "question_id": 9002,
      "type": "SHORT_ANSWER",
      "stem": "请说明二分查找为什么能把时间复杂度降到 O(log n)。",
      "user_answer": "因为每次比较后都会把搜索区间缩小一半。",
      "score": 85,
      "correct": true,
      "feedback": "Answer covers most key points, but wording can be more precise.",
      "error_tags": []
    }
  ],
  "strengths": ["完整覆盖了本轮题目", "关键前提判断正确"],
  "weaknesses": ["短答题表述还可以更精确"],
  "review_focus": ["CONSOLIDATE_CORE_CONCEPT"],
  "next_round_advice": "Start the next round with one harder scenario question.",
  "suggested_next_action": "NEXT_ROUND",
  "recommended_action": "NEXT_ROUND",
  "selected_action": null,
  "source": "RULE"
}
```

前端可直接渲染：
- `overall_summary`
- `question_results`
- `strengths`
- `weaknesses`
- `review_focus`
- `next_round_advice`
- `recommended_action`
- `selected_action`

仅内部诊断：
- `source` 建议不作为主要用户文案展示，只用于埋点或调试。
- `suggested_next_action` 当前与 `recommended_action` 值相同，前端建议只认 `recommended_action`，保留前者做兼容。

### 3.5 GET `/api/sessions/{id}/feedback`

用途：
- 查询已生成的 quiz 反馈报告。

request：

```json
{}
```

response `200`：

```json
{
  "report_id": 7001,
  "quiz_id": 5001,
  "session_id": 101,
  "task_id": 1001,
  "report_status": "SUCCEEDED",
  "overall_summary": "当前测验表现基本稳定，但仍有一个知识点需要回看。",
  "question_results": [
    {
      "question_id": 9001,
      "type": "SINGLE_CHOICE",
      "stem": "下面哪个说法最符合二分查找的前提？",
      "user_answer": "A",
      "score": 100,
      "correct": true,
      "feedback": "Correct choice.",
      "error_tags": []
    }
  ],
  "strengths": ["完整覆盖了本轮题目"],
  "weaknesses": ["仍需巩固短答表达"],
  "review_focus": ["CONSOLIDATE_CORE_CONCEPT"],
  "next_round_advice": "Start the next round with one harder scenario question.",
  "suggested_next_action": "NEXT_ROUND",
  "recommended_action": "NEXT_ROUND",
  "selected_action": "REVIEW",
  "source": "RULE"
}
```

前端可直接渲染：
- 与 `POST /quiz/submit` 返回一致，可直接复用同一套反馈页组件。

仅内部诊断：
- `source`

### 3.6 POST `/api/sessions/{id}/next-action`

用途：
- 用户在反馈页明确选择下一步动作。
- 当前仅支持 `REVIEW` / `NEXT_ROUND`。
- 接口会更新反馈报告中的 `selected_action`，并同步 quiz 状态。

request：

```json
{
  "action": "REVIEW"
}
```

response `200`：

```json
{
  "report_id": 7001,
  "quiz_id": 5001,
  "session_id": 101,
  "task_id": 1001,
  "report_status": "SUCCEEDED",
  "overall_summary": "当前测验表现基本稳定，但仍有一个知识点需要回看。",
  "question_results": [
    {
      "question_id": 9001,
      "type": "SINGLE_CHOICE",
      "stem": "下面哪个说法最符合二分查找的前提？",
      "user_answer": "A",
      "score": 100,
      "correct": true,
      "feedback": "Correct choice.",
      "error_tags": []
    }
  ],
  "strengths": ["完整覆盖了本轮题目"],
  "weaknesses": ["仍需巩固短答表达"],
  "review_focus": ["CONSOLIDATE_CORE_CONCEPT"],
  "next_round_advice": "Start the next round with one harder scenario question.",
  "suggested_next_action": "NEXT_ROUND",
  "recommended_action": "NEXT_ROUND",
  "selected_action": "REVIEW",
  "source": "RULE"
}
```

前端可直接渲染：
- `selected_action`
- 其余字段继续复用 feedback 展示。

仅内部诊断：
- `source`

## 4. 状态码与失败场景

统一错误结构：

```json
{
  "error": "CONFLICT",
  "message": "Quiz generation is not finished yet."
}
```

### 4.1 `200 OK`

适用：
- 查询成功
- 生成请求已受理并返回当前快照
- 提交成功并返回反馈
- 下一步动作更新成功

### 4.2 `400 BAD_REQUEST`

失败场景：
- `sessionId` 非正整数
- `answers` 为空
- `question_id` 缺失
- `answer` 为空串
- `action` 为空
- `action` 非法值，如 `ADVANCE`
- 提交时缺少部分题目答案

典型 message：

```json
{
  "error": "BAD_REQUEST",
  "message": "Missing answers for question ids: [9002]"
}
```

### 4.3 `401 UNAUTHORIZED`

失败场景：
- 未登录
- token 无效

### 4.4 `404 NOT_FOUND`

失败场景：
- session 不存在
- session 下没有 training task
- quiz 不存在
- feedback 不存在

典型 message：

```json
{
  "error": "NOT_FOUND",
  "message": "Practice quiz not found."
}
```

### 4.5 `409 CONFLICT`

失败场景：
- 当前 session 对应任务不是 `TRAINING`
- quiz 还没生成完成就提交答案
- quiz 题目尚未准备好
- feedback 尚不可执行下一步动作

典型 message：

```json
{
  "error": "CONFLICT",
  "message": "Practice feedback action is not available yet."
}
```

### 4.6 `500 INTERNAL_ERROR`

失败场景：
- 服务端 JSON 反序列化内部数据失败
- LLM / 规则生成异常未被正常兜底
- 未知服务端异常

说明：
- 500 仅用于服务端故障，不建议前端做业务分支。

## 5. 前后端字段命名统一建议

建议以当前后端已输出的 snake_case 为唯一对外契约：

- path 参数统一语义名：文档写 `{id}`，实际实现和前端代码统一使用 `session_id`
- response 字段统一保留 snake_case：
  - `session_id`
  - `task_id`
  - `quiz_id`
  - `question_id`
  - `generation_status`
  - `quiz_status`
  - `report_status`
  - `question_count`
  - `answered_count`
  - `failure_reason`
  - `overall_summary`
  - `question_results`
  - `review_focus`
  - `next_round_advice`
  - `recommended_action`
  - `selected_action`

不建议：
- 前端直接混用 `sessionId` / `session_id`
- 将 `recommended_action` 改成 `nextAction`
- 将 `question_results` 改成 `results`

前端落地建议：
- API 层保留原始 snake_case
- 仅在前端 domain/store 层做一次性映射；若当前项目追求 MVP 极简，可直接全链路沿用 snake_case

字段取舍建议：
- `recommended_action` 作为主字段
- `suggested_next_action` 保留兼容，不新增消费场景
- `source` 保留，但标记为非用户展示字段

## 6. 建议的数据流顺序

### 6.1 主流程

1. 前端调用 `POST /api/sessions/{id}/quiz/generate`
2. 前端轮询 `GET /api/sessions/{id}/quiz/status`
3. 当 `generation_status=SUCCEEDED` 且 `quiz_status=QUIZ_READY` 时，调用 `GET /api/sessions/{id}/quiz`
4. 用户完成答题后，调用 `POST /api/sessions/{id}/quiz/submit`
5. 进入反馈页后，如需刷新或回访，调用 `GET /api/sessions/{id}/feedback`
6. 用户点击下一步动作，调用 `POST /api/sessions/{id}/next-action`

### 6.2 前端状态切换建议

- 出题中：看 `generation_status`
- 题目可作答：看 `quiz_status=QUIZ_READY`
- 已全部提交待反馈：提交接口返回反馈后直接进入反馈页，无需额外过渡状态
- 反馈页 CTA：
  - 默认高亮 `recommended_action`
  - 用户确认后写入 `selected_action`

### 6.3 最小前端判断规则

```json
{
  "can_poll": ["PENDING", "RUNNING"],
  "can_open_quiz": {
    "generation_status": "SUCCEEDED",
    "quiz_status": ["QUIZ_READY", "ANSWERED", "FEEDBACK_READY", "REVIEWING", "NEXT_ROUND"]
  },
  "can_submit": {
    "generation_status": "SUCCEEDED",
    "quiz_status": "QUIZ_READY"
  },
  "can_choose_next_action": {
    "quiz_status": ["FEEDBACK_READY", "REVIEWING", "NEXT_ROUND"]
  }
}
```

## 7. MVP 收敛结论

- 本轮 quiz 主链路只围绕 `session` 维度，不再要求前端显式管理 `task_id` 选择。
- 用户可见主字段收敛为：状态、题目、答题结果、反馈总结、推荐动作、已选动作。
- 诊断类字段只保留最小必要量，内部追踪字段继续留在后端表和日志中，不进入 MVP 契约。
