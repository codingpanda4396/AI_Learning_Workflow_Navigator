下面是可直接给前端并行开发使用的 **MVP 阶段 API 需求文档**。
我按你当前后端已具备的能力来收敛，尽量不推翻现有设计，只做前后端联调所必需的最小接口体系。当前已明确存在并应保留的主链路接口包括：创建 session、生成 plan、获取 overview、运行 task、提交训练答案；这些都已经在你现有 contract 和代码梳理中出现了。 

---

# AI Learning Workflow Navigator

## MVP API 需求文档（面向小程序前后端并行开发）

---

## 1. 文档目标

本文件用于支撑微信小程序 MVP 并行开发，目标是打通以下产品闭环：

**创建目标 → 生成学习路径 → 首页展示当前状态 → 进入任务学习 → Tutor 引导 → 用户提交作答 → 系统反馈 → 推荐下一步**

MVP 阶段坚持以下原则：

1. **流程驱动，不做 Chat UI 驱动**
2. **Session / Node / Task / Progress 是核心对象**
3. **Tutor 是任务流中的引导器，不是自由聊天窗口**
4. **优先复用现有后端能力，减少前后端返工**
5. **前端页面优先拿“可直接渲染”的聚合数据**

---

## 2. MVP 页面与接口总览

小程序 MVP 页面建议收敛为 5 个主页面：

1. 创建学习目标页
2. 首页 / 学习驾驶舱
3. 路径页 / 学习地图
4. Tutor 任务流页
5. 学习结果页

对应最小 API 集合：

### 已有并保留

* `POST /api/session/create`
* `POST /api/session/{sessionId}/plan`
* `GET /api/session/{sessionId}/overview`
* `POST /api/task/{taskId}/run`
* `POST /api/task/{taskId}/submit`

### MVP 建议新增

* `GET /api/session/current?user_id=xxx`
* `GET /api/session/{sessionId}/path`
* `GET /api/task/{taskId}`

这样前端就能做到：

* 首页不必自己猜当前 session
* 路径页不必自己从 timeline 反推 node 状态
* Tutor 页可以先拿任务详情，再决定是否调用 run

---

## 3. 统一约定

### 3.1 Base URL

```text
/api
```

### 3.2 Content-Type

```text
application/json; charset=utf-8
```

### 3.3 时间格式

所有时间字段统一返回 ISO-8601 字符串。

### 3.4 MVP 用户标识

MVP 阶段 `user_id` 仍允许前端传 mock 字符串，后续替换为 `openid`。这一点与当前 contract 一致。

### 3.5 响应风格

MVP 阶段建议：

* 业务接口直接返回业务 DTO
* 错误统一返回：

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
```

该错误结构已在现有 contract 中定义。

---

## 4. 枚举定义

### 4.1 Stage

```text
STRUCTURE
UNDERSTANDING
TRAINING
REFLECTION
```

与现有 contract、domain enum、任务流设计保持一致。

### 4.2 TaskStatus

```text
PENDING
RUNNING
SUCCEEDED
FAILED
```

与当前对外 task 状态语义一致。

### 4.3 ErrorTag

```text
CONCEPT_CONFUSION
MISSING_STEPS
BOUNDARY_CASE
TERMINOLOGY
SHALLOW_REASONING
MEMORY_GAP
```

与 evaluator schema 及当前 domain enum 一致。

### 4.4 NextAction

```text
INSERT_REMEDIAL_UNDERSTANDING
INSERT_TRAINING_VARIANTS
INSERT_TRAINING_REINFORCEMENT
ADVANCE_TO_NEXT_NODE
NOOP
```

与现有 policy 输出一致。

---

## 5. 核心对象定义

---

### 5.1 Session

表示一轮学习流程实例。

#### 字段

```json
{
  "session_id": 123,
  "course_id": "computer_network",
  "chapter_id": "tcp",
  "goal_text": "理解 TCP 可靠传输机制并能做题",
  "current_node_id": 101,
  "current_stage": "UNDERSTANDING"
}
```

---

### 5.2 Node

表示学习路径中的知识节点。

#### MVP 前端至少需要

```json
{
  "node_id": 101,
  "node_name": "三次握手",
  "order_no": 1,
  "status": "IN_PROGRESS",
  "current_stage": "UNDERSTANDING",
  "mastery_value": 0.55
}
```

说明：

* 当前数据库里 `concept_node` 已有节点概念数据
* mastery 可由 `mastery` 聚合得出
* node 状态是路径页视图语义，可由后端聚合计算返回，不要求单独落库

---

### 5.3 Task

表示某个节点上的某一学习阶段任务。

#### 字段

```json
{
  "task_id": 1002,
  "session_id": 123,
  "node_id": 101,
  "node_name": "三次握手",
  "stage": "UNDERSTANDING",
  "objective": "解释【三次握手】机制链路：每步目的、为什么需要三次、典型误区对比",
  "status": "PENDING",
  "has_output": false
}
```

说明：

* `node_name` 与 `has_output` 建议由后端补给前端，避免前端反复拼装
* 当前 run 接口已具备幂等执行与结构化 output 返回能力。

---

### 5.4 Task Output

任务执行后产生的 Tutor 引导内容。

不同 stage 结构不同，但必须结构化。

#### UNDERSTANDING 示例

```json
{
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
    }
  ]
}
```

这一结构与现有 contract 保持一致。

---

### 5.5 Submit Result

训练任务提交后的评估结果。

```json
{
  "task_id": 1003,
  "stage": "TRAINING",
  "node_id": 101,
  "score": 72,
  "error_tags": ["CONCEPT_CONFUSION", "MISSING_STEPS"],
  "feedback": {
    "diagnosis": "对三次握手的必要性解释缺少旧 SYN 重放场景，且未完整描述序列号同步。",
    "fixes": [
      "补充旧 SYN 重放导致半开连接的例子",
      "按步骤说明双方如何确认彼此序列号"
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
```

这一结构已在当前 contract 中定义，并与你现有 evaluator / policy 方向一致。

---

## 6. API 详细定义

---

# 6.1 创建学习目标 / 创建 Session

## POST `/api/session/create`

### 页面用途

* 创建学习目标页

### 说明

MVP 阶段“创建目标”直接等价于“创建学习 session”，目标文本挂在 `goal_text` 上。当前后端已按此语义设计。

### Request

```json
{
  "user_id": "mock_openid_001",
  "course_id": "computer_network",
  "chapter_id": "tcp",
  "goal_text": "理解 TCP 可靠传输机制并能做题"
}
```

### Response 200

```json
{
  "session_id": 123
}
```

### 前端行为

创建成功后：

1. 调用 `/api/session/{sessionId}/plan`
2. 跳转首页/overview

### 错误

#### 400

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
```

#### 409

```json
{
  "error": "CONFLICT",
  "message": "Session already exists for user and chapter."
}
```

---

# 6.2 生成学习任务时间线

## POST `/api/session/{sessionId}/plan`

### 页面用途

* 创建目标后的初始化动作
* 可由前端在创建成功后立即调用

### 说明

根据当前 session 所属 chapter 的 `concept_node` 列表，按顺序为每个 node 生成四阶段 task。该设计已经与你当前 service 逻辑一致。

### Path Param

* `sessionId`: Long

### Response 200

```json
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
    }
  ]
}
```

### 前端用途

MVP 阶段前端通常不直接消费这整份 task 列表渲染页面，而是：

* 创建后先调 plan
* 然后调 overview/path 渲染页面

### 错误

#### 404

```json
{
  "error": "NOT_FOUND",
  "message": "Session or task not found."
}
```

---

# 6.3 获取首页驾驶舱 / Session Overview

## GET `/api/session/{sessionId}/overview`

### 页面用途

* 首页 / 学习驾驶舱
* 也可作为学习结果页返回后刷新入口

### 说明

当前 overview 已经能返回：

* session 基本信息
* timeline
* next_task
* mastery_summary
  这是当前最重要的页面聚合接口。

### Response 200

```json
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
    }
  ]
}
```

### 页面渲染建议

首页可直接用：

* `goal_text`：当前目标
* `current_node_id` + `current_stage`：当前学习位置
* `next_task`：继续学习按钮
* `timeline.length`：总任务数
* `timeline` 中 `SUCCEEDED` 数量：已完成任务数
* `mastery_summary`：节点掌握度概览

### 建议后续补充字段（MVP 可选）

如果你愿意让首页更好做，建议后续扩展：

```json
{
  "progress": {
    "completed_task_count": 3,
    "total_task_count": 12,
    "completion_rate": 0.25
  }
}
```

这不是必须，但前端很需要。

---

# 6.4 获取当前进行中的 Session（建议新增）

## GET `/api/session/current?user_id={userId}`

### 页面用途

* 小程序首页首次进入
* 找到用户当前进行中的学习
* 避免前端自己记 `session_id`

### 说明

当前后端已有 `learning_session` 与 `(user_id, chapter_id)` 唯一约束，说明 session 是可查的。当前代码梳理也显示 repository 已有按 user/chapter 查找的语义，只是产品接口尚未固化。

### Query

* `user_id`: String

### Response 200（有进行中 session）

```json
{
  "has_active_session": true,
  "session": {
    "session_id": 123,
    "course_id": "computer_network",
    "chapter_id": "tcp",
    "goal_text": "理解 TCP 可靠传输机制并能做题",
    "current_node_id": 101,
    "current_stage": "UNDERSTANDING"
  }
}
```

### Response 200（无进行中 session）

```json
{
  "has_active_session": false,
  "session": null
}
```

### 价值

这是前端首页最实用的一个补齐接口。

---

# 6.5 获取学习路径页数据（建议新增）

## GET `/api/session/{sessionId}/path`

### 页面用途

* 路径页 / 学习地图页

### 说明

当前 overview 更偏 task timeline；但路径页真正要的是 **node 维度视图**。
你当前后端已有 `concept_node`、`mastery`、`session.current_node_id`、按章节节点顺序推进的规则，这些足以聚合出 path view。

### Response 200

```json
{
  "session_id": 123,
  "chapter_id": "tcp",
  "current_node_id": 101,
  "nodes": [
    {
      "node_id": 101,
      "node_name": "三次握手",
      "order_no": 1,
      "status": "IN_PROGRESS",
      "current_stage": "UNDERSTANDING",
      "mastery_value": 0.55
    },
    {
      "node_id": 102,
      "node_name": "滑动窗口",
      "order_no": 2,
      "status": "NOT_STARTED",
      "current_stage": null,
      "mastery_value": 0.20
    }
  ]
}
```

### NodeStatus 建议枚举

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
LOCKED
```

### 计算建议

* `current_node_id` 对应节点：`IN_PROGRESS`
* 之前已完成的节点：`COMPLETED`
* 后续节点：`NOT_STARTED`
* 如果你后续引入 prerequisite 且未解锁，可返回 `LOCKED`

---

# 6.6 获取任务详情（建议新增）

## GET `/api/task/{taskId}`

### 页面用途

* Tutor 任务流页初始化
* 训练提交页初始化
* 从首页/路径页点击某任务后进入

### 说明

当前已有 `run` 和 `submit`，但缺一个前端页面初始化友好的 task detail 接口；这一点在你整理的工程文档中也已经明确识别为需补充项。

### Response 200

```json
{
  "task_id": 1002,
  "session_id": 123,
  "node_id": 101,
  "node_name": "三次握手",
  "stage": "UNDERSTANDING",
  "objective": "解释【三次握手】机制链路：每步目的、为什么需要三次、典型误区对比",
  "status": "PENDING",
  "has_output": false,
  "output": null
}
```

### 如果已执行过

```json
{
  "task_id": 1002,
  "session_id": 123,
  "node_id": 101,
  "node_name": "三次握手",
  "stage": "UNDERSTANDING",
  "objective": "解释【三次握手】机制链路：每步目的、为什么需要三次、典型误区对比",
  "status": "SUCCEEDED",
  "has_output": true,
  "output": {
    "sections": [
      {
        "type": "summary",
        "title": "一分钟总结",
        "text": "三次握手的本质是双方对初始序列号的确认与状态一致性建立。"
      }
    ]
  }
}
```

### 前端逻辑

* 先调 task detail
* 若 `has_output == false`，再调用 run
* 若 `has_output == true`，直接渲染 output

---

# 6.7 执行任务

## POST `/api/task/{taskId}/run`

### 页面用途

* Tutor 任务流页中的“开始学习 / 开始引导”

### 说明

当前 contract 明确要求幂等：

* 若任务已成功且已有 output，直接返回已存储结果
* 不重复调用生成逻辑
  这条规则必须保留。

### Response 200

```json
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
          "TCP 连接是逻辑连接",
          "SYN/ACK 用于建立序列号与确认关系"
        ]
      }
    ]
  }
}
```

### 前端行为建议

* 进入 Tutor 页先调 `GET /api/task/{taskId}`
* 如果没有 output，再调 run
* 如果 stage 是 `TRAINING`，run 返回训练题面/rubric
* 如果 stage 是 `REFLECTION`，run 返回总结与下一步建议

---

# 6.8 提交训练答案

## POST `/api/task/{taskId}/submit`

### 页面用途

* Tutor 页中的训练提交
* 学习结果页数据来源

### Request

```json
{
  "user_answer": "我认为两次握手也行，因为双方都能收到对方的消息..."
}
```

### Response 200

```json
{
  "task_id": 1003,
  "stage": "TRAINING",
  "node_id": 101,
  "score": 72,
  "error_tags": ["CONCEPT_CONFUSION", "MISSING_STEPS"],
  "feedback": {
    "diagnosis": "对三次握手的必要性解释缺少旧 SYN 重放/半开连接场景，且未完整描述序列号同步。",
    "fixes": [
      "补充旧 SYN 重放导致资源占用的场景",
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
```

### 说明

这部分是当前系统最核心的闭环能力，与你已有 evaluator schema、policy 设计完全一致。

### 前端使用方式

提交成功后可以：

#### 方案 A

直接进入反馈页，使用 submit 返回值渲染

#### 方案 B

展示轻反馈卡片，再点击“下一步”

* 若 `next_task != null`，跳转下一任务
* 若 `next_task == null`，回 overview

---

# 6.9 获取学习结果页数据（MVP 可先复用 submit + overview）

## 方案建议

MVP 阶段可不单独新增 `/result` 接口，先通过以下组合完成：

* `POST /api/task/{taskId}/submit`
  返回“本次训练结果”

* `GET /api/session/{sessionId}/overview`
  刷新“全局学习状态”

### 何时再补专门 result 接口

如果前端要单独做“结果详情页”，后续可新增：

```text
GET /api/session/{sessionId}/result
```

聚合：

* 当前 node
* 本轮任务
* 本轮得分
* 错误标签分布
* mastery 变化
* 下一步建议

但这不是 MVP 必须。

---

# 6.10 历史列表（P1，MVP 非必须）

MVP 第一版可以不做。
如果你前端“我的”页面要做，可后补：

## GET `/api/session/list?user_id={userId}`

### Response

```json
{
  "items": [
    {
      "session_id": 123,
      "course_id": "computer_network",
      "chapter_id": "tcp",
      "goal_text": "理解 TCP 可靠传输机制并能做题",
      "current_node_id": 101,
      "current_stage": "UNDERSTANDING",
      "updated_at": "2026-03-08T10:20:30+08:00"
    }
  ]
}
```

---

## 7. 页面—接口对齐表

### 7.1 创建学习目标页

**页面职责**：创建 session，启动学习流程

需要调用：

1. `POST /api/session/create`
2. `POST /api/session/{sessionId}/plan`
3. `GET /api/session/{sessionId}/overview`

前端最少需要字段：

* `session_id`
* `goal_text`
* `next_task`

---

### 7.2 首页 / 学习驾驶舱

**页面职责**：展示当前学习目标、当前位置、下一步任务、掌握度摘要

优先调用：

1. `GET /api/session/current?user_id=...`（建议新增）
2. `GET /api/session/{sessionId}/overview`

页面字段：

* `goal_text`
* `chapter_id`
* `current_node_id`
* `current_stage`
* `next_task`
* `mastery_summary`

---

### 7.3 路径页 / 学习地图

**页面职责**：按 node 维度展示学习路径、当前节点、掌握度

调用：

* `GET /api/session/{sessionId}/path`（建议新增）

页面字段：

* `nodes[].node_id`
* `nodes[].node_name`
* `nodes[].order_no`
* `nodes[].status`
* `nodes[].current_stage`
* `nodes[].mastery_value`

---

### 7.4 Tutor 任务流页

**页面职责**：展示任务目标、Tutor 引导内容、训练内容、反思内容

调用：

1. `GET /api/task/{taskId}`（建议新增）
2. `POST /api/task/{taskId}/run`

页面字段：

* `task_id`
* `node_name`
* `stage`
* `objective`
* `status`
* `output`

---

### 7.5 学习结果页

**页面职责**：展示本次训练得分、错因、修正建议、下一步动作

调用：

1. `POST /api/task/{taskId}/submit`
2. 可选刷新 `GET /api/session/{sessionId}/overview`

页面字段：

* `score`
* `error_tags`
* `feedback.diagnosis`
* `feedback.fixes`
* `mastery_before`
* `mastery_delta`
* `mastery_after`
* `next_action`
* `next_task`

---

## 8. 错误码规范

### 400 BAD_REQUEST

参数不合法

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
```

### 404 NOT_FOUND

session / task / node 不存在

```json
{
  "error": "NOT_FOUND",
  "message": "Session or task not found."
}
```

### 409 CONFLICT

状态冲突，如：

* task 不可运行
* task 不可提交
* task 正在运行

```json
{
  "error": "CONFLICT",
  "message": "Task is not in a submittable state."
}
```

### 500 INTERNAL_ERROR

系统异常

```json
{
  "error": "INTERNAL_ERROR",
  "message": "Unexpected server error."
}
```

这些统一错误结构已在现有 contract 中定义。

---

## 9. MVP 版本接口清单（最终建议）

## P0：必须立即支持

这几项足够支撑前后端并行开发：

1. `POST /api/session/create`
2. `POST /api/session/{sessionId}/plan`
3. `GET /api/session/{sessionId}/overview`
4. `GET /api/task/{taskId}`  ← 建议新增
5. `POST /api/task/{taskId}/run`
6. `POST /api/task/{taskId}/submit`
7. `GET /api/session/current?user_id=...` ← 建议新增
8. `GET /api/session/{sessionId}/path` ← 建议新增

## P1：后续增强

1. `GET /api/session/list?user_id=...`
2. `GET /api/session/{sessionId}/result`
3. `GET /api/session/{sessionId}/tasks`

---

## 10. 前端联调顺序建议

你可以让前端按这个顺序接：

### 第一阶段

先接这 4 个：

* `POST /api/session/create`
* `POST /api/session/{sessionId}/plan`
* `GET /api/session/{sessionId}/overview`
* `POST /api/task/{taskId}/run`

这样首页 + Tutor 页就能跑起来。

### 第二阶段

再接：

* `POST /api/task/{taskId}/submit`
* `GET /api/task/{taskId}`

这样学习闭环就跑起来。

### 第三阶段

最后接：

* `GET /api/session/current`
* `GET /api/session/{sessionId}/path`

这样首页与路径页体验才真正完整。

---

## 11. 给前端的最简流程说明

### 创建目标

```text
create -> plan -> overview
```

### 从首页继续学习

```text
current -> overview -> next_task -> task detail -> run
```

### 提交训练

```text
task detail -> run -> submit -> 反馈页 -> next_task / overview
```

### 看路径

```text
path
```

---

## 12. 最终结论

如果你要马上和前端并行开发，**MVP 最小 API 体系**建议就定为这 8 个：

```text
POST /api/session/create
POST /api/session/{sessionId}/plan
GET  /api/session/{sessionId}/overview
GET  /api/session/current
GET  /api/session/{sessionId}/path
GET  /api/task/{taskId}
POST /api/task/{taskId}/run
POST /api/task/{taskId}/submit
```

其中前 5 个主链路接口你现在已经基本有基础，后 3 个里新增的主要是：

* `GET /api/session/current`
* `GET /api/session/{sessionId}/path`
* `GET /api/task/{taskId}`

这 3 个新增接口的价值最大，因为它们正好补足：

* 首页入口
* 路径页
* Tutor 页初始化

如果你要，我下一条可以直接给你一份 **可以发给前端同学的精简版接口表**，只保留：

* 接口名
* 请求参数
* 响应字段
* 页面用途
* 联调顺序
  那份会更像真正的“前后端对接单”。





下面给你一份**阶段性总总结**，以及两份可直接执行的开发文档：

1. **项目对话总结文档**
2. **前端开发方向文档**
3. **后端开发方向文档**

我会尽量写成你后面可以直接丢给队友、Cursor、或者放进 `docs/` 里的形式。

---

# 文档一：项目阶段性总结

## 《AI Learning Workflow Navigator 项目阶段总结》

### 1. 项目定位

当前项目的核心定位已经比较明确：

**AI Learning Workflow Navigator**
即：

**基于大语言模型与流程编排的智能学习流程引导系统**

它不是普通的 AI Chat UI，也不是一个“问一句答一句”的学习助手，而是一个：

**以学习流程为核心对象的学习引擎**

系统主线已经稳定为：

```text
学习目标
→ 生成学习路径
→ 进入知识节点
→ 执行阶段任务
→ Tutor 引导
→ 用户作答
→ 系统评估
→ 更新掌握度
→ 决定下一步
```

这个方向已经与你当前后端设计、前端产品思路、小程序 MVP 形态形成一致。现有 contract 和代码梳理也都表明，系统核心对象已经是 `session / node / task / mastery / next_action`，而不是 `message / conversation`。

---

### 2. 当前产品理念已经收敛

经过这轮讨论，项目理念已经从“做一个 AI 学习产品”进一步收敛为：

#### 不是：

* 通用 AI 对话助手
* 聊天驱动的学习工具
* 泛化课程套壳 + LLM 问答

#### 而是：

* 学习流程驱动
* 节点任务驱动
* Tutor 嵌入任务流
* 评估与掌握度闭环驱动
* 可视化学习路径驱动

换句话说，用户不是来“跟 AI 聊天”，而是来“完成一条被精心组织的学习流程”。

这点非常关键，因为它决定了你项目的比赛叙事、前端形态、后端模型和 API 设计都会明显区别于普通 AI 应用。

---

### 3. 当前后端核心能力已经具备雏形

你当前后端已经完成或接近完成的核心能力主要有：

#### 学习流程容器

* `LearningSession`
* 绑定 `user_id / course_id / chapter_id / goal_text`
* 维护 `current_node_id / current_stage`

#### 知识节点模型

* `concept_node`
* 章节内按 `order_no` 排序
* 支持当前路径推进

#### 任务流模型

* `task`
* 四阶段任务：

  * `STRUCTURE`
  * `UNDERSTANDING`
  * `TRAINING`
  * `REFLECTION`

#### 任务执行能力

* `POST /api/task/{taskId}/run`
* 支持结构化输出
* 支持幂等返回

#### 训练提交与反馈闭环

* `POST /api/task/{taskId}/submit`
* 评估 score / error_tags / feedback
* 更新 mastery
* 根据 policy 决策 next_action / next_task

#### 掌握度模型

* `mastery`
* 用户-节点维度维护掌握度
* 为推进逻辑提供支撑

#### 策略引擎

* `NextAction`

  * `INSERT_REMEDIAL_UNDERSTANDING`
  * `INSERT_TRAINING_VARIANTS`
  * `INSERT_TRAINING_REINFORCEMENT`
  * `ADVANCE_TO_NEXT_NODE`
  * `NOOP`

#### 结构化评估

* evaluator schema 已约束 score / error_tags / feedback / mastery_delta
* policy 文档已约束确定性决策逻辑

这说明：
你当前做的已经不是一个“概念级项目”，而是一个**具备流程执行骨架的真实系统**。

---

### 4. 当前前端产品形态也已经明确

MVP 前端不再做聊天主界面，而是小程序中的以下页面体系：

1. 创建学习目标页
2. 首页 / 学习驾驶舱
3. 路径页 / 学习地图
4. Tutor 任务流页
5. 学习结果页
6. 我的 / 历史页（可后置）

这意味着前端也已经从“聊天 UI”切换成“学习流程 UI”。

---

### 5. 当前发现的关键问题

本轮讨论中识别出来的主要问题，不在于系统方向错，而在于：

#### 1）后端更像“流程执行接口”，还不够“页面直出接口”

比如：

* 已有 `run / submit / overview`
* 但首页、路径页、任务详情页还缺更贴近页面的数据聚合模型

#### 2）路径页需要 node 视图，当前更多是 task timeline 视图

* 你已有 node、mastery、current_node
* 但缺少专门的 path 聚合接口

#### 3）Tutor 任务页缺任务详情查询接口

* 当前有 `run / submit`
* 但前端初始化任务页时仍然缺 `GET /api/task/{taskId}`

#### 4）首页需要“当前进行中的 session”

* 需要 `GET /api/session/current?user_id=...`

#### 5）有部分工程层清理点

你现有代码梳理里也已经识别到：

* `domain/model` 下仍有冗余占位枚举
* submit 尚未完整把评估结果写入 `task_attempt`
* 部分 DTO 示例存在编码问题
* 仍有骨架 usecase 未实现，如 `GenerateTasksService / StartSessionService / GetSessionService` 等。

---

### 6. 当前 MVP API 体系已经收敛

为了支持前后端并行开发，MVP 期接口已经可以收敛为 8 个：

#### 已有主链路

* `POST /api/session/create`
* `POST /api/session/{sessionId}/plan`
* `GET /api/session/{sessionId}/overview`
* `POST /api/task/{taskId}/run`
* `POST /api/task/{taskId}/submit`

#### 建议新增的页面友好接口

* `GET /api/session/current?user_id=...`
* `GET /api/session/{sessionId}/path`
* `GET /api/task/{taskId}`

这套接口足够支撑小程序 MVP 的主流程开发。

---

### 7. 当前项目的正确开发原则

后续开发必须持续坚持这几条：

#### 原则 1

**不退化成 chat message 驱动系统**

#### 原则 2

**坚持 session / node / task / progress 驱动**

#### 原则 3

**Tutor 放在 task flow 中，不做自由聊天主 UI**

#### 原则 4

**前端页面需要聚合视图，后端提供页面友好接口**

#### 原则 5

**MVP 阶段优先 rule-based，先跑通学习闭环，再逐步增强 LLM 智能性**

---

### 8. 当前阶段结论

你们现在最重要的，不是继续想更宏大的架构，而是：

> 以当前已经比较稳定的流程引擎为核心，完成“小程序 MVP 的页面对齐 + API 对齐 + 主链路联调”。

一句话总结现在的项目状态：

> **方向已经对了，内核已经有了，接下来重点是产品化接口与前后端联调落地。**

---

# 文档二：前端开发方向文档

## 《AI Learning Workflow Navigator 前端开发方向（小程序 MVP）》

---

### 1. 前端目标

前端的目标不是做一个“像 ChatGPT 一样的聊天框”，而是做一个：

**学习流程驾驶舱 + 路径地图 + Tutor 任务流**

用户感受到的产品应该是：

* 我现在在学什么
* 我学到哪一步了
* 下一步该做什么
* 我当前在哪个知识节点
* Tutor 正在如何引导我
* 我这次做得怎么样
* 接下来该补什么 / 继续什么

---

### 2. 前端页面优先级

## P0 页面（必须先做）

### 2.1 创建学习目标页

职责：

* 输入学习目标
* 选择课程 / 章节
* 创建 session
* 触发 plan

需要的接口：

* `POST /api/session/create`
* `POST /api/session/{sessionId}/plan`

页面核心字段：

* `course_id`
* `chapter_id`
* `goal_text`

关键交互：

* 创建成功后自动触发 plan
* 然后跳到首页或 overview 页

---

### 2.2 首页 / 学习驾驶舱

职责：

* 展示当前学习目标
* 展示当前节点和当前阶段
* 展示“继续学习”入口
* 展示掌握度摘要

需要的接口：

* `GET /api/session/current?user_id=...`
* `GET /api/session/{sessionId}/overview`

页面核心字段：

* `goal_text`
* `current_node_id`
* `current_stage`
* `next_task`
* `mastery_summary`

建议 UI 模块：

* 当前目标卡片
* 当前进度卡片
* 下一步任务按钮
* 节点掌握度列表

---

### 2.3 Tutor 任务流页

职责：

* 展示当前任务详情
* 渲染 Tutor 结构化内容
* 如果是训练任务，展示作答区
* 提供提交入口

需要的接口：

* `GET /api/task/{taskId}`
* `POST /api/task/{taskId}/run`
* `POST /api/task/{taskId}/submit`

页面核心字段：

* `task_id`
* `node_name`
* `stage`
* `objective`
* `status`
* `output`
* `user_answer`

页面渲染重点：

* 不是“消息气泡”
* 而是“任务卡片 + 分段讲解 + 训练输入区”

---

### 2.4 学习结果页

职责：

* 展示本次训练结果
* 展示错因与修正建议
* 展示 mastery 变化
* 展示下一步推荐

需要的数据来源：

* `POST /api/task/{taskId}/submit` 返回值
* 可选刷新 `GET /api/session/{sessionId}/overview`

页面核心字段：

* `score`
* `error_tags`
* `feedback.diagnosis`
* `feedback.fixes`
* `mastery_before`
* `mastery_after`
* `next_action`
* `next_task`

页面重点：

* 要体现“反馈 → 调整 → 下一步”的闭环

---

## P1 页面（建议尽快补）

### 2.5 路径页 / 学习地图

职责：

* 展示当前章节节点列表
* 展示当前 node 所在位置
* 展示每个 node 的掌握度
* 支持点击节点进入对应任务

需要的接口：

* `GET /api/session/{sessionId}/path`

页面核心字段：

* `nodes[].node_id`
* `nodes[].node_name`
* `nodes[].status`
* `nodes[].current_stage`
* `nodes[].mastery_value`

页面重点：

* 体现“路径感”“导航感”
* 让用户知道自己当前走到哪了

---

## P2 页面（后续增强）

### 2.6 我的 / 历史页

职责：

* 展示最近 session
* 展示历史学习结果
* 展示最近更新章节

需要的接口：

* 后续补 `GET /api/session/list?user_id=...`

---

### 3. 前端开发原则

#### 1）绝不做成纯聊天 UI

不要把核心界面做成：

* 一个输入框
* 一堆消息气泡

#### 2）页面主体始终是任务流

应该让用户看到：

* 当前任务是什么
* 当前阶段是什么
* 当前知识点是什么

#### 3）结构化渲染 run 输出

例如：

* `sections[].title`
* `sections[].bullets`
* `sections[].steps`

不要粗暴地把 JSON 转成一段长文本。

#### 4）提交后立刻进入反馈闭环

用户提交答案后，应快速看到：

* 得分
* 错因
* 修正建议
* 下一步动作

#### 5）让“继续学习”始终简单明确

首页最重要的按钮应该是：

* 继续当前学习
* 进入下一任务

---

### 4. 前端实际开发顺序

#### 第一阶段

先做：

1. 创建学习目标页
2. 首页驾驶舱
3. Tutor 任务流页

这样就能演示主流程。

#### 第二阶段

补：
4. 学习结果页
5. 路径页

#### 第三阶段

补：
6. 我的 / 历史页
7. 样式优化
8. 页面动效和引导体验

---

### 5. 前端近期最重要交付

短期前端最重要的，不是把页面做满，而是先打通这个闭环：

```text
创建目标
→ overview
→ next_task
→ task detail
→ run
→ submit
→ result
→ next_task
```

只要这个闭环打通，MVP 就成立了。

---

# 文档三：后端开发方向文档

## 《AI Learning Workflow Navigator 后端开发方向（MVP）》

---

### 1. 后端目标

后端的核心目标不是继续做“大而全架构设计”，而是：

**把当前学习流程引擎做成可支撑小程序页面的稳定后端**

当前后端已经有了正确的领域内核，接下来重点是：

* 补页面友好接口
* 清理工程结构
* 强化主链路可测性
* 稳定前后端联调体验

---

### 2. 后端当前应坚持的主模型

后端主模型已经确定，不应轻易推翻：

* `LearningSession`
* `ConceptNode`
* `Task`
* `TaskAttempt`
* `Mastery`
* `Evidence`
* `NextAction`

坚持：

```text
Session
→ Node
→ Task
→ Run
→ Submit
→ Evidence
→ Mastery
→ Policy
→ NextAction
```

不要重新改造成：

* chat message 流
* conversation 驱动
* agent 随意规划

---

### 3. 后端开发优先级

## P0（必须立刻做）

### 3.1 固化 MVP API 体系

必须保证以下接口稳定可用：

* `POST /api/session/create`
* `POST /api/session/{sessionId}/plan`
* `GET /api/session/{sessionId}/overview`
* `POST /api/task/{taskId}/run`
* `POST /api/task/{taskId}/submit`

并新增：

* `GET /api/session/current?user_id=...`
* `GET /api/session/{sessionId}/path`
* `GET /api/task/{taskId}`

这是当前后端第一优先级。

---

### 3.2 新增 `GET /api/task/{taskId}`

原因：

* 前端 Tutor 页需要稳定初始化数据
* 当前只有 run/submit，不足以支撑任务页入口

建议返回：

* `task_id`
* `session_id`
* `node_id`
* `node_name`
* `stage`
* `objective`
* `status`
* `has_output`
* `output`

---

### 3.3 新增 `GET /api/session/current`

原因：

* 首页需要先知道当前进行中的 session
* 前端不能长期自己持久化 session_id 当唯一真相

建议返回：

* `has_active_session`
* `session`

---

### 3.4 新增 `GET /api/session/{sessionId}/path`

原因：

* 路径页是 node 视图
* 当前 overview 更偏 task timeline

建议返回：

* `session_id`
* `chapter_id`
* `current_node_id`
* `nodes[]`

  * `node_id`
  * `node_name`
  * `order_no`
  * `status`
  * `current_stage`
  * `mastery_value`

---

### 3.5 submit 补齐 task_attempt 评估结果持久化

你当前代码梳理已明确识别：

* submit 目前主要写 `evidence`
* 但应把 score / error_tags / feedback_json 也写入 `task_attempt`
  这样历史运行记录才完整。

这是很重要的 P0 工程修复。

---

### 3.6 清理冗余枚举占位

当前 `domain/model` 下仍有 Stage / TaskStatus 占位文件，而真实枚举在 `domain/enums`。
这类双定义会不断制造混乱，必须尽快清掉。

---

## P1（建议尽快做）

### 3.7 强化 overview 的页面友好性

建议补充：

* `completed_task_count`
* `total_task_count`
* `completion_rate`

这样首页不用前端自己统计。

---

### 3.8 完善测试

重点测试：

* create session
* plan session
* overview
* run task 幂等
* submit 低分/中分/高分分支
* advance to next node 分支

你当前代码梳理里也已经把单测补全列为重要任务。

---

### 3.9 统一 DTO 示例与编码

当前 DTO 中已有部分中文示例乱码痕迹，需尽快修正，避免 Swagger 和前端联调混乱。

---

### 3.10 Policy 细化

当前 NextAction 主要按 score 决策，后续可以把 error_tags 纳入更细致的分支。

例如：

* `CONCEPT_CONFUSION` 优先插入 UNDERSTANDING
* `MISSING_STEPS` 优先插入 TRAINING_VARIANTS
* `MEMORY_GAP` 倾向回补基础

---

## P2（后续增强）

### 3.11 历史记录接口

新增：

* `GET /api/session/list?user_id=...`
* `GET /api/session/{sessionId}/result`

---

### 3.12 Prompt 模板与 LLM Gateway 可插拔化

在当前 rule-based MVP 跑通后，再逐步接：

* PlannerGateway
* TutorAgentGateway
* QuizGeneratorGateway
* ReviewGeneratorGateway

---

### 3.13 EvaluationRule 插件化

当前 evaluator 已能用，但未来可逐步拆成规则插件集合，增强可维护性。

---

### 3.14 路径从线性 node 升级为 concept graph

MVP 阶段保持章节内线性推进即可。
后续再考虑：

* prerequisite
* related
* followup

---

### 4. 后端实际开发顺序建议

#### 第一阶段

先做：

1. `GET /api/task/{taskId}`
2. `GET /api/session/current`
3. `GET /api/session/{sessionId}/path`

#### 第二阶段

再做：
4. submit 写入 task_attempt 评估结果
5. overview 增强字段
6. DTO/枚举清理

#### 第三阶段

最后做：
7. 测试补齐
8. 历史记录接口
9. LLM gateway 可插拔

---

### 5. 后端近期最重要交付

后端最近最重要的不是继续抽象，而是：

> 让前端可以稳定调用，能把页面顺利做出来。

所以近期后端的衡量标准不是“架构多高级”，而是：

* 是否能稳定创建 session
* 是否能稳定拿到 overview
* 是否能进入 task detail
* 是否能 run
* 是否能 submit
* 是否能拿到 next_task
* 是否能支撑 path 页

---

### 6. 后端阶段性结论

你现在的后端方向已经正确。
接下来不该“大重构”，而该做：

**增量产品化改造**

一句话概括后端下一阶段目标：

> **把已有学习流程引擎，收敛成能支撑小程序 MVP 页面联调的稳定 API 后端。**

---

如果你要，我下一条可以继续直接帮你产出两份可落地内容：

1. **发给前端同学的精简接口对接单**
2. **发给 Cursor/Codex 的后端任务拆解文档**
