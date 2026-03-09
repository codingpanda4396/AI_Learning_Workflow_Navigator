# 训练题生成与获取最小闭环

## 范围
在现有四阶段任务流基础上，新增 TRAINING 阶段题目生成与获取闭环：
- 题目获取：`GET /api/session/{sessionId}/tasks/{taskId}/practice-items`
- 题目生成：`POST /api/session/{sessionId}/tasks/{taskId}/practice-items/generate`
- 生成策略：`LLM 优先 + Rule fallback`

## 服务能力
`PracticeService` 新增并实现：
- `listPracticeItems(sessionId, taskId, userId)`
- `generatePracticeItems(sessionId, taskId, userId)`
- `getOrCreatePracticeItems(sessionId, taskId, userId)`

关键约束：
- task 必须属于 session
- session 必须属于 user
- task 必须是 `TRAINING` 阶段，否则抛业务冲突异常

## 生成链路
1. `getOrCreatePracticeItems` 先查 `practice_item`，存在则直接复用。
2. 不存在时进入 `generatePracticeItems`：
   - 优先尝试 `LlmPracticeGenerator`
   - LLM 输出经 `LlmJsonParser` + `PromptOutputValidator.validatePracticeGeneration` 校验
   - 校验/解析/调用任一步失败且 `fallbackToRule=true` 时自动切到 `RulePracticeGenerator`
3. 生成结果入库 `practice_item`，并记录 `learning_event`：`PRACTICE_ITEMS_GENERATED`

## 题型
最小支持 3 种：
- `SINGLE_CHOICE`
- `TRUE_FALSE`
- `SHORT_ANSWER`

## LLM 模板
新增 `PromptTemplateKey.PRACTICE_GENERATION_V1` 和模板注册。
输出 schema 要求：
- `items` 固定 3 题
- 至少各包含 1 题 `SINGLE_CHOICE/TRUE_FALSE/SHORT_ANSWER`
- 含字段：`question_type/stem/options/standard_answer/explanation/difficulty`

## 日志与事件
- 服务日志记录 source、fallback、数量、provider/model/promptVersion
- 事件记录 `llm_parse_succeeded`、`fallback_triggered`、token/latency、生成数量
