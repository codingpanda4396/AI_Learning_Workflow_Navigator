# LLM MVP2 使用教程

本文档说明如何在当前项目中启用并使用 LLM 增强能力（`run` 生成 + `submit` 评估）。

## 1. 功能概览

启用后：

1. `POST /api/task/{taskId}/run`：按任务阶段调用 LLM 生成结构化 JSON。
2. `POST /api/task/{taskId}/submit`：调用 LLM 对训练答案做结构化评估。
3. LLM 异常时可自动降级到规则/模板逻辑（由配置控制）。
4. 调用元数据会落到 `task_attempt`，调用留痕会写入 `llm_call_log`。

## 2. 配置入口（API Key 在哪里配）

项目支持两种方式：

1. 环境变量（推荐）
2. `application-local.yml`（本地开发）

配置键如下（前缀 `app.llm`）：

- `enabled`
- `provider`
- `base-url`
- `api-key`
- `model`
- `timeout-ms`
- `fallback-to-rule`
- `log-request`
- `log-response`

在 `application.yml` 中已映射为环境变量：

- `LLM_ENABLED`
- `LLM_PROVIDER`
- `LLM_BASE_URL`
- `LLM_API_KEY`
- `LLM_MODEL`
- `LLM_TIMEOUT_MS`
- `LLM_FALLBACK_TO_RULE`
- `LLM_LOG_REQUEST`
- `LLM_LOG_RESPONSE`

## 3. 本地快速启动（推荐流程）

### 3.1 准备本地配置文件

在 `backend` 目录创建 `application-local.yml`（如果已有可直接改），示例：

```yml
app:
  llm:
    enabled: true
    provider: openai-compatible
    base-url: https://api.openai.com/v1
    api-key: sk-xxxx
    model: gpt-4o-mini
    timeout-ms: 15000
    fallback-to-rule: true
    log-request: false
    log-response: false
```

### 3.2 启动后端

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

## 4. 用环境变量配置（不改文件）

Windows PowerShell 示例：

```powershell
$env:LLM_ENABLED="true"
$env:LLM_PROVIDER="openai-compatible"
$env:LLM_BASE_URL="https://api.openai.com/v1"
$env:LLM_API_KEY="sk-xxxx"
$env:LLM_MODEL="gpt-4o-mini"
$env:LLM_FALLBACK_TO_RULE="true"
cd backend
mvn spring-boot:run
```

## 5. 如何调用接口

### 5.1 任务生成（run）

```bash
curl -X POST "http://127.0.0.1:8080/api/task/1002/run"
```

返回中的 `output` 为结构化 JSON（按 stage 不同字段不同）。

### 5.2 训练提交（submit）

```bash
curl -X POST "http://127.0.0.1:8080/api/task/1003/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "user_answer":"这是我的作答内容"
  }'
```

返回会包含：

- `score`
- `normalized_score`
- `error_tags`
- `feedback`
- `strengths`
- `weaknesses`
- `next_action`

## 6. 如何确认是否真的走了 LLM

连接数据库后执行：

```sql
-- 最近的尝试记录
select id, task_id, generation_mode, llm_provider, llm_model, prompt_version, token_input, token_output, latency_ms, created_at
from task_attempt
order by id desc
limit 20;

-- LLM 调用留痕
select id, task_attempt_id, biz_type, provider, model, prompt_template_key, status, latency_ms, created_at
from llm_call_log
order by id desc
limit 20;
```

判断标准：

1. `task_attempt.generation_mode = 'LLM'`：表示本次直接走了 LLM。
2. `task_attempt.generation_mode = 'TEMPLATE_FALLBACK'` 或 `RULE_FALLBACK`：表示已降级。
3. `llm_call_log` 有 `TASK_RUN` / `TASK_SUBMIT` 且 `status='SUCCEEDED'`：表示调用成功。

## 7. 常见问题

### 7.1 没配 API Key 会怎样？

不会导致服务启动失败。系统会按配置降级到规则/模板逻辑。

### 7.2 provider 支持哪些？

当前按 OpenAI-compatible 协议接入，兼容同协议网关（如 OpenAI / DeepSeek / OpenRouter 等），主要依赖 `base-url` 与 `model`。

### 7.3 线上建议配置

1. 使用环境变量注入 `LLM_API_KEY`，不要写死在仓库文件中。
2. `fallback-to-rule=true`，确保第三方波动时服务可用。
3. 生产建议 `log-request=false`、`log-response=false`，避免敏感数据泄露。

