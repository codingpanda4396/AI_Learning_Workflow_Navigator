# Learning Plan JSON Hardening

## 问题背景

Learning Plan 链路里，LLM 调用本身成功，但下游 `LlmJsonParser` 在解析结构化输出时失败，最终触发 rule fallback。原链路缺少足够的输出契约约束、解析分层兜底和可诊断日志，因此很难判断失败到底是空响应、额外文本污染、JSON 语法错误，还是 schema 不匹配。

## 根因分析

1. `LlmJsonParser` 只有一次简化清洗和一次对象提取，面对 code fence、前后缀说明、轻微结尾污染时容易失败。
2. parser 失败日志过于粗糙，缺少 stage、model、traceId/requestId、原始长度、样本预览、提取范围和修复步骤。
3. Learning Plan prompt 对“只输出一个 JSON object”的约束不够强，字段契约也没有写到字段级。
4. `LearningPlanResultValidator` 对 schema 错误没有输出精确字段路径，导致 fallback 原因只能模糊归类。
5. orchestrator/service 只知道“fallback 了”，但不清楚具体 reason code，也无法稳定统计。

## 新解析策略

### 第 1 层：严格解析

直接对 LLM 原始文本做 JSON parse。

### 第 2 层：提取首个 JSON object

如果原文混有说明文字或包装文本，提取首个最外层 `{ ... }` 再解析。

### 第 3 层：轻量修复

在可追踪前提下做保守修复：

- 去掉 ```` ```json ```` / ```` ``` ````
- 截断到首个 JSON object
- trim 前后空白
- 修复明显缺失的闭合 `}`
- 去掉对象/数组结尾处明显的 trailing comma

所有修复步骤都会写入诊断日志。

### 第 4 层：schema 校验

JSON 本身合法后，再对 Learning Plan 合同做字段级校验，明确输出：

- 缺失字段
- 类型错误
- stage 枚举错误
- task 数量不匹配
- 长度/范围约束不满足

schema 错误会以字段路径形式输出，例如 `$.task_preview[0].stage invalid enum value: INVALID`。

## fallback 策略

fallback 保留，但 reason code 变得更明确：

- `LLM_NOT_READY`
- `JSON_EMPTY_RESPONSE`
- `JSON_EXTRA_TEXT`
- `JSON_PARSE_ERROR`
- `JSON_SCHEMA_MISMATCH`
- 其他既有 LLM API / timeout / unknown reason

`LearningPlanOrchestrator` 会在 fallback 时记录：

- 是否使用了 LLM 结果
- 是否进入 fallback
- fallback reason code
- traceId / requestId / model
- parser/schema 诊断摘要

`LearningPlanService` 也会记录最终 `planSource`、`fallbackApplied` 和 `fallbackReasons`，便于后续统计 fallback 占比。

接口响应新增轻量字段：

- `planSource: "LLM" | "RULE_FALLBACK"`

## 后续优化建议

1. 为 `LLM_STRUCTURED_OUTPUT_FAILURE` 增加独立 metric，按 `stage + reason` 统计。
2. 对常见污染样本沉淀回归测试样本库，避免 parser 回退。
3. 如果后续模型支持更强 schema 模式，可把 Learning Plan 升级为更严格的 provider-side schema enforcement。
4. 可按 reason code 做看板，优先优化 `JSON_EXTRA_TEXT` 和 `JSON_SCHEMA_MISMATCH` 高发场景。
