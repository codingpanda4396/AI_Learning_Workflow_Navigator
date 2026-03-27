# 后端梳理

## 1. 后端定位

后端是一个围绕学习闭环组织的 Spring Boot 应用，不是通用聊天服务。它的职责是把学习流程拆成可持久化、可校验、可追踪的业务阶段。

技术栈：

- Spring Boot 3.2
- Spring Web + WebFlux
- MyBatis-Plus
- Flyway
- PostgreSQL
- Lombok
- Caffeine

启动入口是 `backend/src/main/java/navigator/BackendApplication.java`。

## 2. 分层结构

当前后端整体可以分成四层：

- `api`
控制器、统一响应、异常处理、鉴权拦截
- `application`
业务编排核心
- `domain`
枚举、模型、规则输入输出对象
- `infrastructure`
持久化、缓存、LLM 适配、内存存储、配置

其中真正承载业务骨架的是 `application`。

## 3. 控制器与业务阶段映射

控制器基本就是学习闭环的 API 投影：

- `GoalController`
目标创建
- `DiagnosisController`
诊断会话创建与提交
- `LearningPlanController`
计划预览与提交
- `SessionController`
当前任务、当前引导、报告、下一步确认
- `TaskController`
任务脚手架、消息、自我解释、检查、完成任务
- `LearningScaffoldController`
专题学习脚手架引擎
- `AiTutorController`
AI 导师提示、解释、反馈、聊天、流式聊天
- `AuthController`
注册、登录、登出、当前用户

从 API 命名就可以看出，系统是按业务阶段设计的，而不是按技术资源随意拆分。

## 4. 核心应用服务

### 4.1 `GoalApplicationService`

职责：

- 接收目标输入
- 用 `GoalRuleEngine` 生成结构化目标
- 用 `GoalContextDeriver` 生成目标上下文
- 持久化 `learning_goal`
- 在 `InMemoryStore` 中缓存 `goalId -> structuredGoal / context`

产出是后续诊断与规划的起点。

### 4.2 `DiagnosisApplicationService`

职责：

- 基于 `goalId` 创建诊断会话
- 同时创建学习会话 `learning_session`
- 生成诊断题集
- 处理诊断提交
- 基于答案推导 `LearnerProfileSnapshot`
- 生成 `DiagnosisEvidenceSummary`
- 生成 `LearnerStrategyProfile`
- 回写数据库与内存状态

这里的关键价值是把“用户当前状态”结构化。

### 4.3 `PlanningApplicationService`

职责：

- 组装规划上下文 `PlanningContext`
- 用 `PlanStrategySelector` 选择学习策略
- 用 `PlanTemplateFactory` 生成阶段与任务蓝图
- 生成推荐入口与成功标准
- 预览计划并持久化
- 在 commit 后创建 session task、更新学习会话、生成 `ExecutableTaskSpec`

这层是系统从“判断状态”走向“生成可执行路径”的中枢。

### 4.4 `ExecutionApplicationService`

职责：

- 返回当前任务与当前进度
- 代理当前任务引导查询
- 记录任务交互
- 完成任务
- 汇总任务完成信息、方法画像、证据快照
- 推进 session 的当前任务索引

它主要负责“会话级执行编排”，而更细的任务状态机在 `TaskExecutionFlowService`。

### 4.5 `TaskExecutionFlowService`

这是任务执行链路的核心服务。

职责：

- 加载或初始化任务运行时 `TaskExecutionRuntime`
- 生成任务脚手架 `TaskScaffold`
- 处理用户消息
- 检测学习动作类型
- 调用 `TaskTutorOrchestrator` 生成导师回复
- 驱动任务状态迁移
- 生成当前引导块与推荐动作
- 处理自我解释
- 处理 checkpoint 检查
- 累积执行证据
- 持久化消息、状态迁移、运行时、检查结果

它实际上实现了一套任务内状态机，常见状态包括：

- `ORIENT`
- `EXPLORE`
- `SELF_EXPLAIN`
- `CHECK`
- `REMEDIAL`
- `PASS`

### 4.6 `LearningScaffoldEngineService`

这是一个专题型执行引擎，目前明显为 DFS/BFS 场景定制。

职责：

- 查询当前脚手架阶段
- 生成结构骨架
- 校验每个脚手架动作卡的提交
- 驱动 `STRUCTURE -> UNDERSTANDING -> TRAINING -> REFLECTION`
- 生成反思记录与洞察
- 在脚手架结束后回推任务状态

这说明系统已经不满足于通用任务执行，而是在尝试将某些知识点做成“受控训练引擎”。

### 4.7 `ReportApplicationService`

职责：

- 聚合 session 级证据
- 汇总学习报告
- 计算结果状态
- 汇总学习方法画像
- 生成下一步决策 `NextActionDecision`
- 接受用户确认下一步动作

它把任务级证据收束为一次学习闭环的总结。

## 5. 领域模型重点

`domain/model` 里可以看到当前业务抽象的核心对象：

- `StructuredLearningGoal`
- `GoalContextSnapshot`
- `LearnerProfileSnapshot`
- `LearnerStrategyProfile`
- `LearningPlanPreview`
- `TaskBlueprint`
- `ExecutableTaskSpec`
- `TaskScaffold`
- `TaskExecutionRecord`
- `LearningReport`
- `NextActionDecision`
- `LearningScaffoldEngineState`

可以把它们理解为三组：

- 学习起点建模：目标、上下文、用户画像
- 执行路径建模：计划、任务蓝图、执行规范、脚手架
- 学习结果建模：执行记录、证据汇总、报告、下一步建议

## 6. 规则与决策引擎

后端的“智能”主要不是写在 Controller，而是下沉到规则与装配层：

- `GoalRuleEngine`
- `DiagnosisRuleEngine`
- `PlanStrategySelector`
- `RuleEngine` / `PlanningRule`
- `TaskGuidanceEngine`
- `GuidanceRuleEngine`
- `CompletionEvaluator`

这说明系统当前更偏“规则驱动的学习编排”，而不是完全依赖 LLM 直接决定业务状态。

## 7. LLM 与导师能力

LLM 相关能力主要集中在：

- `application/llm`
- `application/tutor`
- `infrastructure/llm`

关键角色：

- `LlmGateway`
- `OpenAiCompatibleLlmGateway`
- `TaskTutorOrchestrator`
- `TutorPromptBuilder`
- `AiTutorServiceImpl`
- `TutorFallbackRegistry`

它们的职责不是接管整套业务，而是为执行阶段补充：

- 讲解
- 引导
- 反馈
- 对话
- 流式输出

因此当前项目的架构重心仍然是业务状态机，LLM 更像增强模块。

## 8. 数据持久化结构

Flyway migration 展示了系统数据库主干。

### 8.1 主业务表

来自 `V1__create_runtime_core_tables.sql`：

- `learning_goal`
- `learning_session`
- `diagnosis_session`
- `diagnosis_answer`
- `learner_profile_snapshot`
- `learning_plan`
- `session_task`
- `task_interaction`
- `task_completion`

它们对应一条学习闭环的阶段性落点。

### 8.2 执行运行时表

来自 `V5__task_execution_runtime_persistence.sql`：

- `task_execution_runtime`
- `task_state_transition`
- `task_message`
- `task_checkpoint_result`
- `task_method_profile`

这部分是当前系统的亮点，因为它把任务执行过程也持久化了，不只记录结果。

### 8.3 认证表

来自 `V7__add_user_auth_tables.sql`：

- `app_user`
- `user_session`

并在主要业务表补充了 `user_id`。

## 9. InMemoryStore 的角色

`InMemoryStore` 在当前架构里不是临时细节，而是一个重要的会话态容器，保存了：

- 目标缓存
- 诊断状态
- plan preview
- learning session state
- executable task spec
- task runtime
- report 相关中间结果

可以理解为当前系统是“数据库持久化 + 内存运行态”双轨结构：

- 数据库负责可靠落库
- 内存负责高频运行态访问

这对开发效率很友好，但后续如果要多实例部署，需要继续考虑一致性和恢复策略。

## 10. 当前后端架构特点

当前后端有几个明显特征：

- 业务边界清晰，严格围绕学习闭环拆分
- 规则驱动强于纯 LLM 驱动
- 任务执行是独立状态机，不是简单问答记录
- 专题脚手架引擎已经成为差异化能力
- 数据既记录结果，也记录过程

## 11. 当前值得继续关注的后端点

- `TaskExecutionFlowService` 和 `LearningScaffoldEngineService` 已经很重，后续要持续控制复杂度
- `InMemoryStore` 当前承担较多运行态职责，未来要评估多实例与恢复策略
- 知识包能力目前明显偏向 DFS/BFS，后续扩展时要关注可配置化程度
- 报告、方法画像、下一步决策已经具备产品化基础，适合继续强化复盘闭环

