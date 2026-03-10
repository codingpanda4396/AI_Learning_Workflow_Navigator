下面是可以**直接丢给 Codex / Cursor** 的 TODO Prompt 文件内容。我按你当前后端现状来写了：保留现有 Spring Boot 3 + Java 17 + PostgreSQL + Flyway + JDBC 分层架构，在 **`run` 和 `submit` 两个关键链路** 上引入 LLM，且尽量最小侵入，不推翻已有主链路。你当前系统已经有 `session -> plan -> run -> submit -> mastery -> nextAction` 闭环、四阶段任务、`task_attempt` / `mastery` / `evidence` 等核心模型，所以这次改造重点就是把模板生成和启发式评估升级为模型驱动。

你直接复制下面整段即可。

````md
# TODO Prompt：为当前后端 MVP 引入 LLM 能力（最小侵入式改造）

你现在的角色是：

1. 高级 Java 后端架构师
2. Spring Boot 3 / JDK 17 / PostgreSQL / Flyway / Spring JDBC 工程顾问
3. LLM 接入与结构化输出设计专家
4. 现有项目最小侵入式改造执行者

你的任务不是从零重写项目，而是：

**基于我当前已经完成的后端 MVP，实现“LLM 增强版 MVP”。**
要求保留现有架构、目录分层、已有主流程和数据模型，尽量小改动完成接入。

---

# 一、项目现状（必须基于此改造，不要推翻）

当前技术栈与架构现状如下：

## 技术基线
- Spring Boot 3.4.x
- Java 17
- Spring JDBC（不是 JPA）
- PostgreSQL
- Flyway
- springdoc OpenAPI

## 分层结构
- `api`：Controller + DTO
- `application`：UseCase / Service
- `domain`：实体、枚举、策略接口
- `infrastructure`：JDBC Repository、配置、异常处理

## 已有 API
### Session / Workflow
- `POST /api/session/create`
- `POST /api/session/{sessionId}/plan`
- `GET /api/session/{sessionId}/overview`
- `GET /api/session/{sessionId}/path`
- `GET /api/session/current?user_id=...`

### Task
- `GET /api/task/{taskId}`
- `POST /api/task/{taskId}/run`
- `POST /api/task/{taskId}/submit`

## 已有核心模型
- `learning_session`
- `concept_node`
- `task`
- `task_attempt`
- `mastery`
- `evidence`

## 已有阶段枚举
- `STRUCTURE`
- `UNDERSTANDING`
- `TRAINING`
- `REFLECTION`

## 当前主链路
- 创建 session
- 生成全章节任务
- `run`：执行任务并生成阶段产物
- `submit`：提交训练答案并评估
- 更新 mastery
- 根据 `NextActionPolicy` 决策后续动作

## 当前问题 / 演进目标
1. `run` 当前主要是模板生成 JSON，缺少真正 AI 能力
2. `submit` 当前 `EvaluatorService` 偏启发式，通用性有限
3. `NextActionPolicy` 主要还是看分数阈值，尚未充分利用 error tags
4. 希望 MVP 真正具备 LLM 能力，但不要把系统做成普通聊天框

---

# 二、本次改造的明确目标

请你实现一个 **LLM 增强版后端 MVP**，满足：

## 目标 1：阶段内容生成接入 LLM
让 `POST /api/task/{taskId}/run` 在不同 stage 下调用 LLM，生成结构化 JSON 内容，而不是写死模板。

### 四阶段要求
#### STRUCTURE
输出：
- title
- summary
- key_points
- common_misconceptions
- suggested_sequence

#### UNDERSTANDING
输出：
- concept_explanation
- analogy
- step_by_step_reasoning
- common_errors
- check_questions

#### TRAINING
输出：
- questions（3~5道）
每题至少包含：
- id
- type
- question
- reference_points
- difficulty

#### REFLECTION
输出：
- reflection_prompt
- review_checklist
- next_step_suggestion

---

## 目标 2：训练答案评估接入 LLM
让 `POST /api/task/{taskId}/submit` 调用 LLM 对用户答案进行结构化评估，而不是仅靠启发式规则。

### 评估输出必须是结构化 JSON
至少包含：
- score: 0~100
- normalized_score: 0~1
- feedback
- error_tags: string[]
- strengths: string[]
- weaknesses: string[]
- suggested_next_action

---

## 目标 3：保留现有主链路
必须保留以下既有行为，不要推翻：
- `task_attempt` 记录任务执行和提交痕迹
- `mastery` 继续按已有规则更新（可复用现有 EMA）
- `evidence` 继续留痕
- `NextActionPolicy` 继续负责决策
- 现有 Controller API 路径尽量不变
- 现有 DTO 尽量兼容前端

---

## 目标 4：最小侵入式
不要把整个项目重构成复杂的 agent 系统。
不要引入重量级框架。
不要引入 JPA。
优先复用当前代码风格与现有层次。

---

# 三、改造原则（非常重要）

请严格遵守以下原则：

## 原则 1：先抽象，后接具体厂商
不要在 Service 里直接写 OpenAI / DeepSeek 的 HTTP 调用。
请先定义抽象接口，再在 infrastructure 层实现。

## 原则 2：结构化输出优先
LLM 不能只返回一大段文本。
必须尽可能返回可解析 JSON。
需要做输出 schema 校验或最少的解析保护。

## 原则 3：失败可降级
如果 LLM 调用失败：
- `run` 应可降级到原先模板生成逻辑（如果现有逻辑可复用）
- `submit` 应可降级到规则评估逻辑（如果现有 EvaluatorService 可复用）

## 原则 4：保留可审计性
模型调用必须留痕，方便调试、回放、比赛展示。

## 原则 5：不要做成聊天接口
本项目不是通用 chat bot。
LLM 的角色是：
- 生成阶段学习卡片内容
- 评估训练答案
- 辅助学习流程决策

---

# 四、你需要完成的具体开发任务

请直接在代码层面完成以下 TODO。

---

## TODO A：设计领域抽象接口

请新增或补充以下抽象（命名可稍调，但语义必须保留）：

### 1. LlmGateway
统一模型调用网关

建议：
```java
public interface LlmGateway {
    LlmTextResult generate(LlmPrompt prompt);
}
````

### 2. StageContentGenerator

阶段内容生成器

```java
public interface StageContentGenerator {
    StageContent generate(StageGenerationContext context);
}
```

### 3. AnswerEvaluator

答案评估器

```java
public interface AnswerEvaluator {
    EvaluationResult evaluate(EvaluationContext context);
}
```

### 4. PromptTemplateProvider（可选但推荐）

用于管理不同业务 prompt 模板

---

## TODO B：定义核心 DTO / VO / 领域对象

请新增结构化对象，至少包括：

### LLM 基础对象

* `LlmPrompt`
* `LlmTextResult`
* `LlmUsage`（token_input / token_output / latency_ms）

### 阶段生成上下文

* `StageGenerationContext`

  * taskId
  * sessionId
  * chapterId
  * nodeId
  * nodeTitle / nodeName
  * stage
  * objective
  * prerequisite summary（若拿得到）
  * 历史 mastery（若拿得到）

### 阶段生成结果

* `StageContent`

  * 建议内部用统一对象 + `JsonNode` / Map 承载内容
  * 或按 stage 拆分子类也可以，但不要过度设计

### 评估上下文

* `EvaluationContext`

  * taskId
  * sessionId
  * nodeId
  * taskObjective
  * generatedQuestionContent
  * userAnswer
  * masteryBefore（可选）
  * stage

### 评估结果

* `EvaluationResult`

  * score
  * normalizedScore
  * feedback
  * errorTags
  * strengths
  * weaknesses
  * suggestedNextAction
  * rawJson（可选）

---

## TODO C：实现 infrastructure 层的具体 LLM 适配器

请实现一个**可通过配置切换 provider 的 LLM 适配层**。

### 配置要求

新增配置项，例如：

* `app.llm.enabled=true`
* `app.llm.provider=openai-compatible`
* `app.llm.base-url=...`
* `app.llm.api-key=...`
* `app.llm.model=...`
* `app.llm.timeout-ms=...`

### 实现要求

1. 优先使用 Spring 原生方式实现 HTTP 调用（如 `RestClient` / `WebClient` / `RestTemplate` 任选其一，但请统一）
2. 实现一个 OpenAI-compatible 风格适配器，便于后续切换：

   * OpenAI
   * DeepSeek
   * 通义千问兼容层
   * OpenRouter
3. 不要硬编码某一家厂商到业务层
4. 注意超时、错误处理、响应解析

### 产出

至少实现：

* `OpenAiCompatibleLlmGateway`

---

## TODO D：实现 Prompt 模板体系

请为以下业务场景编写 Prompt Builder / Template：

### 1. STRUCTURE_PROMPT_V1

要求模型输出 JSON，字段为：

* title
* summary
* key_points
* common_misconceptions
* suggested_sequence

### 2. UNDERSTANDING_PROMPT_V1

字段为：

* concept_explanation
* analogy
* step_by_step_reasoning
* common_errors
* check_questions

### 3. TRAINING_PROMPT_V1

字段为：

* questions[]

### 4. REFLECTION_PROMPT_V1

字段为：

* reflection_prompt
* review_checklist
* next_step_suggestion

### 5. EVALUATE_PROMPT_V1

字段为：

* score
* normalized_score
* feedback
* error_tags
* strengths
* weaknesses
* suggested_next_action

### Prompt 编写要求

* 明确要求输出 JSON
* 明确禁止输出 markdown code fence
* 明确限制字段名
* 明确限定语言为中文
* 明确告知业务身份：这是一个面向大学生学习流程的智能导师系统
* 对 `TRAINING` 和 `EVALUATE` 要强调“不要脱离题目和知识点本身”

---

## TODO E：改造 `run` 主链路

请改造 `POST /api/task/{taskId}/run` 对应的应用服务。

### 新逻辑要求

1. 查询 task / session / node / objective 等上下文
2. 如果任务已有成功输出且允许复用，则保持现有幂等行为
3. 如果启用 LLM：

   * 根据 stage 构造 `StageGenerationContext`
   * 调用 `StageContentGenerator`
   * 得到结构化内容
   * 写入 `task_attempt.output_json`
   * 记录 LLM 调用信息
4. 如果 LLM 调用失败：

   * 降级到原模板逻辑（若存在）
   * 仍然写入 attempt
5. 返回给前端的仍然是结构化 JSON 结果，不要改成大段纯文本

### 注意

* 保持事务边界合理
* 不要在事务中做过长的外部调用，必要时调整流程
* 失败状态要写清楚

---

## TODO F：改造 `submit` 主链路

请改造 `POST /api/task/{taskId}/submit` 对应的应用服务。

### 新逻辑要求

1. 仅 TRAINING 任务允许提交，保持现有校验
2. 读取题目内容、用户答案、task objective、node 信息
3. 构造 `EvaluationContext`
4. 若启用 LLM：

   * 调用 `AnswerEvaluator`
   * 得到结构化评估结果
5. 若 LLM 失败：

   * 降级到现有规则评估器
6. 用 `normalizedScore` 继续走原有 `MasteryUpdateService`
7. 继续记录：

   * `task_attempt`
   * `evidence`
8. 继续调用 `NextActionPolicy`
9. 响应中增加更丰富的结构化字段，便于前端展示：

   * score
   * feedback
   * errorTags
   * strengths
   * weaknesses
   * nextAction

---

## TODO G：升级 Evaluator 设计为“规则保底 + LLM 增强”

请不要直接删掉现有 `EvaluatorService`。
而是重构为：

### 方案建议

* `RuleBasedAnswerEvaluator`
* `LlmAnswerEvaluator`
* `CompositeAnswerEvaluator` 或应用层 fallback 机制

### 要求

规则评估至少处理：

* 空答案
* 字数过短
* 明显偏题
* 必要关键词缺失（适度即可，不要过拟合）

LLM 评估负责：

* 语义理解
* 优缺点提炼
* error_tags 生成
* 反馈生成
* suggested_next_action 推荐

---

## TODO H：补充数据库迁移（Flyway）

请新增 Flyway migration。

### 最少方案

给 `task_attempt` 增加字段：

* `llm_provider`
* `llm_model`
* `prompt_version`
* `token_input`
* `token_output`
* `latency_ms`
* `generation_mode`

### 推荐方案

额外增加 `llm_call_log` 表，字段示例：

* id
* task_attempt_id
* biz_type
* provider
* model
* prompt_template_key
* prompt_version
* request_payload
* response_payload
* parsed_json
* status
* latency_ms
* token_input
* token_output
* created_at

### 要求

* 给出完整 migration SQL
* 与 PostgreSQL 兼容
* 保持命名风格一致
* 不破坏现有数据

---

## TODO I：增强 NextActionPolicy 的输入能力

目前策略主要看分数阈值。
请在尽量不大改现有结构的前提下，让策略可以利用：

* score
* errorTags
* masteryBefore
* masteryAfter
* 最近尝试趋势（能方便拿到就做，拿不到可先留扩展点）

### 最少实现

支持以下判断：

1. 低分 + 核心概念错误 -> `INSERT_REMEDIAL_UNDERSTANDING`
2. 中等分 + 理解不完整 -> `INSERT_TRAINING_VARIANTS`
3. 高分但不稳定 -> `INSERT_TRAINING_REINFORCEMENT`
4. 高分且错误少 -> `ADVANCE_TO_NEXT_NODE`

如果本次改动过大，允许先：

* 保留原阈值策略主干
* 增加对 `errorTags` 的少量增强判断

---

## TODO J：增加配置与开关能力

请新增配置开关，便于本地 / 测试 / 生产切换：

* `app.llm.enabled`
* `app.llm.provider`
* `app.llm.base-url`
* `app.llm.api-key`
* `app.llm.model`
* `app.llm.timeout-ms`
* `app.llm.fallback-to-rule`
* `app.llm.log-request`
* `app.llm.log-response`

要求：

* 本地默认可不开启
* 未配置 API Key 时系统能优雅降级
* 不要让整个服务因缺失 key 启动失败（除非显式要求）

---

## TODO K：补充测试

请为本次改造补充测试，至少包括：

### 单元测试

1. `StageContentGenerator` / Prompt Builder 测试
2. `LlmAnswerEvaluator` JSON 解析测试
3. `CompositeAnswerEvaluator` fallback 测试
4. `NextActionPolicy` 利用 errorTags 的策略测试

### 应用层测试

1. `run` 成功走 LLM 生成
2. `run` LLM 失败后走模板降级
3. `submit` 成功走 LLM 评估
4. `submit` LLM 失败后走规则降级

### 可选增强

如果项目已有 Testcontainers 习惯，可加 PostgreSQL 集成测试；
如果暂时太重，可至少把 repository 相关 TODO 留好注释。

---

## TODO L：补充接口文档与示例响应

请同步补充 OpenAPI 注释或至少整理响应 DTO，使以下接口的返回更清晰：

### `POST /api/task/{taskId}/run`

示例返回按不同 stage 给出结构化 JSON

### `POST /api/task/{taskId}/submit`

示例返回包括：

* score
* normalizedScore
* feedback
* errorTags
* strengths
* weaknesses
* nextAction
* masteryBefore / masteryAfter（如果已有）

---

# 五、实现边界（不要做过头）

本次不要做以下内容，除非实现非常顺手且不影响主线：

1. 不要上 RAG
2. 不要做多轮 agent 编排
3. 不要做复杂会话记忆系统
4. 不要改成 WebSocket
5. 不要额外发明聊天接口
6. 不要大规模重构 repository 层
7. 不要把全部实体都改成 DDD 重模型

本次核心目标只有一句话：

**让现有学习流程后端具备“阶段内容由 LLM 生成、训练答案由 LLM 评估”的真实 AI 能力。**

---

# 六、输出要求（你必须这样交付）

请不要只给建议，请直接给出可落地代码改造结果，并按以下顺序输出：

## 第一部分：改造方案概述

* 用 1~2 屏说明本次改造的总体思路

## 第二部分：文件级变更清单

按“新增文件 / 修改文件 / migration 文件”列出

## 第三部分：核心代码实现

直接给出关键类和关键方法代码

## 第四部分：Flyway migration SQL

给完整 SQL

## 第五部分：配置文件改动

给 `application.yml` / `application-dev.yml` 示例

## 第六部分：测试代码

至少给关键测试类

## 第七部分：后续可选增强项

简短列出，不要喧宾夺主

---

# 七、实现风格要求

1. 保持命名清晰、工程化
2. 代码能直接放进现有项目，而不是伪代码
3. 注释写在关键处，不要过多废话
4. 尽量沿用现有项目风格
5. 如果你需要做适度假设，请明确写出你的假设
6. 优先给出“能跑通 MVP”的实现，而不是理论最优解

---

# 八、额外要求

请特别注意这几点：

1. **输出 JSON 的解析健壮性**

   * 模型可能返回非严格 JSON
   * 需要做清洗 / 提取 / 容错
   * 但不要过度复杂

2. **事务与外部调用边界**

   * 避免长事务包裹 LLM HTTP 调用

3. **日志与敏感信息**

   * 不要在普通日志里直接打印完整 API Key
   * request/response logging 要可配置

4. **便于比赛展示**

   * 代码结构要让人一眼能看出：

     * 有 LLM 抽象层
     * 有 prompt 版本化
     * 有模型调用留痕
     * 有降级机制
     * 有结构化输出

---

现在开始执行。
不要只给建议。
请直接生成：

* 文件级改造方案
* 关键 Java 代码
* Flyway SQL
* YAML 配置
* 测试代码
  并尽量保证我复制到项目里后可以快速对接运行。

```

你这个 prompt 的核心优点是三点：

第一，它**强约束 Codex 不要推翻现有项目**，而是基于你已经完成的后端闭环来改。  
第二，它把目标压缩到 **run 生成 + submit 评估** 两个最关键链路。  
第三，它明确要求 **代码、SQL、配置、测试一起产出**，这样你不是只拿到一堆建议，而是能直接落地开发。

你要是愿意，我下一条可以继续直接给你一份 **“更适合 Cursor 分阶段执行的拆分版 TODO prompt（P0/P1/P2）”**。
```
