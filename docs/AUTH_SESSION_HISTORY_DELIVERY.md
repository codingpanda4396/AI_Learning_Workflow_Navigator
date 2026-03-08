下面是你这份文档的 **完整中文翻译版**（保持原有结构与技术语义，适合直接放入项目文档 / README / 技术设计文档）。

---

# Auth + Session History 实现（阶段 A）

## 1. 范围（Scope）

本次交付在 **不重写当前 session/task/stage 业务流程** 的前提下，实现 Stage A 能力：

新增能力：

* 应用内用户系统（`app_user`）
* JWT 鉴权机制
* 请求用户上下文
* Session 所有权校验
* Session 历史记录 API
* 学习事件持久化（`learning_event`）

不包含（后续阶段实现）：

* OAuth 登录
* 微信登录
* 企业 SSO

---

# 2. 数据库迁移（Flyway）

## 新增 Migration

```
backend/src/main/resources/db/migration/V11__add_user_and_session_history.sql
```

## 数据库变更

### 1）新增表 `app_user`

字段：

* `id`
* `username`
* `password_hash`
* `created_at`
* `last_login_at`
* `status`

索引：

* `username` 唯一索引

---

### 2）修改表 `learning_session`

新增字段：

* `user_pk`
* `status`
* `completed_at`
* `last_active_at`

删除旧约束：

```
UNIQUE(user_id, chapter_id)
```

新增索引：

```
(user_pk, created_at desc)
```

兼容策略：

* 保留旧字段 `user_id`
* 用于兼容旧逻辑

---

### 3）新增表 `learning_event`

字段：

* `id`
* `session_id`
* `user_id`
* `event_type`
* `event_data`
* `created_at`

索引：

* `session_id`
* `user_id`

---

# 3. Auth 实现

新增包：

```
backend/src/main/java/com/pandanav/learning/auth
```

## 新增类

* `AuthController`
* `AuthService`
* `JwtUtil`
* `AuthInterceptor`
* `UserContextHolder`
* `AuthProperties`
* `UnauthorizedException`

---

## JWT 设计

JWT payload：

```
userId
```

默认过期时间：

```
7 天
```

配置项：

```
app.auth.jwt-secret
app.auth.token-expire-days
```

---

# 4. 拦截器注册

配置文件：

```
AuthWebMvcConfig.java
```

路径：

```
backend/src/main/java/com/pandanav/learning/infrastructure/config/AuthWebMvcConfig.java
```

拦截规则：

拦截：

```
/api/**
```

排除：

```
/api/auth/**
/api/health
/api/db/**
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
/actuator/**
```

---

# 5. 新增 / 更新 API

---

# Auth API

注册

```
POST /api/auth/register
```

登录

```
POST /api/auth/login
```

获取当前用户

```
GET /api/users/me
```

---

# Session API

## 更新 API

### 创建 Session

```
POST /api/session/create
```

变更：

* 不再依赖请求参数 `user_id`
* 用户从 **JWT Context** 获取
* 写入 `learning_session.user_pk`
* 写入 `learning_event`

事件：

```
SESSION_CREATED
```

---

### 获取当前 Session

```
GET /api/session/current
```

逻辑：

* 用户来自 JWT Context
* 查询条件：

```
user_pk + status='ACTIVE'
```

---

# 新增 API

---

## Session 历史记录

```
GET /api/session/history
```

参数：

```
page
page_size
status
```

排序：

```
last_active_at desc
```

---

## Session 详情

```
GET /api/session/{sessionId}
```

特点：

* 带 **ownership 校验**

---

## 恢复学习 Session

```
POST /api/session/{sessionId}/resume
```

逻辑：

1️⃣ 校验 session ownership

2️⃣ 更新：

```
last_active_at
```

3️⃣ 写入 `learning_event`

事件：

```
SESSION_RESUMED
```

---

# 6. Ownership 与安全控制

所有通过 `id` 查询 session/task 的接口均进行所有权校验：

包括：

* session overview
* session path
* session plan
* session detail
* session resume
* task detail
* task run
* task submit

实现方式：

### Repository 层

查询条件：

```
id + user_pk
```

### Service 层

业务执行前进行：

```
ownership check
```

---

# 7. 兼容策略

为了避免破坏当前系统逻辑：

保持以下策略：

### 1

现有：

```
session / task / stage
```

编排逻辑保持不变

---

### 2

保留旧字段：

```
learning_session.user_id
```

并继续填充：

```
user-{userPk}
```

---

### 3

保留 **无认证调用模式**

用于：

```
非 Web 层单元测试
```

---

# 8. 主要文件变更

---

# 新增文件

```
backend/src/main/java/com/pandanav/learning/auth/*
```

```
backend/src/main/java/com/pandanav/learning/api/controller/SessionHistoryController.java
```

```
backend/src/main/java/com/pandanav/learning/application/service/SessionHistoryService.java
```

```
backend/src/main/java/com/pandanav/learning/api/dto/session/SessionHistoryResponse.java
```

```
backend/src/main/java/com/pandanav/learning/api/dto/session/SessionHistoryItemResponse.java
```

```
backend/src/main/java/com/pandanav/learning/domain/model/AppUser.java
```

```
backend/src/main/java/com/pandanav/learning/domain/model/LearningEvent.java
```

```
backend/src/main/java/com/pandanav/learning/domain/repository/AppUserRepository.java
```

```
backend/src/main/java/com/pandanav/learning/domain/repository/LearningEventRepository.java
```

```
backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcAppUserRepository.java
```

```
backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcLearningEventRepository.java
```

```
backend/src/main/java/com/pandanav/learning/infrastructure/config/AuthWebMvcConfig.java
```

```
backend/src/main/resources/db/migration/V11__add_user_and_session_history.sql
```

---

# 更新文件（核心）

```
backend/pom.xml
```

```
backend/src/main/resources/application.yml
```

```
SessionController.java
```

```
CreateSessionService.java
```

```
GetCurrentSessionService.java
```

```
GetSessionOverviewService.java
```

```
GetSessionPathService.java
```

```
PlanSessionTasksService.java
```

```
TaskQueryService.java
```

```
TaskRunnerService.java
```

```
SubmitTrainingAnswerService.java
```

```
LearningSession.java
```

```
SessionRepository.java
```

```
TaskRepository.java
```

```
JdbcLearningSessionRepository.java
```

```
JdbcTaskRepository.java
```

```
GlobalExceptionHandler.java
```

---

# 9. 构建与测试状态

在 `backend` 目录执行：

### 构建

```
mvn clean package -DskipTests
```

结果：

```
SUCCESS
```

---

### 完整构建

```
mvn clean package
```

编译：

```
通过
```

测试：

```
失败
```

原因：

```
PostgreSQL 外部连接失败
SQL State 08001
```

原因说明：

```
integration test 环境无法连接数据库
```

---

# 10. 前端集成说明

所有受保护接口需要：

### Header

```
Authorization: Bearer <token>
```

---

前端需要修改：

原流程：

```
create session -> 传 user_id
```

新流程：

```
login -> JWT
create session -> 从 token 获取 user
```

---

如果你愿意，我可以 **顺便帮你把这份文档升级成一个真正专业的项目文档版本**（适合比赛 / 开源 / 技术展示），例如增加：

* Architecture Diagram
* Auth Flow Diagram
* Session Lifecycle
* Event Model
* ER Diagram

这样会 **非常像一个成熟 SaaS 的 backend design doc**。
