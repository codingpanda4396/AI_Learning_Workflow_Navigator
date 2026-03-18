# Sprint 3 各阶段落地说明（已实现）

## Phase 1 静态脚手架

| 项 | 实现 |
|----|------|
| 领域模型 | [`TaskScaffold`](backend/src/main/java/navigator/domain/model/TaskScaffold.java) |
| 生成 | [`TaskScaffoldFactory`](backend/src/main/java/navigator/application/task/TaskScaffoldFactory.java) 基于 `TaskBlueprint` 规则生成列表字段 |
| API | `GET /api/tasks/{taskId}/scaffold?sessionId=` → [`TaskScaffoldResponse`](backend/src/main/java/navigator/api/dto/TaskScaffoldResponse.java) |
| 存储 | 挂在 [`TaskExecutionRuntime`](backend/src/main/java/navigator/application/task/TaskExecutionRuntime.java)，键 `sessionId\|taskId`，见 [`InMemoryStore`](backend/src/main/java/navigator/infrastructure/memory/InMemoryStore.java) |

## Phase 2 执行状态机

| 状态 | 枚举 [`TaskExecutionState`](backend/src/main/java/navigator/domain/enums/TaskExecutionState.java) |
| 流转 | [`TaskExecutionFlowService`](backend/src/main/java/navigator/application/task/TaskExecutionFlowService.java)：拉脚手架 → ORIENT；首条消息 → EXPLORE；满足探索轮次后自解释 → CHECK/REMEDIAL；微检查 → PASS/REMEDIAL |
| complete 门禁 | 仅当**已创建 runtime**（拉过脚手架或发过 `/messages`）且状态非 `PASS` 时拒绝，错误码 `TASK_EXECUTION_NOT_READY_FOR_COMPLETE`（[`TaskProgressGuard`](backend/src/main/java/navigator/application/guard/TaskProgressGuard.java)） |
| 兼容 | 未创建 runtime 的会话仍可像以前一样直接 `complete`（集成测试与旧客户端） |

## Phase 3 受控导师 LLM

| 项 | 实现 |
|----|------|
| 网关 | [`LlmGateway`](backend/src/main/java/navigator/application/llm/LlmGateway.java) + [`MockLlmGateway`](backend/src/main/java/navigator/application/llm/MockLlmGateway.java) |
| 编排 | [`TaskTutorOrchestrator`](backend/src/main/java/navigator/application/llm/TaskTutorOrchestrator.java) 按 EXPLORE/REMEDIAL 裁剪回复 |
| API | `POST /api/tasks/{taskId}/messages`、`/self-explanation`、`/checkpoint` |

## Phase 4 学习动作识别

| 项 | 实现 |
|----|------|
| 检测 | [`LearningActionDetector`](backend/src/main/java/navigator/application/learning/LearningActionDetector.java) 规则 + 枚举 [`LearningActionType`](backend/src/main/java/navigator/domain/enums/LearningActionType.java) |
| 单测 | [`LearningActionDetectorTest`](backend/src/test/java/navigator/application/learning/LearningActionDetectorTest.java) |

## Phase 5 方法反馈

| 项 | 实现 |
|----|------|
| 单任务画像 | [`LearningMethodProfile`](backend/src/main/java/navigator/domain/model/LearningMethodProfile.java) + [`LearningMethodProfileAggregator`](backend/src/main/java/navigator/application/task/LearningMethodProfileAggregator.java) |
| 沉淀时机 | 每次 `completeTask` 若存在 runtime 则写入 `sessionMethodProfiles` |
| 报告 | [`LearningReport.learningMethodProfile`](backend/src/main/java/navigator/domain/model/LearningReport.java) 会话级汇总；[`ReportApplicationService`](backend/src/main/java/navigator/application/ReportApplicationService.java) 中 Next Action 可结合 `questioningQuality==LOW` 调整建议 |
| 前端 | [`TaskRunView.vue`](frontend/src/views/TaskRunView.vue) 四区脚手架；[`ReportView.vue`](frontend/src/views/ReportView.vue)「学习方法表现」 |

## 验证

- `mvn test`：含 [`Sprint3TaskExecutionFlowTest`](backend/src/test/java/navigator/api/controller/Sprint3TaskExecutionFlowTest.java)（单任务全链路）
- `npm run build`：前端通过

## 后续可迭代

- 将 `task_execution` 相关从内存迁表；接入真实 OpenAI 等 `LlmGateway` 实现；`/interactions` 内部委托 `/messages`（当前仍并存）。
