# 习题训练层最小可行后端模型（MVP）

## 1. 变更范围
本次仅引入结构化训练数据基础设施，不改现有 `task/session` 控制器主链路行为。

- 新增 Flyway migration：`V13__create_practice_tables.sql`
- 新增领域模型：`PracticeItem`、`PracticeSubmission`
- 新增枚举：`PracticeItemStatus`、`PracticeItemSource`、`PracticeQuestionType`
- 新增仓储接口：`PracticeRepository`、`PracticeSubmissionRepository`
- 新增 JDBC 实现：`JdbcPracticeRepository`、`JdbcPracticeSubmissionRepository`
- 新增服务骨架：`PracticeService`、`PracticeServiceImpl`、`PracticeTaskStats`

## 2. 表结构说明

### practice_item
用途：存储训练阶段可生成/可获取的结构化题目。

核心字段：
- 业务关联：`session_id`、`task_id`、`user_id`、`node_id`、`stage`
- 题目内容：`question_type`、`stem`、`options_json`、`standard_answer`、`explanation`、`difficulty`
- 生命周期：`source`（RULE/LLM/MANUAL）、`status`、`created_at`
- 扩展预留：`prompt_version`、`token_input`、`token_output`、`latency_ms`、`trace_id`

约束与索引：
- 外键：`learning_session`、`task`、`app_user`、`concept_node`
- 约束：
  - `stage` 仅允许 `TRAINING`
  - `source/status/question_type` 使用 `CHECK` 保证值域
- 索引：
  - `idx_practice_item_session_task_created(session_id, task_id, created_at desc, id desc)`
  - `idx_practice_item_user_node_created(user_id, node_id, created_at desc, id desc)`

### practice_submission
用途：存储题目级提交、判分、反馈与错误标签。

核心字段：
- 业务关联：`practice_item_id`、`session_id`、`task_id`、`user_id`
- 提交结果：`user_answer`、`score`、`is_correct`、`error_tags_json`、`feedback`、`submitted_at`
- 判分来源：`judge_mode`（RULE/LLM/MANUAL）
- 扩展预留：`prompt_version`、`token_input`、`token_output`、`latency_ms`、`trace_id`

约束与索引：
- 外键：
  - 直接外键到 `practice_item/learning_session/task/app_user`
  - 组合外键 `(practice_item_id, session_id, task_id, user_id)` -> `practice_item` 对应唯一键，确保提交与题目作用域一致
- 索引：
  - `idx_practice_submission_session_task_submitted(session_id, task_id, submitted_at desc, id desc)`
  - `idx_practice_submission_item_submitted(practice_item_id, submitted_at desc, id desc)`

## 3. 领域与仓储设计

### 枚举与模型
- `PracticeItemSource`：`RULE` / `LLM` / `MANUAL`
- `PracticeItemStatus`：`GENERATED` / `ACTIVE` / `ANSWERED` / `ARCHIVED`
- `PracticeQuestionType`：`SINGLE_CHOICE` / `MULTIPLE_CHOICE` / `FILL_BLANK` / `SHORT_ANSWER`

模型字段与数据库一一对应，保持当前项目 POJO 风格（非 JPA 注解）。

### Repository
按现有 `domain interface + JdbcTemplate` 风格实现：
- `PracticeRepository`
  - `save`
  - `findById`
  - `findByIdAndUserPk`（防越权）
  - `findBySessionIdAndTaskId`
  - `findBySessionIdAndTaskIdAndUserPk`
  - `updateStatus`
- `PracticeSubmissionRepository`
  - `save`
  - `findByPracticeItemId`
  - `findBySessionIdAndTaskId`
  - `findBySessionIdAndTaskIdAndUserPk`
  - `findLatestByPracticeItemIdAndUserPk`

## 4. 与现有业务对齐说明

### task 与 session 归属关系
`PracticeServiceImpl` 在题目创建/查询/提交前统一校验：
- `task` 必须存在
- `task.session_id` 必须等于请求 `session_id`
- `task.stage` 必须是 `TRAINING`

### user/session 越权访问
延续现有风格：
- 通过 `UserContextHolder.getUserId()` 获取用户主键
- 优先走 `findByIdAndUserPk` / `findBySessionIdAndTaskIdAndUserPk`
- 未通过校验时返回 `NotFoundException("Session or task not found.")`

### TRAINING 阶段绑定
通过服务层和库表约束双重保证：
- 服务层：仅允许绑定 `Stage.TRAINING` 任务
- DB 层：`practice_item.stage` `CHECK (stage = 'TRAINING')`

### node_id 对后续掌握度统计支持
`practice_item` 强制记录 `node_id`，并建立 `(user_id, node_id, created_at desc)` 索引，满足后续弱项统计/趋势计算最小需求。

## 5. 后续最值得做的一步
新增最小 API（生成/获取/分题提交）对接 `PracticeService`，并把 `SubmitTrainingAnswerService` 从“整任务提交”逐步迁移为“题目级提交聚合 + 任务级汇总返回”。
