# Application UseCases Behavior Summary

## Scope

本次实现打通了以下接口的真实数据库流程（不再返回 mock）：

- `POST /api/session/create`
- `POST /api/session/{sessionId}/plan`
- `GET /api/session/{sessionId}/overview`

未改动 Flyway 迁移语义，未引入 LLM。

## Key Responsibilities

### API Layer

- `SessionController`
  - 仅负责参数接收、校验、调用 use case。
  - 不包含数据库访问逻辑。

### Application Layer

- `CreateSessionUseCase` + `CreateSessionService`
  - 创建会话。
  - 设定 `current_stage = STRUCTURE`。
  - 读取 chapter 下最小 `order_no` 的 `concept_node` 并写入 `current_node_id`。

- `PlanSessionTasksUseCase` + `PlanSessionTasksService`
  - 读取 session 对应 chapter 的全部 concept node（按 `order_no` 升序）。
  - 每个 node 生成 4 类任务：`STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION`。
  - 使用规则模板生成 objective。
  - 持久化至 `task` 表并返回 tasks 列表。

- `GetSessionOverviewUseCase` + `GetSessionOverviewService`
  - 查询 session。
  - 查询 timeline（task + latest task_attempt.status，缺省 `PENDING`）。
  - `next_task` 取第一个状态非 `SUCCEEDED` 的任务。
  - 查询 chapter 维度 mastery（按 node 排序）并组装 `mastery_summary`。

### Domain Layer

- 模型：`LearningSession`、`ConceptNode`、`Task`、`Mastery`
- 枚举：`Stage`、`TaskStatus`
- 仓储接口：`SessionRepository`、`ConceptNodeRepository`、`TaskRepository`、`MasteryRepository`

### Infrastructure Layer

- `JdbcLearningSessionRepository`
- `JdbcConceptNodeRepository`
- `JdbcTaskRepository`
- `JdbcMasteryRepository`

均基于 `JdbcTemplate` 实现真实 SQL 访问。

## Database Interaction Flow

### 1) Create Session

1. `concept_node` 按 `chapter_id` + `order_no` 查询第一条。
2. 插入 `learning_session`：
   - `user_id/course_id/chapter_id/goal_text`
   - `current_stage = STRUCTURE`
   - `current_node_id = first concept node id`
3. 返回 `session_id`。

### 2) Plan Session Tasks

1. 根据 `session_id` 读取 `learning_session`。
2. 按 `chapter_id` 查询 `concept_node` 列表并排序。
3. 对每个 node 生成 4 条任务并插入 `task`。
4. 返回 contract 中 `tasks` 数组（默认状态 `PENDING`）。

### 3) Session Overview

1. 读取 `learning_session`。
2. 查询 `task` 列表，并关联每个 task 最新 `task_attempt.status`（无记录则 `PENDING`）。
3. 组装 `timeline`。
4. 推导 `next_task`（第一个非 `SUCCEEDED`）。
5. 查询 `mastery`（按 user + chapter 的 concept nodes 左连接），组装 `mastery_summary`。

## Objective Rule Templates

- `STRUCTURE`：为【{conceptName}】构建结构：定义 / 组成 / 关键机制 / 与上下文关系
- `UNDERSTANDING`：解释【{conceptName}】机制链路：核心原理、因果关系、常见误区
- `TRAINING`：围绕【{conceptName}】生成训练任务，用于检测掌握度
- `REFLECTION`：基于【{conceptName}】学习结果总结错因并给出下一步建议

## Quick Test (curl)

```bash
curl -X POST http://127.0.0.1:8080/api/session/create \
  -H "Content-Type: application/json" \
  -d '{"user_id":"mock_openid_001","course_id":"computer_network","chapter_id":"tcp","goal_text":"理解 TCP 可靠传输机制并能做题"}'
```

```bash
curl -X POST http://127.0.0.1:8080/api/session/1/plan
```

```bash
curl http://127.0.0.1:8080/api/session/1/overview
```

## Notes

- 需要先确保 `concept_node` 中存在对应 chapter 的数据，否则会返回 `NOT_FOUND`。
- 本次已编译通过：`mvn -DskipTests compile`。
