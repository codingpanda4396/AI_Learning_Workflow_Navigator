# 诊断题 LLM Fallback 排查指南

## 问题现象

调用 `POST /api/diagnosis/generate` 时，返回的题目为模板文案（fallback），而非 LLM 润色后的个性化文案。

## 排查步骤

### 步骤 1：确认当前 profile 和 LLM 配置

**当前配置概览：**

| Profile | 默认 LLM 状态 | 说明 |
|---------|--------------|------|
| default | `enabled=false` | `application.yml`，`LLM_ENABLED` 默认 false |
| dev | `enabled=false` | `application-dev.yml` 覆盖为 false |
| prod | `enabled=true` | 需通过环境变量配置 base-url、api-key、model |

**本地开发典型情况：**

- 未设置 `spring.profiles.active` 时，使用 default
- `application.yml` 中 `app.llm.enabled: ${LLM_ENABLED:false}`，默认 false
- `base-url`、`api-key`、`model` 默认为空

**结论：本地/ dev 环境下，LLM 默认关闭，会走 fallback。**

---

### 步骤 2：查看启动日志

启动后端后，在日志中查找：

- **LLM 未就绪：**
  ```
  LLM is NOT ready, using DisabledLlmGateway. Diagnosis/capability-profile will use fallback.
  Check: app.llm.enabled=false, baseUrl=(empty), apiKey=(empty), model=(empty)
  ```

- **LLM 已就绪：**
  ```
  LLM is ready. Using OpenAiCompatibleLlmGateway with baseUrl=...
  ```

若看到 "LLM is NOT ready"，则 fallback 是因为配置未就绪。

---

### 步骤 3：Fallback 时的具体原因日志

调用诊断生成接口时，若发生 fallback，会在日志中看到类似信息：

| 日志内容 | 可能原因 |
|----------|----------|
| `LLM enhance failed, reason=LLM is not enabled or not fully configured.` | 使用 DisabledLlmGateway，配置未就绪 |
| `LLM enhance failed, reason=LLM provider call timed out` | 网络超时 |
| `LLM enhance failed, reason=LLM provider returned 4xx/5xx` | API 错误或 key 无效 |
| `LLM response invalid, questions size=X, expected=5` | 返回题目数量不符合要求 |
| `LLM returned unknown questionId=xxx` | 返回的 questionId 与模板不一致 |
| `LLM modified options count for questionId=xxx` | 修改了选项数量 |

---

### 步骤 4：启用 LLM 并验证

**方式 A：环境变量（推荐）**

```bash
# Windows PowerShell
$env:LLM_ENABLED="true"
$env:LLM_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"  # 或你的 API 地址
$env:LLM_API_KEY="sk-xxx"
$env:LLM_MODEL="qwen3.5-plus"
mvn spring-boot:run
```

```bash
# Linux/macOS
export LLM_ENABLED=true
export LLM_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export LLM_API_KEY="sk-xxx"
export LLM_MODEL="qwen3.5-plus"
mvn spring-boot:run
```

**方式 B：application-local.yml**

1. 复制 `application-local.yml.example` 为 `application-local.yml`
2. 配置 `app.llm.enabled: true` 以及 `base-url`、`api-key`、`model`
3. 重启应用

**验证：**

- 启动日志出现 `LLM is ready`
- 调用诊断生成接口后，题目 `title` 与 `copy.title` 一致（表示使用 LLM 润色结果）

---

## 配置参考

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| LLM_ENABLED | 是否启用 LLM | false |
| LLM_BASE_URL | API 基础地址 | 空 |
| LLM_API_KEY | API Key | 空 |
| LLM_MODEL | 模型名称 | 空 |
| LLM_LOG_REQUEST | 是否打印请求日志 | false |
| LLM_LOG_RESPONSE | 是否打印响应日志 | false |

排查时可临时开启 `LLM_LOG_REQUEST` 和 `LLM_LOG_RESPONSE`，以便确认是否发起请求及返回内容。
