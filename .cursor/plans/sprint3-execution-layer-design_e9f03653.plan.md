---
name: sprint3-execution-layer-design
overview: 基于你提供的“Plan 模式冻结版输出”，为 Sprint 3 的 5 个 Phase（静态脚手架、执行状态机、受控导师 LLM、学习动作识别、方法反馈）整理一份更工程化的高层技术方案，覆盖后端/前端/测试与演进策略。
todos:
  - id: phase1-scope-refine
    content: 在现有代码仓恢复可见后，将 Phase 1 涉及的具体类名、包路径和 DTO 以逐文件粒度对齐到真实实现
    status: pending
  - id: backend-domain-application-design
    content: 细化后端 domain/application 层中 TaskScaffold、TaskExecutionStateMachine、LearningActionType 等对象及服务的内部方法签名
    status: pending
  - id: frontend-taskrun-structure
    content: 细化 TaskRunView 和 taskExecution store 的状态结构、API 调用与组件拆分方案
    status: pending
  - id: api-contract-check
    content: 对照现有 controller 与 DTO，落地 /scaffold、/messages、/self-explanation、/checkpoint 的最终入参和出参合同
    status: pending
isProject: false
---

## Sprint 3 执行层整体技术方案（高层）

### 1. 总体设计目标

- **目标**：把现有“任务卡 + 交互记录”的执行层，升级为“有状态的学习行为编排器”，同时不打断既有主链路（目标→诊断→规划→执行→反馈&NextAction）。
- **核心思路**：
  - 规则主导：状态机、动作识别、方法归因全部由规则/枚举驱动，LLM 只做文案与细节建议。
  - 运行时快照：以 `TaskScaffold` + `TaskExecutionState` 为执行期主视角，不让 `TaskBlueprint` 膨胀。
  - 渐进演进：Phase 1-5 每个阶段都保持接口兼容，支持前后端迭代解耦。
- **关键对象**：`TaskScaffold`、`TaskExecutionState`、`LearningActionType`、`LearningMethodProfile`、`TaskTutorOrchestrator`、`LlmGateway`。

### 2. 后端整体架构演进（按包/模块）

- **domain 层**
  - `navigator.domain.task`
    - 新增：`TaskScaffold`、`TaskExecutionState`、`TaskStateTransition`。 
    - 约束：`TaskBlueprint` 保持规划职责，不直接暴露执行期字段；执行期逻辑统一靠 `TaskScaffold + TaskExecutionState`。
  - `navigator.domain.learning`
    - 新增：`LearningActionType`、`LearningActionEvent`。 
    - 后续：`LearningMethodProfile` 作为报告期方法画像对象。
- **application 层**
  - `navigator.application.task`
    - `TaskScaffoldService`：
      - 输入：`TaskBlueprint` + goal + profile + plan；
      - 输出：`TaskScaffold` 并缓存/持久化；
      - 提供：按 `taskId` 查询当前 scaffold 的接口供 API 使用。
    - `TaskExecutionStateMachine`：
      - 实现状态转移规则（INIT→ORIENT→ASK→...→PASS/REMEDIAL）；
      - 落 `TaskStateTransition` 记录；
      - 对外暴露 `advanceState(taskId, trigger)` 风格方法。
  - `navigator.application.llm`
    - `TaskTutorOrchestrator`：对接状态机、scaffold、消息记录与 `LlmGateway`；
    - `PromptTemplateRegistry`：按 `TaskExecutionState` + `taskType` 返回固定模板；
    - `ResponseSchemaValidator`：对 LLM JSON 结构做校验和降级。
  - `navigator.application.learning`
    - `LearningActionDetector`：基于规则/有限 LLM 识别 `LearningActionType`；
    - `LearningMethodProfileAggregator`：聚合 action event + state transition + checkpoint 结果生成 `LearningMethodProfile`。
  - `navigator.application.report`
    - 在现有报告生成流程中，引入 `LearningMethodProfile` 作为新组成部分。
- **api 层**
  - `navigator.api.task`
    - 保留：`GET /api/sessions/{sessionId}/current-task`、`POST /api/tasks/{taskId}/complete`；
    - 新增/演进：
      - `GET /api/tasks/{taskId}/scaffold`：返回执行期脚手架；
      - `POST /api/tasks/{taskId}/messages`：统一用户消息入口，内部委托给 orchestrator + detector + state machine；
      - `POST /api/tasks/{taskId}/self-explanation`：自解释提交；
      - `POST /api/tasks/{taskId}/checkpoint`：微检查答题；
      - `POST /api/tasks/{taskId}/interactions`：对外保持兼容，对内转发到 `messages` 或打标为 legacy 交互。
  - `navigator.api.report`
    - 扩展 `GET /api/sessions/{sessionId}/report` DTO，在 `data` 中新增 `learningMethodProfile` 字段。
- **infrastructure 层**
  - `navigator.infrastructure.persistence`
    - 新增表/实体：
      - `task_scaffold`：`scaffold_id, task_id, session_id, task_type, objective, why_this_task, templates(json), completion_signals(json), anti_patterns(json), created_at`；
      - `task_execution_state`：`task_id, session_id, current_state, updated_at`；
      - `task_state_transition`：`id, task_id, from_state, to_state, trigger, reason, created_at`；
      - `learning_action_event`：`id, session_id, task_id, message_id, action_type, created_at`；
      - `task_message`（如尚无）：`id, task_id, session_id, role, content, created_at`；
      - `checkpoint_result`：`id, task_id, session_id, result, reason, created_at`；
      - `learning_method_profile`（可先仅内存聚合，后续再落库）。
  - `navigator.infrastructure.llm`
    - `LlmGateway` 接口 + `OpenAiLlmGateway` / `MockLlmGateway` 实现；
    - 提供统一的超时、重试、降级策略；
    - 对外只暴露结构化 response 对象。

### 3. 前端整体架构演进（Vue 假设）

- **路由保持不变**：继续使用 `/tasks/:taskId/run`、`/sessions/:sessionId/report` 等路径。
- **状态管理**（示例为 Pinia）
  - `learningPlan` store 保持规划态；
  - 新增 `taskExecution` store：
    - state：`currentTaskId`、`scaffold`、`executionState`、`messages`、`isLoading` 等；
    - actions：`fetchCurrentTask()`、`fetchScaffold(taskId)`、`sendMessage()`、`submitSelfExplanation()`、`submitCheckpoint()`；
    - getters：`canSelfExplain`、`canCheckpoint`、`isPass` 等。
- **主要视图组件**
  - `TaskRunView.vue`
    - 布局拆分为 4 区：
      - 任务卡区：展示 `TaskBlueprint` 的 title/estimatedMinutes 等；
      - 脚手架区：绑定 `TaskScaffold` 的目标、why、推荐问法、自检模板；
      - 导师区：消息列表 + 输入框 + “下一句推荐” chips；
      - 状态条/进度区：展示 `TaskExecutionState`，高亮当前阶段。
  - `ReportView.vue`
    - 新增“学习方法表现”模块：图标或标签形式展示质询质量、自解释质量、是否通过检查、正/反向行为；
    - 避免 UI 杂乱，控制在 3-5 条关键信息 + 一段 summary 文案。
- **API client 层**
  - 基于已有 axios 实例，新增 `taskExecutionApi`：
    - `getScaffold(taskId)`、`postMessage(taskId, payload)`、`postSelfExplanation(taskId, payload)`、`postCheckpoint(taskId, payload)`；
  - 兼容旧 `/interactions` 用途：在新接口 ready 之前，TaskRunView 可以通过 feature flag 切换。

### 4. Phase 1：静态脚手架（高层技术方案）

- **后端关键工作**
  - 在 `navigator.domain.task` 引入 `TaskScaffold`；
  - 在 `navigator.application.task` 实现 `TaskScaffoldService`：
    - 暂时只基于 `TaskBlueprint` + `taskType` 规则生成固定脚手架（不依赖 LLM）；
    - 支持按需缓存（InMemoryStore + 简单 repository）。
  - 在 `navigator.api.task` 增加 `GET /api/tasks/{taskId}/scaffold`：
    - DTO 聚焦展示字段，不暴露内部 ID；
  - 在 `persistence` 新增 `task_scaffold` 存储结构（可先走内存实现，预留表结构注释）。
- **前端关键工作**
  - `TaskRunView.vue` 接入新的 `getScaffold`，渲染 3-4 类模板字段；
  - 引入基础的状态条组件（仅 ORIENT / EXPLORE / PASS 占位）；
  - 不引入 LLM 调用，仅做 UI 框架搭建。
- **测试与验证**
  - 后端：`TaskScaffoldService` 单测覆盖典型 taskType；
  - 前端：`TaskRunView` 的渲染 smoke test（或手工验证）。

### 5. Phase 2：执行状态机（高层技术方案）

- **状态与触发**
  - 状态：`INIT, ORIENT, ASK, EXPLORE, SELF_EXPLAIN, CHECK, PASS, REMEDIAL`；
  - 触发源：
    - 初始化 / 进入任务；
    - 首条有效消息；
    - 自解释提交；
    - 检查结果；
    - 补救完成。
- **后端实现要点**
  - `TaskExecutionStateMachine`：纯规则实现 + 枚举触发器（`INIT_LOADED`, `FIRST_QUESTION`, `SELF_EXPLAIN_SUBMITTED`, `CHECK_PASSED`, `CHECK_FAILED`, `REMEDIAL_DONE`）；
  - `TaskExecutionStateRepository`：基于 `task_execution_state` 表或 InMemoryStore；
  - 在 `GET /current-task` 和 `GET /scaffold` 的响应中增加 `currentExecutionState` 字段；
  - `POST /complete` 收紧：仅允许在 `PASS` 或手动 override 时成功。
- **前端实现要点**
  - `taskExecution` store 引入 `executionState` 字段；
  - `TaskRunView` 状态条根据 `executionState` 高亮当前阶段；
  - 自解释/检查入口暂只做占位按钮，点击后提示“即将上线”。
- **测试与验证**
  - 状态机单测：涵盖常见 happy path + remedial path；
  - 简单集成测试：执行若干触发器后检查状态持久化与查询。

### 6. Phase 3：受控导师 LLM（高层技术方案）

- **后端实现要点**
  - `TaskTutorOrchestrator`：
    - 根据 `executionState` 选取 prompt 模板；
    - 调用 `LlmGateway` 并用 `ResponseSchemaValidator` 解析结构化结果；
    - 决定下一步推荐状态（例如 `MOVE_TO_SELF_EXPLAIN`）；
    - 生成 `TutorReplyEnvelope`，包含 `assistantReply`、`suggestedNextPrompts`、`stateRecommendation` 等。
  - `LlmGateway`：
    - 支持 provider 抽象与 mock 实现；
    - 提供 JSON schema 保底解析与错误降级（返回规则模板文案）。
  - API：
    - `POST /api/tasks/{taskId}/messages`：主消息入口，链路为：
      - 保存 `TaskMessage`；
      - 识别 action（Phase 4 预留）；
      - 调用 orchestrator；
      - 推进状态机；
      - 返回导师回复与最新状态。
- **前端实现要点**
  - `TaskRunView` 将发送消息改为调用 `/messages`；
  - 消息列表展示 `assistantReply` 与 `suggestedNextPrompts`；
  - 提供基于 `suggestedNextPrompts` 的一键填充按钮。
- **测试与验证**
  - 使用 `MockLlmGateway` 做 deterministic 集成测试；
  - 手工验证 EXPLORE / SELF_EXPLAIN / CHECK 不同 prompt 行为差异。

### 7. Phase 4：学习动作识别（高层技术方案）

- **后端实现要点**
  - `LearningActionDetector`：
    - 首版仅规则匹配常见句式和关键词（举例、对比、更简单、自我理解、直接给答案等）；
    - 输出 `LearningActionType`；
    - 按 `task_message` 记录 `learning_action_event`。
  - 在 `POST /messages` / `POST /self-explanation` / `POST /checkpoint` 流程中插入检测；
  - 为状态机提供额外触发信号（如 `SELF_EXPLANATION` 到 `CHECK`）。
- **前端实现要点**
  - 可以轻量展示 `detectedAction`（如小标签），也可只用于报告与 Next Action，不强制用户看到。
- **测试与验证**
  - detector 单测覆盖至少 1-2 条示例句，确保每个枚举均有典型命中；
  - 确保误判不会导致严重状态错乱（例如 `SEEK_DIRECT_ANSWER` 只记负面信号，不强制状态回退）。

### 8. Phase 5：方法反馈（高层技术方案）

- **后端实现要点**
  - `LearningMethodProfileAggregator`：
    - 以 sessionId + taskId 为粒度，从 `learning_action_event` 与 `task_state_transition` + `checkpoint_result` 汇总：
      - 提问质量（根据 ASK_* 动作的比例与多样性）；
      - 是否发生自解释及其质量（基于自解释文本长度 / 关键短语，简单规则）；
      - 检查是否通过；
      - 主要正向/反向行为列表；
      - 生成结构化 `nextMethodAdvice` 列表。
  - 在 report 生成流程中：
    - 生成 `LearningMethodProfile`；
    - 挂入 `LearningReport` 的 DTO 中。
- **前端实现要点**
  - `ReportView.vue` 新增“学习方法表现”区域：
    - 一行 summary 文案（可由 LLM 在受控模板下生成）；
    - 3-5 个 badge/tag 表示关键点（如“有自解释”“检查通过”“多次 seek direct answer”）。
- **测试与验证**
  - 方法聚合单测：给定一组虚拟 action/状态/检查数据，验证输出 profile 与预期一致；
  - 手工走一轮完整任务，确认报告中方法区与实际行为对应。

### 9. 风险与兼容性策略（高层）

- **接口兼容**
  - 保留旧 `current-task` / `complete` / `interactions`，内部逐步迁移到新实现；
  - 所有新增字段以扩展方式出现在 DTO 中，前端可渐进消费。
- **LLM 风险**
  - 所有关键决策（状态迁移、动作分类、是否 PASS）必须由规则/枚举主导；
  - LLM 调用失败时：
    - 不改变状态机结果；
    - 返回规则模板回复 + 提示文案。
- **实现节奏**
  - Phase 1-2 完成后，即可在无 LLM 的情况下跑通一个“有状态但无导师”的执行闭环；
  - Phase 3-5 属于在此基础上的增强，可分支并行开发。

### 10. 验证路径（端到端）

- **最小闭环路径**
  1. 通过已有目标/诊断/规划链路生成 session 与 tasks；
  2. 进入 `TaskRunView`：
    - 加载 `current-task` 与 `scaffold`；
    - 初始化状态为 `ORIENT`；
  3. 用户发送消息、进入 EXPLORE；
  4. 用户自解释，进入 SELF_EXPLAIN；
  5. 微检查 PASS，状态进入 PASS，允许 `complete`；
  6. 报告页展示任务结果 + 方法表现。
- **验收建议**
  - 为上述路径写 1 条端到端测试用例（后端集成测试为主，前端可补录屏或手工脚本）；
  - 所有关键对象与接口在 CLAUDE / RULE 文档中补充简要说明，确保后续迭代口径统一。

