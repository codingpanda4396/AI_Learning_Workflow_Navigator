# AI Learning Workflow Navigator - Work Summary

## 1. Backend 工程重构与分层骨架

- 已将后端 Maven 子工程落到 `backend/` 目录。
- 主包调整为 `com.pandanav.learning`。
- 分层结构已建立并可编译运行：
  - `api`（controller / dto）
  - `application`（service / command / query / usecase）
  - `domain`（model / repository）
  - `infrastructure`（persistence / exception / config / external）
- 主启动类：`com.pandanav.learning.LearningApplication`

## 2. 基础可运行能力

- 健康检查接口：`GET /health`，返回 `{"status":"ok"}`。
- 已接入 springdoc-openapi，Swagger 可访问：`/swagger-ui.html`。
- `application.yml` 已支持数据库环境变量占位：
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `DB_DRIVER`

## 3. 数据库迁移（Flyway + PostgreSQL）

- 保持已有表结构语义不变，沿用并确认 `spec/01_db.sql` 的拆分迁移脚本：
  - `V1__create_enum_types.sql`
  - `V2__create_learning_session.sql`
  - `V3__create_concept_node_tables.sql`
  - `V4__add_session_current_node_fk.sql`
  - `V5__create_task_tables.sql`
  - `V6__create_mastery.sql`
- 启动自动迁移已开启：`spring.flyway.enabled=true`。
- 新增 DB 调试接口：`GET /debug/db`（包含 `select 1` 与 flyway info）。
- 对 V4 外键迁移补充了约束存在性检查，提升幂等性，不改变语义。

## 4. DDD-lite 骨架落地（空实现）

### Domain 模型

- `LearningSession`
- `ConceptNode`
- `LearningTask`
- `Mastery`
- `Evidence`
- 枚举：`Stage`、`TaskStatus`

### Domain 仓储接口

- `LearningSessionRepository`
- `ConceptNodeRepository`
- `TaskRepository`
- `MasteryRepository`
- `EvidenceRepository`

### Application 用例

- `StartSessionUseCase`
- `GenerateTasksUseCase`
- `GetSessionUseCase`
- 对应 service 空实现已创建（当前抛 `UnsupportedOperationException` 或使用 stub 返回）。

### Infrastructure 持久化占位

- 基于当前依赖采用 `JdbcTemplate`。
- 已新增 `Jdbc*Repository` 占位实现，方法签名齐全。

## 5. API Contract 落地（当前阶段重点）

已按 `spec/02_api_contract.md` 增加 API 层骨架，并保持 controller 仅做参数接收/校验/转发：

- `POST /api/session/create`
- `POST /api/session/{sessionId}/plan`
- `GET  /api/session/{sessionId}/overview`
- `POST /api/task/{taskId}/run`
- `POST /api/task/{taskId}/submit`

### DTO 设计

- 已补齐 session/task 的 request/response DTO。
- 使用 `@JsonProperty` 保证对外 JSON 字段遵循 contract 的 snake_case（如 `session_id`、`user_id`、`next_task`）。

### 异常与错误响应统一

- 新增异常类：
  - `BadRequestException`
  - `NotFoundException`
  - `ConflictException`
  - `InternalServerException`
- 全局异常处理：`GlobalExceptionHandler`（`@RestControllerAdvice`）。
- 错误响应格式统一为：

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request payload."
}
```

并覆盖：400 / 404 / 409 / 500。

### OpenAPI 可见性

- Controller 增加 `@Operation` / `@ApiResponses`。
- DTO 增加 `@Schema`，文档中可见接口与模型。

## 6. 当前边界说明

- 未改动数据库表结构语义。
- 未重做 Flyway 历史迁移。
- 业务逻辑目前仍为骨架/stub，后续可在 application/domain/infrastructure 按用例逐步替换为真实实现。

## 7. 验证状态

- 代码编译通过：`mvn -DskipTests compile`
- 已具备基础运行与接口联调能力（health / swagger / debug db / API contract skeleton）。
