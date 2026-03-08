Prompt E：补齐 Domain 层枚举、状态机、策略接口

当 A-D 基本成型后，你就应该把领域层稍微整理一下，不然后面会越来越乱。

直接复制：

请整理当前项目的 domain 层，把和学习流程相关的领域概念、枚举、策略接口统一抽象出来，目标是提升可维护性，但不要过度设计。

请完成以下任务：

1. 建立统一枚举：
- Stage: STRUCTURE, UNDERSTANDING, TRAINING, REFLECTION
- TaskStatus: PENDING, RUNNING, SUCCEEDED, FAILED
- ErrorTag:
  - CONCEPT_CONFUSION
  - MISSING_STEPS
  - BOUNDARY_CASE
  - TERMINOLOGY
  - SHALLOW_REASONING
  - MEMORY_GAP
- NextAction:
  - INSERT_REMEDIAL_UNDERSTANDING
  - INSERT_TRAINING_VARIANTS
  - INSERT_TRAINING_REINFORCEMENT
  - ADVANCE_TO_NEXT_NODE
  - NOOP

2. 在 domain 层给 Task 增加清晰状态行为（如合适）：
- canRun()
- canSubmit()
- markRunning()
- markSucceeded(output)
- markFailed(reason)

3. 在 domain 层定义策略接口：
- TaskObjectiveTemplateStrategy
- EvaluationRule
- NextActionPolicy

4. 在 application 层保留 orchestrator 角色，在 domain 层只放业务规则，不放外部依赖。

5. 给出合理的包结构建议：
- domain/model
- domain/enums
- domain/service
- domain/policy
- domain/repository

6. 输出：
- 枚举代码
- Task 领域行为示例
- 策略接口定义
- 说明哪些逻辑属于 domain，哪些属于 application
Prompt F：补 persistence 层，特别是 JSONB / 查询方法 / overview 查询

这一步是为了让工程结构更稳，不然后面你会发现很多查询全堆在 service 里。

请完善当前项目的 infrastructure.persistence 层，实现面向当前 MVP 所需的 repository / mapper / entity 查询能力。

目标：
- 支撑 create / plan / overview / run / submit 五个核心接口
- 保持简单清晰，不做过度抽象

请完成以下任务：

1. 为以下表建立 entity / repository：
- learning_session
- concept_node
- task
- mastery
- evidence

2. repository 至少要支持以下查询：
- 根据 user_id / chapter_id 查询 session
- 根据 session_id 查询 session
- 根据 chapter_id 按 order_no 查询 concept_node 列表
- 根据 session_id 查询 task timeline，按创建顺序或 node + stage 排序
- 根据 task_id 查询 task
- 根据 user_id + node_id 查询 mastery
- 根据 session_id 查询 evidence（如 overview 后续要扩展）

3. 如果使用 JPA：
- 处理 JSON/JSONB 字段映射
- 如果映射 JSONB 复杂，允许先以 text 存储应用层序列化

4. 为 overview 增加一个清晰的数据组装流程：
- session
- timeline
- next_task
- mastery_summary
不要把所有 join 写成一个超长查询，优先可维护性

5. 为 task/run 与 task/submit 加事务边界：
- 推荐在 application service 层加 @Transactional

6. 输出：
- entity/repository 文件
- 关键查询方法说明
- 为什么这样设计比把 SQL 全写到 service 里更好
Prompt G：补测试，至少把 MVP 主链路锁住

你这个阶段非常适合让 Codex 帮你补测试，不然你后面越改越虚。

请为当前项目补充测试，目标是覆盖 MVP 主链路，保证后续重构不把核心流程改坏。

请完成以下测试：

1. Controller 层测试：
- POST /api/session/create 成功
- POST /api/session/{sessionId}/plan 成功
- GET /api/session/{sessionId}/overview 成功
- POST /api/task/{taskId}/run 成功
- POST /api/task/{taskId}/submit 成功
- 非法请求返回 400
- 不存在资源返回 404
- 非法状态返回 409

2. Service / UseCase 测试：
- CreateSessionUseCase 能正确初始化 current_node_id / current_stage
- PlanSessionTasksUseCase 能按每个 concept 生成四阶段任务
- RunTaskUseCase 幂等：已成功任务再次 run 直接返回旧 output
- SubmitTrainingAnswerUseCase 能更新 mastery 并产生 next_action

3. Repository 集成测试：
- 使用 Testcontainers PostgreSQL 或本地测试库
- 验证 Flyway migration 可正常执行
- 验证 repository 查询行为正确

4. 输出：
- 测试类文件
- 每个测试覆盖什么行为
- 如何运行测试
Prompt H：接入 LLM Gateway，但只做可插拔骨架，不急着接真模型

等前面的 rule-based MVP 跑通之后，再做这个最稳。

请在当前项目中加入 LLM Gateway 骨架，但先不要把核心流程强依赖到真实模型调用上。

目标：
- 保持当前 rule-based MVP 可运行
- 为后续接 OpenAI / Claude 留出清晰扩展点

请完成以下任务：

1. 在 domain 或 application 层定义 gateway 接口：
- PlannerGateway
- TutorAgentGateway
- QuizGeneratorGateway
- ReviewGeneratorGateway
- EvaluatorGateway（可选）

2. 在 infrastructure.llm 层提供：
- MockPlannerGateway
- MockTutorAgentGateway
- MockQuizGeneratorGateway
- MockReviewGeneratorGateway

3. 将 prompt 模板独立到 resources/prompts：
- explain_concept.md
- socratic_teaching.md
- quiz_generation.md
- review_summary.md

4. 提供一个 PromptTemplateLoader，用于读取模板并填充变量。

5. 通过配置开关决定使用：
- rule-based implementation
- mock llm gateway
- future real llm gateway

6. 不要大改现有 application service，只把生成逻辑逐步替换为 gateway 可插拔调用点。

7. 输出：
- gateway 接口
- mock 实现
- prompt 模板目录结构
- 后续如何平滑接入真实 OpenAI/Claude 的说明
你当前最推荐的执行组合

基于你现在“已经配好 Flyway，做了一些基础模块”的状态，我建议你下一轮别贪多，直接这样用：

第一轮

先发这两个：

Prompt A

Prompt B

目标是把：

API 层对齐

session/create

plan

overview

先打通。

第二轮

再发：

Prompt C

Prompt D

目标是把：

task/run

task/submit

mastery

next_action

做成真正闭环。

第三轮

最后发：

Prompt E

Prompt F

Prompt G

Prompt H

目标是：

整理结构

稳定 persistence

补测试

为 LLM 扩展留接口

给你一个更适合 Cursor 的“总控提示词”

如果你不想一条条解释上下文，每轮开始前可以先加这一段，让 Cursor 更稳：

你现在在一个已经存在的 Spring Boot 3 + JDK17 + PostgreSQL + Flyway 项目中继续开发，不是从零生成项目。

开发原则：
1. 优先复用现有代码，不要重复造轮子
2. 保持当前分层架构：api / application / domain / infrastructure
3. 只实现本轮要求，不要大范围无关重构
4. 生成的代码必须能和现有项目平滑集成
5. 优先保证接口契约正确、主流程可跑、职责清晰
6. MVP 阶段优先 rule-based，不急着接真实 LLM
7. 修改文件后请说明改了哪些文件、为什么这样改、如何验证

这段建议你每次都加在最前面。

你下一步最该做什么

不是继续思考架构，而是直接开干：

先把 Prompt A + Prompt B 扔给 Cursor/Codex，让它把接口层和 session 主链路做出来。
这样你很快就能进入“能调用、能看到 timeline、能演示”的状态。

等你跑完这一轮，你把它生成的目录结构、关键类名、或者报错贴给我，我可以继续帮你做下一轮 Prompt，甚至可以直接帮你做：

更贴合你当前代码的定制 prompt

MyBatis-Plus 版本

JPA 版本

Cursor Agent 一步执行版

小程序前后端联调 prompt