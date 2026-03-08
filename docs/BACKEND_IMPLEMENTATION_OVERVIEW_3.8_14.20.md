# 后端当前实现综述（架构视角）

## 1. 技术基线
- 框架：Spring Boot 3.4.3（Java 17）
- Web：spring-boot-starter-web + validation + actuator
- 数据访问：Spring JDBC（非 JPA）
- 数据库迁移：Flyway（`backend/src/main/resources/db/migration`）
- 数据库：PostgreSQL
- API 文档：springdoc OpenAPI（`/v3/api-docs`、`/swagger-ui.html`）

## 2. 分层与职责
代码结构整体接近“清晰分层 + 用例驱动”：
- `api`：Controller + DTO，负责 HTTP 入参校验与响应封装
- `application`：UseCase 接口 + Service 实现，承载业务编排
- `domain`：枚举、实体模型、策略接口（NextActionPolicy / TaskObjectiveTemplateStrategy）
- `infrastructure`：JDBC Repository、配置、异常处理

关键点：
- API 层基本只依赖 UseCase/Service，不直接触库。
- 领域策略通过 `DomainPolicyConfig` 注入，实现了策略可替换。
- 持久化层采用手写 SQL，行为可控、可读性高。

## 3. 当前已实现 API
### Session / Workflow
- `POST /api/session/create`（兼容 `/api/workflow/create`）创建学习会话
- `POST /api/session/{sessionId}/plan` 生成会话任务
- `GET /api/session/{sessionId}/overview` 会话总览（时间线/下一任务/掌握度/进度）
- `GET /api/session/{sessionId}/path` 学习路径（节点状态 + mastery）
- `GET /api/session/current?user_id=...` 查询用户当前会话

### Task
- `GET /api/task/{taskId}` 任务详情
- `POST /api/task/{taskId}/run` 执行任务（生成阶段产物）
- `POST /api/task/{taskId}/submit` 提交训练答案并触发评估/策略分流

### 其他
- `GET /health` 基础健康检查
- `GET /debug/db` DB/Flyway 调试信息

## 4. 领域模型与状态机
### 核心模型
- `learning_session`：用户在某章的学习上下文（唯一键 `user_id + chapter_id`）
- `concept_node`：章节知识节点（`order_no` 定义学习顺序）
- `task`：会话下的学习任务（stage + objective）
- `task_attempt`：任务执行/提交记录（状态、输出、评分、反馈）
- `mastery`：用户-节点掌握度（0~1）
- `evidence`：评估证据留痕（JSON）

### 枚举状态
- Stage：`STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION`
- TaskStatus：`PENDING / RUNNING / SUCCEEDED / FAILED`
- NextAction：`INSERT_REMEDIAL_UNDERSTANDING / INSERT_TRAINING_VARIANTS / INSERT_TRAINING_REINFORCEMENT / ADVANCE_TO_NEXT_NODE / NOOP`

### 任务行为约束
- `Task.canRun()`：仅 `PENDING/FAILED` 可执行
- `Task.canSubmit()`：仅 `TRAINING` 阶段且 `SUCCEEDED/PENDING` 可提交

## 5. 关键业务流程（端到端）
1. 创建会话：
   - 根据 `chapterId` 找首个 `concept_node`，初始化 `learning_session.current_node_id` 与 `current_stage=STRUCTURE`
   - 若同用户同章节重复创建，依赖 DB 唯一键抛冲突

2. 规划任务：
   - 对章节内每个节点生成 4 个阶段任务（结构/理解/训练/反思）
   - objective 由 `TaskObjectiveTemplateStrategy` 模板生成

3. 执行任务：
   - `run` 会创建 `task_attempt(RUNNING)`，再按 Stage 生成 JSON 输出并回写 `SUCCEEDED`
   - 若任务已成功且有输出，直接返回缓存输出（避免重复执行）

4. 提交训练答案：
   - 仅 TRAINING 任务可提交
   - `EvaluatorService` 打分并输出 error_tags + feedback
   - `MasteryUpdateService` 以 EMA 方式更新掌握度：`after = before*0.7 + normalizedScore*0.3`
   - 记录 `task_attempt`（user_answer/score/error_tags/feedback）与 `evidence`
   - `NextActionPolicy` 决策后续动作：补理解、补训练、强化训练、推进到下一个节点

5. 推进节点：
   - `ADVANCE_TO_NEXT_NODE` 时更新 session 当前节点与阶段
   - 若下一节点 STRUCTURE 任务不存在则自动补建

## 6. 数据库迁移完成度
已存在 V1~V8：
- V1：枚举 `task_stage`、`run_status`
- V2：`learning_session`
- V3：`concept_node`、`concept_prerequisite`
- V4：`learning_session.current_node_id` 外键
- V5：`task`、`task_attempt`
- V6：`mastery`
- V7：`evidence`
- V8：`task_attempt.user_answer`

整体看，主链路涉及的数据对象均已落库，且索引与外键约束基本齐备。

## 7. 工程能力现状
- 全局异常：`GlobalExceptionHandler` 已统一 400/404/409/500
- CORS：全局放开（`allowedOriginPatterns=*`）
- 测试：
  - `TaskRunnerServiceTest`（缓存输出幂等返回）
  - `SubmitTrainingAnswerServiceTest`（策略触发补训练）
  - `CorsPreflightIntegrationTest`（预检请求）

## 8. 当前架构结论与边界
### 已具备
- 端到端闭环：建会话 -> 规划 -> 执行 -> 提交 -> 评估 -> 掌握度更新 -> 自适应分流
- 领域策略可替换，后续可演进为多策略 AB
- SQL 持久化路径清晰，便于性能与事务控制

### 主要边界/风险
- `TaskRepository` 对任务状态依赖“最近一条 task_attempt”推断，语义上是事件流式而非 task 表内强状态，后续需要明确一致性规范。
- `NextActionPolicy` 当前仅按分数阈值决策，未真正使用 `errorTags` 细化策略。
- `EvaluatorService` 规则偏启发式（目前含 TCP 三次握手特化词），通用性有限。
- CORS 目前全开放，生产环境建议收敛白名单。
- `domain.repository.LearningSessionRepository` 目前未见被应用层使用，可评估是否清理或合并接口。

## 9. 建议的下一步（按优先级）
1. 明确并固化“任务状态来源”模型（task 快照 vs attempt 事件），补充并发与回滚场景测试。
2. 将 NextAction 决策从“分数阈值”升级为“分数 + errorTags + 历史掌握度趋势”的组合策略。
3. 抽象 Evaluator 为可插拔评估器（规则版/模型版），并增加 schema 校验与回归集。
4. 引入仓储层集成测试（Testcontainers + PostgreSQL），覆盖迁移与关键 SQL。
