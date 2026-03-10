# AI Tutor MVP 实现总结

## 1. 目标达成情况

本次在“任务详情页”完成了 MVP 级 AI Tutor 面板，实现了从“任务展示器”到“AI 学习助手”的最小闭环：

- 在任务详情页新增 AI Tutor 区域。
- 用户可输入问题并发送。
- 后端提供 Tutor Chat API（GET/POST）。
- 采用可扩展的 Provider 抽象，当前接入 Mock 实现。
- Tutor 对话按 `sessionId + taskId + userId` 绑定并持久化。
- 前端支持对话历史、发送中状态、失败重试、空状态引导。
- UI 延续现有暗色蓝色风格，无破坏原有任务详情布局。

## 2. 后端实现

### 2.1 数据库与迁移

新增 Flyway 迁移：

- `V12__create_tutor_message_table.sql`

核心内容：

- 新表 `tutor_message`：
  - 字段：`id/session_id/task_id/user_id/role/content/llm_provider/llm_model/created_at`
  - `role` 使用约束：`USER/ASSISTANT`
- 约束保障绑定关系：
  - `(session_id, task_id) -> task(session_id, id)`
  - `(session_id, user_id) -> learning_session(id, user_pk)`
- 索引：
  - 会话历史查询索引：`(session_id, task_id, user_id, created_at, id)`

### 2.2 领域与仓储

新增：

- `TutorMessage`（domain model）
- `TutorMessageRole`（enum）
- `TutorMessageRepository`（domain repository）
- `JdbcTutorMessageRepository`（JDBC 实现）

### 2.3 服务与可扩展 Provider

新增：

- `TutorMessageService`
  - `listMessages(sessionId, taskId, userId)`
  - `sendMessage(sessionId, taskId, userId, content)`
  - 内部校验 task 归属与 session 一致，防止越权访问
- Provider 扩展层：
  - `TutorProvider`（接口）
  - `TutorProviderRequest/TutorProviderReply`（上下文与返回结构）
  - `MockTutorProvider`（占位实现，可直接替换为真实 LLM）

### 2.4 API 层

新增 Controller：

- `TutorMessageController`

接口：

- `GET /api/session/{sessionId}/tasks/{taskId}/tutor/messages`
- `POST /api/session/{sessionId}/tasks/{taskId}/tutor/messages`

请求体（POST）：

```json
{
  "content": "我不理解链式法则"
}
```

返回结构：

- 历史接口返回 `messages[]`
- 发送接口返回 `user_message + assistant_message`
- 每条消息包含：`id/session_id/task_id/role/content/created_at`

## 3. 前端实现

### 3.1 API 与类型

新增：

- `src/api/tutor.ts`
- `src/mappers/tutorMapper.ts`
- `src/stores/tutor.ts`

扩展类型：

- `TutorMessage`
- `TutorMessageListResponse`
- `TutorSendMessageResponse`

### 3.2 页面改造（任务详情页）

改造文件：

- `src/views/TaskRunView.vue`

新增 AI Tutor 面板能力：

- 对话历史展示
- 空状态引导文案
- 输入框 + 发送按钮
- 发送中状态（`AI 正在思考...`）
- 历史加载失败重试
- 发送失败重试（重发上次失败内容）

布局策略：

- 保持原任务内容区与“返回会话”按钮不变
- Tutor 面板插入在任务输出后、操作区前
- 移动端增加响应式处理

## 4. 联调说明

1. 启动后端，确保 Flyway 自动执行到 `V12`。
2. 登录后进入任务详情页（`/task/{id}/run`），页面会自动加载 Tutor 历史。
3. 输入问题并发送，观察：
   - 用户消息与助手回复都入库并回显。
   - 刷新页面后历史仍存在。
4. 可通过接口验证：
   - `GET /api/session/{sessionId}/tasks/{taskId}/tutor/messages`
   - `POST /api/session/{sessionId}/tasks/{taskId}/tutor/messages`

## 5. 真实 LLM 接入扩展点

建议最小替换路径：

1. 新建 `RealTutorProvider implements TutorProvider`。
2. 在 `generateReply(TutorProviderRequest)` 中接入真实模型调用。
3. 保持 `TutorProviderReply(content, provider, model)` 不变，服务层无需改动。
4. 可在 `TutorProviderRequest.history` 中利用历史消息做多轮上下文。
5. 如需审计/计费，可扩展 `tutor_message`（token/latency/trace_id）字段。

该设计保证：MVP 先跑通、后续接真实 LLM 时变更面最小。
