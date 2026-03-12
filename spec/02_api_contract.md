# spec/02_api_contract.md

# API Contract (MVP / WeChat Mini Program)

## Conventions
- Base URL: `/api`
- Content-Type: `application/json; charset=utf-8`
- All timestamps are ISO-8601 strings when returned.
- `user_id` in MVP can be a mock string passed from client; later replace with `openid`.

## Enums

### Stage
- `STRUCTURE`
- `UNDERSTANDING`
- `TRAINING`
- `REFLECTION`

### TaskStatus
- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`

### SessionStatus
- `ANALYZING`
- `PLANNING`
- `LEARNING`
- `PRACTICING`
- `REPORT_READY`
- `COMPLETED`
- `FAILED`

### PracticeQuizStatus
- `GENERATING`
- `READY`
- `ANSWERING`
- `REVIEWING`
- `REPORT_READY`
- `NEXT_ROUND`
- `FAILED`

### PracticeItemStatus
- `READY`
- `ANSWERED`
- `ARCHIVED`

### ErrorTag (Evaluator output)
- `CONCEPT_CONFUSION`  # 概念混淆
- `MISSING_STEPS`      # 步骤缺失
- `BOUNDARY_CASE`      # 边界条件/特殊情况
- `TERMINOLOGY`        # 术语不准
- `SHALLOW_REASONING`  # 推理浅/论证不足
- `MEMORY_GAP`         # 记忆缺口/遗忘

### NextAction (Policy output)
- `INSERT_REMEDIAL_UNDERSTANDING`
- `INSERT_TRAINING_VARIANTS`
- `INSERT_TRAINING_REINFORCEMENT`
- `ADVANCE_TO_NEXT_NODE`
- `NOOP`

---

## 1) Create Session

### POST `/session/create`

Create a learning session.

#### Request
```json
{
  "user_id": "mock_openid_001",
  "course_id": "computer_network",
  "chapter_id": "tcp",
  "goal_text": "理解 TCP 可靠传输机制并能做题"
}
Response 200
{
  "session_id": 123
}
2) Plan Session Tasks
POST /session/{session_id}/plan

Generate and persist the task timeline for the selected chapter.

Notes

MVP uses preloaded concept_node data for the chapter.

Planner may be rule-based (non-LLM) in MVP.

Response 200
{
  "session_id": 123,
  "tasks": [
    {
      "task_id": 1001,
      "stage": "STRUCTURE",
      "node_id": 101,
      "objective": "为【三次握手】构建结构：定义/参与方/报文段字段/状态迁移/与可靠性关系",
      "status": "PENDING"
    },
    {
      "task_id": 1002,
      "stage": "UNDERSTANDING",
      "node_id": 101,
      "objective": "解释【三次握手】机制链路：每步目的、为什么需要三次、典型误区对比",
      "status": "PENDING"
    },
    {
      "task_id": 1003,
      "stage": "TRAINING",
      "node_id": 101,
      "objective": "围绕【三次握手】出 3 题（含变式）用于检测掌握度，并给出评分 rubric",
      "status": "PENDING"
    },
    {
      "task_id": 1004,
      "stage": "REFLECTION",
      "node_id": 101,
      "objective": "基于训练表现总结错因并给出下一步建议",
      "status": "PENDING"
    }
  ]
}
3) Session Overview (For Timeline Page)
GET /session/{session_id}/overview

Return session timeline, current position, next task recommendation, and mastery summary for the chapter.

Response 200
{
  "session_id": 123,
  "course_id": "computer_network",
  "chapter_id": "tcp",
  "goal_text": "理解 TCP 可靠传输机制并能做题",
  "current_node_id": 101,
  "current_stage": "UNDERSTANDING",
  "timeline": [
    {
      "task_id": 1001,
      "stage": "STRUCTURE",
      "node_id": 101,
      "status": "SUCCEEDED"
    },
    {
      "task_id": 1002,
      "stage": "UNDERSTANDING",
      "node_id": 101,
      "status": "PENDING"
    },
    {
      "task_id": 1003,
      "stage": "TRAINING",
      "node_id": 101,
      "status": "PENDING"
    }
  ],
  "next_task": {
    "task_id": 1002,
    "stage": "UNDERSTANDING",
    "node_id": 101
  },
  "mastery_summary": [
    {
      "node_id": 101,
      "node_name": "三次握手",
      "mastery_value": 0.55
    },
    {
      "node_id": 102,
      "node_name": "滑动窗口",
      "mastery_value": 0.20
    }
  ]
}
4) Run Task (Idempotent)
POST /task/{task_id}/run

Execute a task and persist its structured output.

Idempotency Rules (MUST)

If task.status == SUCCEEDED and task.output_json is not null/empty, return stored output directly.

No additional LLM calls should happen for a succeeded task.

Response 200
{
  "task_id": 1002,
  "stage": "UNDERSTANDING",
  "node_id": 101,
  "status": "SUCCEEDED",
  "output": {
    "sections": [
      {
        "type": "concepts",
        "title": "核心概念",
        "bullets": [
          "TCP 连接是逻辑连接：用状态机维护双方一致性",
          "SYN/ACK 是建立初始序列号与确认关系的关键"
        ]
      },
      {
        "type": "mechanism",
        "title": "机制链路",
        "steps": [
          "客户端发送 SYN：声明初始序列号 ISN(c)",
          "服务端回 SYN+ACK：声明 ISN(s) 并确认 ISN(c)+1",
          "客户端回 ACK：确认 ISN(s)+1，双方进入 ESTABLISHED"
        ]
      },
      {
        "type": "misconceptions",
        "title": "常见误区",
        "items": [
          "误区：两次握手就够 —— 反例：旧 SYN 重放导致半开连接",
          "误区：三次握手只为确认双方在线 —— 实际还用于序列号同步"
        ]
      },
      {
        "type": "summary",
        "title": "一分钟总结",
        "text": "三次握手的本质是双方对彼此初始序列号的确认与状态一致性建立，从而为可靠传输打基础。"
      }
    ]
  }
}
5) Submit Training Answer (Evaluate + Update + Next)
POST /task/{task_id}/submit

Submit user answer for a TRAINING task. Evaluator returns score and diagnostics; backend updates mastery and decides next step via policy.

Request
{
  "user_answer": "我认为两次握手也行，因为双方都能收到对方的消息..."
}
Response 200
{
  "task_id": 1003,
  "stage": "TRAINING",
  "node_id": 101,
  "score": 72,
  "error_tags": ["CONCEPT_CONFUSION", "MISSING_STEPS"],
  "feedback": {
    "diagnosis": "对三次握手的必要性解释缺少“旧 SYN 重放/半开连接”场景，且未完整描述序列号与确认号的同步。",
    "fixes": [
      "补充：两次握手无法防止旧 SYN 重放造成服务端资源占用",
      "按步骤说明 ISN(c)/ISN(s) 如何分别被确认"
    ]
  },
  "mastery_before": 0.55,
  "mastery_delta": 0.05,
  "mastery_after": 0.60,
  "next_action": "INSERT_TRAINING_VARIANTS",
  "next_task": {
    "task_id": 2001,
    "stage": "TRAINING",
    "node_id": 101
  }
}
Common Error Responses
400 Bad Request
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
404 Not Found
{
  "error": "NOT_FOUND",
  "message": "Session or task not found."
}
409 Conflict
{
  "error": "CONFLICT",
  "message": "Task is not in a submittable state."
}
500 Internal Server Error
{
  "error": "INTERNAL_ERROR",
  "message": "Unexpected server error."
}
