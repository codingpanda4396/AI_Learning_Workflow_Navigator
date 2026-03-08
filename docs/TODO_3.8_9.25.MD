````md
# 后端开发 TODO（MVP / Cursor + CodexApp 版）

> 项目：AI Learning Workflow Navigator  
> 角色：后端负责人  
> 技术栈：Spring Boot 3 / JDK 17 / PostgreSQL / Flyway  
> 当前目标：支撑小程序 MVP 前后端并行联调

---

## 一、当前阶段总目标

后端当前不是继续做“大重构”，而是：

**把现有学习流程引擎收敛成可稳定支撑前端页面的 API 后端。**

当前必须围绕这条主链路推进：

```text
create
→ plan
→ overview
→ task detail
→ run
→ submit
→ next_task
````

---

## 二、P0：本周必须完成

---

### TODO 1：新增 `GET /api/task/{taskId}`

#### 目标

为 Tutor 页提供任务初始化接口。

#### 为什么要做

前端进入任务页时，不能直接盲调 `run`，要先知道：

* 任务属于哪个 session
* 当前 stage 是什么
* objective 是什么
* 是否已经有 output
* 如果已执行过，直接返回已有 output

#### 接口定义

```http
GET /api/task/{taskId}
```

#### 期望响应

```json
{
  "task_id": 1002,
  "session_id": 123,
  "node_id": 101,
  "node_name": "三次握手",
  "stage": "UNDERSTANDING",
  "objective": "解释三次握手机制链路",
  "status": "PENDING",
  "has_output": false,
  "output": null
}
```

#### 实现要点

* 查询 task
* 查询 concept_node 获取 `node_name`
* 判断 `output_json` 是否存在
* 映射为 `TaskDetailResponse`

#### 验收标准

* task 存在时返回 200
* task 不存在时返回 404
* 已有 output 时 `has_output=true`
* 无 output 时 `has_output=false`

#### Codex Prompt

```text
Implement endpoint GET /api/task/{taskId}.

Requirements:
- Add controller method in TaskController
- Add application service TaskQueryService
- Query task by id
- Query concept node by task.node_id to get node_name
- Build response DTO with:
  task_id
  session_id
  node_id
  node_name
  stage
  objective
  status
  has_output
  output

Rules:
- has_output = true if output_json is not null and not empty
- return 404 if task not found
- keep code style consistent with current layered architecture
```

---

### TODO 2：新增 `GET /api/session/current?user_id=...`

#### 目标

给首页提供“当前进行中的 session”。

#### 为什么要做

前端首页不能总靠本地缓存 `session_id`。
需要后端告诉前端：当前用户有没有正在学习的 session。

#### 接口定义

```http
GET /api/session/current?user_id=mock_openid_001
```

#### 期望响应（有 session）

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

#### 期望响应（无 session）

```json
{
  "has_active_session": false,
  "session": null
}
```

#### 实现要点

* 根据 `user_id` 查询当前有效 session
* MVP 可先取最近更新的一条 session
* 返回聚合 DTO，不要把 entity 直接透出

#### 验收标准

* 用户存在活动 session 时返回完整对象
* 用户无 session 时返回 `has_active_session=false`
* 不抛 500

#### Codex Prompt

```text
Implement endpoint GET /api/session/current?user_id={userId}.

Requirements:
- Add controller method in SessionController
- Add application service SessionQueryService
- Query the latest active learning session by user_id
- Return DTO:
  has_active_session
  session

Session fields:
  session_id
  course_id
  chapter_id
  goal_text
  current_node_id
  current_stage

Rules:
- if not found, return has_active_session=false and session=null
- do not expose persistence entity directly
- keep code style consistent with current project
```

---

### TODO 3：新增 `GET /api/session/{sessionId}/path`

#### 目标

给“学习地图 / 路径页”提供 node 维度聚合数据。

#### 为什么要做

`overview` 更偏向 timeline/task 视图。
前端路径页真正要的是：

* 当前章节有哪些 node
* 当前学到哪个 node
* 每个 node 的 mastery
* 每个 node 的状态

#### 接口定义

```http
GET /api/session/{sessionId}/path
```

#### 期望响应

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

#### NodeStatus 规则（MVP）

* `current_node_id` 对应节点：`IN_PROGRESS`
* 当前节点之前：`COMPLETED`
* 当前节点之后：`NOT_STARTED`

#### 实现要点

* 查询 session
* 查询 chapter 下全部 concept_node，按 `order_no` 排序
* 查询用户在这些 node 上的 mastery
* 聚合计算 node status
* 映射为 `SessionPathResponse`

#### 验收标准

* 返回 nodes 有序
* `current_node_id` 对应节点状态正确
* mastery 缺失时给默认值（如 0 或 null，按你当前设计统一）
* session 不存在返回 404

#### Codex Prompt

```text
Implement endpoint GET /api/session/{sessionId}/path.

Requirements:
- Add controller method in SessionController
- Add application service SessionPathQueryService
- Load session by id
- Load concept nodes by chapter_id ordered by order_no
- Load mastery records by user_id and node_ids
- Build response:
  session_id
  chapter_id
  current_node_id
  nodes[]

Node fields:
  node_id
  node_name
  order_no
  status
  current_stage
  mastery_value

Status rules:
- current node -> IN_PROGRESS
- nodes before current node -> COMPLETED
- nodes after current node -> NOT_STARTED

Return 404 if session not found.
```

---

### TODO 4：补全 `submit` 的 `task_attempt` 持久化

#### 目标

让训练提交后的评估结果真正落库。

#### 为什么要做

现在如果只更新 `mastery` 或只写 `evidence`，那么：

* 历史训练记录不完整
* 后续无法做结果页/历史页
* 无法回放用户答案与诊断结果

#### 必须落库的字段

* `task_id`
* `user_answer`
* `score`
* `error_tags`
* `feedback_json`
* `created_at`

#### 实现要点

* submit 时在同一事务中：

  * 保存 `task_attempt`
  * 保存 `evidence`（如果已有机制）
  * 更新 `mastery`
  * 计算并返回 `next_action / next_task`

#### 验收标准

* submit 后数据库中存在对应 `task_attempt`
* score / error_tags / feedback_json 可查
* 失败时事务回滚
* 不出现“前端看到结果但数据库没记录”的情况

#### Codex Prompt

```text
Update submit flow for POST /api/task/{taskId}/submit.

Requirements:
- Persist evaluation result into task_attempt table
- Save fields:
  task_id
  user_answer
  score
  error_tags
  feedback_json
  created_at
- Keep mastery update in the same transaction
- Keep next_action / next_task logic unchanged unless necessary
- Ensure rollback on failure

Goal:
After submit, evaluation results must be queryable from database history.
```

---

## 三、P1：联调前建议补齐

---

### TODO 5：增强 `overview` 响应，补 `progress`

#### 目标

减少前端自己统计任务进度。

#### 建议增加字段

```json
{
  "progress": {
    "completed_task_count": 3,
    "total_task_count": 12,
    "completion_rate": 0.25
  }
}
```

#### 为什么值得做

首页一定会展示：

* 已完成多少
* 总共有多少
* 当前进度多少

这类字段不该让前端硬算。

#### Codex Prompt

```text
Enhance GET /api/session/{sessionId}/overview response.

Add field:
progress {
  completed_task_count
  total_task_count
  completion_rate
}

Rules:
- completed count = tasks with status SUCCEEDED
- total count = total tasks in session timeline
- completion_rate = completed / total, use decimal
- keep existing response fields unchanged
```

---

### TODO 6：确认 `run` 幂等逻辑绝对成立

#### 目标

避免重复生成 Tutor 内容。

#### 必须满足

* 若任务已经 `SUCCEEDED` 且已有 output
* 再次调用 `run`
* 直接返回已有 output
* 不重复生成

#### 验收标准

* 同一 task 多次调用 `run`，结果一致
* 不新增重复 output
* 不破坏任务状态

#### Codex Prompt

```text
Review and enforce idempotency for POST /api/task/{taskId}/run.

Rules:
- if task status is SUCCEEDED and output already exists, return stored output directly
- do not regenerate output
- do not create duplicate records
- keep response shape unchanged
```

---

### TODO 7：清理重复枚举/占位类

#### 目标

减少工程混乱。

#### 重点检查

* 是否同时存在：

  * `domain/model/Stage`
  * `domain/enums/Stage`
* 是否存在重复的 `TaskStatus`
* 是否有旧 DTO / 占位类已无实际引用

#### 验收标准

* Stage 只有一个权威定义
* TaskStatus 只有一个权威定义
* IDE 全局搜索不再混乱
* 编译与测试通过

#### Codex Prompt

```text
Refactor duplicated enums and placeholder classes.

Tasks:
- find duplicated Stage / TaskStatus definitions
- keep only one authoritative enum per concept
- remove or replace obsolete placeholder classes
- update imports safely
- ensure project still compiles and tests pass
```

---

### TODO 8：统一错误返回结构

#### 目标

让前端联调稳定。

#### 错误格式统一为

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
```

#### 重点场景

* 参数错误 → 400
* session/task 不存在 → 404
* 状态冲突 → 409
* 未知异常 → 500

#### Codex Prompt

```text
Standardize API error response format across controllers.

Target format:
{
  "error": "ERROR_CODE",
  "message": "Human readable message"
}

Map common cases:
- bad request -> 400
- not found -> 404
- conflict -> 409
- internal error -> 500

Implement using global exception handler if not present.
```

---

## 四、P2：MVP 跑通后再做

---

### TODO 9：补 `GET /api/session/list?user_id=...`

#### 目标

给“我的 / 历史页”提供 session 列表。

#### 响应示例

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

### TODO 10：补 `GET /api/session/{sessionId}/result`

#### 目标

给结果详情页做专门聚合接口。

#### 可聚合内容

* 本轮任务
* 本轮 score
* error_tags
* feedback
* mastery 变化
* next_action
* next_task

---

### TODO 11：补集成测试 / 接口测试

#### 必测链路

* create session
* plan session
* get overview
* get task detail
* run task（幂等）
* submit task
* path query
* current session query

#### 建议

优先做 Spring Boot integration test，不要只写单元测试。

---

### TODO 12：为 Swagger / OpenAPI 补文档说明

#### 目标

让前端联调更省心。

#### 至少补充

* 接口说明
* 请求示例
* 响应示例
* 枚举含义
* 错误码说明

---

## 五、推荐开发顺序（非常重要）

严格按这个顺序做，不要乱：

### 第 1 批

1. `GET /api/task/{taskId}`
2. `GET /api/session/current`
3. `GET /api/session/{sessionId}/path`
4. submit 落 `task_attempt`

### 第 2 批

5. overview 增强 progress
6. run 幂等检查
7. 错误返回统一
8. 枚举清理

### 第 3 批

9. session list
10. result 聚合
11. 测试补齐
12. Swagger 文档

---

## 六、推荐 Git 提交粒度

建议一项一个 commit。

### 推荐 commit message

```text
feat: add task detail query api
feat: add current session api
feat: add session path api
fix: persist task attempt on submit
feat: add progress summary to overview
refactor: unify stage and task status enums
fix: standardize api error responses
test: add integration tests for mvp workflow
```

---

## 七、每日执行模板（你现在可以直接照着干）

---

### Day 1

* [ ] 实现 `GET /api/task/{taskId}`
* [ ] 本地自测
* [ ] 提交 commit

### Day 2

* [ ] 实现 `GET /api/session/current`
* [ ] 实现 `GET /api/session/{sessionId}/path`
* [ ] 本地联调
* [ ] 提交 commit

### Day 3

* [ ] 修复 submit 持久化 `task_attempt`
* [ ] 补事务检查
* [ ] 提交 commit

### Day 4

* [ ] overview 增强 progress
* [ ] run 幂等复查
* [ ] 错误处理统一
* [ ] 提交 commit

### Day 5

* [ ] 补接口测试
* [ ] 输出联调文档
* [ ] 和前端联调

---

## 八、你当前的判断标准

后端当前做得好不好，不看“抽象是否高级”，只看这几个问题：

* [ ] 前端能不能稳定创建 session
* [ ] 前端能不能拿到当前进行中的 session
* [ ] 前端能不能看到学习路径
* [ ] 前端能不能进入 task detail
* [ ] 前端能不能 run
* [ ] 前端能不能 submit
* [ ] submit 结果是否落库
* [ ] 是否能拿到 next_task

只要这些都成立，你这个后端 MVP 就是成功的。

---

## 九、最终结论

你现在后端的核心任务只有一句话：

**不要大重构，做增量产品化改造。**

把现有学习流程引擎，补成可支撑小程序页面联调的稳定 API 系统。

---

```

你要的话，我下一条可以直接继续给你：

**《Cursor 专用任务拆解 prompt 包》**

也就是把上面每个 TODO 再拆成一条条能直接喂给 CodexApp 的 prompt。
```
