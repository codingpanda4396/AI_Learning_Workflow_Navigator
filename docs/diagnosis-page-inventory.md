# 诊断页完整盘点

> 文档日期：2026-03-14  
> 目的：系统梳理诊断页的入口、组件、接口、LLM 调用、持久化与个性化能力现状，支撑后续“个性化解释”改造。

---

## 一、诊断页当前前端入口

- **入口页面**：`HomeView.vue`（首页）
- **跳转方式**：用户填写学习目标（goal、course、chapter）后点击「开始学习」→ `sessionStore.createSession()` 创建会话 → `router.push` 到 `/diagnosis/${sessionId}`
- **URL 形态**：`/diagnosis/:sessionId?`，query 携带 `goal`、`course`、`chapter`

---

## 二、诊断页对应文件与结构

### 2.1 View 文件

| 文件 | 用途 |
|------|------|
| `frontend/src/views/DiagnosisView.vue` | 主诊断页，承载问答流程与结果展示 |
| `frontend/src/views/DiagnosisResultView.vue` | 诊断结果展示容器，组合多个子组件 |

### 2.2 组件树

```
DiagnosisView.vue
├── AppShell
├── DiagnosisGoalSummaryCard（非结果态：展示 goal / course / chapter）
├── LoadingState（生成中）
├── ErrorState（错误态）
├── DiagnosisResultView（结果态）
│   ├── DiagnosisHeroCard（等级 + summary）
│   ├── DiagnosisExplanationCard（summary + strengths + weaknesses）
│   └── NextStepActionCard（推荐下一步 + 按钮）
├── DiagnosisQuestionCard（答题态：当前题目）
├── DiagnosisProgressCard（答题态：进度条）
└── 底部操作栏（上一题 / 下一题 / 完成诊断）
```

### 2.3 Store

- **文件**：`frontend/src/stores/diagnosis.ts`
- **状态**：`diagnosisId`、`sessionId`、`questions`、`currentQuestionIndex`、`answers`、`capabilityProfile`、`insights`、`nextAction`、`fallback`、`metadata`、`status`、`loading`、`submitting`、`error`
- **Actions**：`generateDiagnosis(sessionId)`、`updateAnswer()`、`setCurrentQuestionIndex()`、`submitDiagnosis()`、`reset()`

### 2.4 API 调用位置

| 调用 | 位置 | 方法 |
|------|------|------|
| `generateDiagnosisApi(sessionId)` | `diagnosis.ts` 的 `generateDiagnosis` action | `POST /api/diagnosis/sessions` |
| `submitDiagnosisApi(diagnosisId, answers)` | `diagnosis.ts` 的 `submitDiagnosis` action | `POST /api/diagnosis/sessions/{diagnosisId}/submissions` |

### 2.5 路由

- **路径**：`/diagnosis/:sessionId?`
- **名称**：`diagnosis`
- **组件**：`DiagnosisView.vue`
- **定义位置**：`frontend/src/router/index.ts`

---

## 三、诊断页接口契约

### 3.1 生成诊断题接口

| 项目 | 内容 |
|------|------|
| **方法 / 路径** | `POST /api/diagnosis/sessions` |
| **请求体** | `{ "sessionId": number }` |
| **响应字段** | `diagnosisId`、`sessionId`、`status`、`questions`、`nextAction`、`fallback`、`metadata` |
| **状态** | `GENERATED` |

**questions 结构**：`questionId`、`dimension`、`type`、`required`、`options`、`title`、`description`、`placeholder`、`submitHint`、`sectionLabel`

### 3.2 提交诊断答案接口

| 项目 | 内容 |
|------|------|
| **方法 / 路径** | `POST /api/diagnosis/sessions/{diagnosisId}/submissions` |
| **请求体** | `{ "answers": DiagnosisAnswer[] }` |
| **answers 结构** | `questionId`、`selectedOptionCode`（单选）/ `selectedOptionCodes`（多选）/ `text`（文本） |
| **响应字段** | `diagnosisId`、`sessionId`、`status`、`capabilityProfile`、`insights`、`nextAction`、`fallback`、`metadata` |
| **状态** | `PROFILED` |

### 3.3 获取诊断结果接口（独立）

| 项目 | 内容 |
|------|------|
| **方法 / 路径** | `GET /api/capability-profile/{sessionId}` |
| **说明** | 按 session 取最新能力画像，诊断页当前**未使用**，结果来自 submit 响应 |
| **响应** | `CapabilityProfileResponse`（sessionId、capabilityProfile、insights） |

### 3.4 请求 / 响应 / 状态汇总

| 字段类型 | 字段 |
|----------|------|
| **请求** | `sessionId`、`answers[].questionId`、`answers[].selectedOptionCode` / `selectedOptionCodes` / `text` |
| **响应** | `capabilityProfile`（currentLevel、strengths、weaknesses、learningPreference、timeBudget、goalOrientation）、`insights`（summary、planExplanation）、`fallback`（applied、reasons、contentSource） |
| **状态** | `GENERATED`、`SUBMITTED`、`PROFILED` |

---

## 四、诊断阶段 LLM 调用点

### 4.1 Service / Usecase / Orchestrator

| 层级 | 类 | 职责 |
|------|-----|------|
| **应用层** | `DiagnosisService` | 编排 create + submit 全流程 |
| **应用层** | `DiagnosisQuestionCopyLlmService` | 题目文案润色（LLM） |
| **应用层** | `CapabilityProfileSummaryLlmService` | 能力总结文案生成（LLM） |
| **领域层** | `CapabilityProfileBuilder` | 能力画像草稿（规则） |
| **领域层** | `DiagnosisTemplateFactory` | 诊断题结构（规则） |
| **领域层** | `CapabilityProfileSummaryGenerator` | 能力总结 fallback 文案（规则） |

### 4.2 Prompt Template

| Key | 用途 | 位置 |
|-----|------|------|
| `DIAGNOSIS_QUESTION_V1` | 诊断题文案润色 | `DiagnosisQuestionCopyLlmService` 内联 prompt |
| `CAPABILITY_SUMMARY_V1` | 能力总结文案生成 | `CapabilityProfileSummaryLlmService` 内联 prompt |

当前未使用 `DefaultPromptTemplateProvider` 的模板文件，prompt 均在代码中内联构造。

### 4.3 Response Parser

| 服务 | 解析方式 |
|------|----------|
| `DiagnosisQuestionCopyLlmService` | `LlmJsonParser.parse()` → 解析 `questions` 数组，逐项校验 `questionId`、`options` 数量 |
| `CapabilityProfileSummaryLlmService` | `LlmJsonParser.parse()` → 读取 `summary`、`planExplanation` |

### 4.4 Fallback 逻辑

| 场景 | 触发条件 | 行为 |
|------|----------|------|
| 题目润色 | LLM 异常 / JSON 解析失败 / 输出结构不符合预期 | 使用 `DiagnosisTemplateFactory` + `DiagnosisQuestionCopyFactory` 的规则题 |
| 能力总结 | LLM 异常 / `summary` 或 `planExplanation` 为空 | 使用 `CapabilityProfileSummaryGenerator.buildFallback(draft)` 的规则文案 |

### 4.5 日志埋点

- **组件**：`LlmCallLogger`
- **阶段**：`LlmStage.DIAGNOSIS_QUESTION_COPY`、`LlmStage.CAPABILITY_SUMMARY`
- **日志类型**：`logStart`、`logSuccess`、`logFallback`、`logFailure`、`logStructuredOutputFailure`

### 4.6 Token 统计

- **位置**：`OpenAiCompatibleLlmGateway` 解析 `usage` → `LlmCallMetrics`（latencyMs、promptTokens、completionTokens、totalTokens）
- **输出**：`LlmCallLogger.logSuccess()` 中输出 `promptTokens`、`completionTokens`、`totalTokens`、`finishReason`、`truncated`

---

## 五、诊断阶段“到底让 LLM 做了什么”

| 环节 | 是否 LLM | 职责 |
|------|----------|------|
| **生成问题** | ❌ 否 | 题目结构、选项、维度由 `DiagnosisTemplateFactory` + `ContractCatalog` 规则生成 |
| **润色文案** | ✅ 是 | `DiagnosisQuestionCopyLlmService`：根据 goal/course/chapter 润色 title、description、placeholder、submitHint、sectionLabel、options.label |
| **能力总结** | ✅ 是 | `CapabilityProfileSummaryLlmService`：把 `CapabilityProfileDraft` 翻译成 `summary`、`planExplanation` 自然语言 |
| **结构化画像** | ❌ 否 | `CapabilityProfileBuilder` 规则计算：currentLevel、strengths、weaknesses、learningPreference、timeBudget、goalOrientation |

**结论**：LLM 只负责**文案润色**和**总结表达**，不参与题目生成和结构化能力判断。

---

## 六、诊断结果持久化结构

### 6.1 表结构（V19）

| 表 | 字段概要 |
|------|----------|
| **diagnosis_session** | id、learning_session_id、user_id、status、generated_questions_json、started_at、completed_at |
| **diagnosis_answer** | id、diagnosis_session_id、question_id、dimension、answer_type、answer_value_json、raw_text |
| **capability_profile** | id、learning_session_id、user_id、source_diagnosis_id、current_level、strengths_json、weaknesses_json、preferences_json、constraints_json、summary_text、version |

### 6.2 实体与维度

| 实体 | 说明 |
|------|------|
| **diagnosis_session** | 单次诊断会话，含题目 JSON |
| **diagnosis_answer** | 用户每道题的答案（含 dimension） |
| **capability_profile** | 能力画像：level、strengths、weaknesses、learningPreference、timeBudget、goalOrientation、summaryText、planExplanation |

### 6.3 维度评分 / 风险标签 / 学习偏好

| 字段类型 | 有无 | 说明 |
|----------|------|------|
| **维度评分** | ❌ 无 | 未按 dimension 存储量化分数，仅有 answer 原始值 |
| **风险标签** | ❌ 无 | 诊断阶段不产出 risk flags，路径规划阶段 `PersonalizedPathPlannerService` 有 `LOW_GOAL_DIAGNOSIS` 等 |
| **学习偏好** | ✅ 有 | `learningPreference`（CONCEPT_FIRST / EXAMPLE_FIRST / PRACTICE_FIRST / PROJECT_DRIVEN） |
| **时间预算** | ✅ 有 | `timeBudget`（LIGHT / STANDARD / HEAVY 等） |
| **目标导向** | ✅ 有 | `goalOrientation`（EXAM / INTERVIEW / PROJECT / COURSE） |

---

## 七、诊断页可用于“个性化解释”的事实源

| 事实源 | 已有 | 说明 |
|--------|------|------|
| **用户目标** | ✅ 有 | 来自 route query `goal`，以及 `LearningSession.goalText` |
| **用户答案** | ✅ 有 | 存于 `diagnosis_answer`，可反查 |
| **各维度得分** | ⚠️ 部分 | 无量化分数，仅有维度下答案 codes（如 FOUNDATION=BEGINNER） |
| **风险标签** | ❌ 无 | 诊断阶段不产出 |
| **历史训练结果** | ❌ 无 | 诊断页不拉取训练/练习数据 |
| **历史 mastery** | ❌ 无 | 诊断页不拉取 mastery 数据 |
| **当前课程 / 章节** | ✅ 有 | route query `course`、`chapter`，以及 `LearningSession` |

---

## 八、诊断页当前 UI 已展示内容

| 区域 | 组件 | 展示内容 |
|------|------|----------|
| **标题区** | `DiagnosisGoalSummaryCard` | 目标导向、学习主题、当前章节、流程说明 |
| **问题列表** | `DiagnosisQuestionCard` | 单题展示，含 dimension、title、description、options、placeholder |
| **诊断总结** | `DiagnosisHeroCard` + `DiagnosisExplanationCard` | 能力等级、summary、strengths、weaknesses（合并为 reasons 列表） |
| **推荐下一步** | `NextStepActionCard` | planExplanation 作为 suggestion，按钮「查看 AI 为你生成的学习路径」 |
| **“为什么这样判断”** | ❌ 未展示 | 无显式“依据你的 X 回答，得出 Y 结论”的因果说明 |

---

## 九、诊断页最大问题列表

### 9.1 已有能力

- 题目结构（维度、选项）由规则生成，稳定可控
- 题目文案可 LLM 润色，提升表达
- 能力画像（level、strengths、weaknesses、偏好）由规则计算
- 总结与计划说明可由 LLM 生成或规则 fallback
- 用户答案、目标、课程/章节等基础事实可查

### 9.2 缺失能力

- 无“依据某题某答案→得出某结论”的因果解释
- 无按维度的量化评分（仅选项 code）
- 无风险标签与个性化风险说明
- 无历史 mastery / 训练结果融合，无法做“与上次对比”
- 未展示 fallback / contentSource，用户不知道是 AI 还是规则生成

### 9.3 为什么不足以体现“个性化”

- 结论（strengths/weaknesses）多为规则模板，与具体题目、选项、课程/章节的关联不显式
- 无“因为你在 FOUNDATION 选了 BEGINNER，所以……”式的解释链
- summary、planExplanation 虽可 LLM 生成，但缺少结构化事实支撑，难以做细粒度“个性化解释”

### 9.4 最小改造方案

1. **增加“解释链”数据结构**：在 submit 响应中增加 `reasoningSteps`，形如 `[{ dimension, questionId, selectedAnswer, inferredConclusion }]`，供前端展示“为什么这样判断”。
2. **在 CapabilityProfileBuilder 中标注来源**：为每条 strength/weakness 标注来自哪个 dimension / 哪道题，便于生成解释。
3. **前端增加“解释”展示**：在 `DiagnosisExplanationCard` 或新组件中，展示“基于你的回答”的因果说明。
4. **可选**：在 `CapabilityProfileSummaryLlmService` 的 prompt 中显式传入 `reasoningSteps`，让 LLM 生成更贴合的 summary。

---

## 十、文件索引

| 分类 | 路径 |
|------|------|
| 前端 View | `frontend/src/views/DiagnosisView.vue`、`DiagnosisResultView.vue` |
| 前端组件 | `frontend/src/components/diagnosis/`、`frontend/src/components/panels/` |
| 前端 Store | `frontend/src/stores/diagnosis.ts` |
| 前端 API | `frontend/src/api/modules/diagnosis.ts` |
| 前端类型 | `frontend/src/types/diagnosis.ts` |
| 后端 Controller | `backend/.../api/controller/DiagnosisController.java` |
| 后端 Service | `backend/.../application/service/DiagnosisService.java`、`DiagnosisQuestionCopyLlmService.java`、`CapabilityProfileSummaryLlmService.java` |
| 领域服务 | `backend/.../domain/service/CapabilityProfileBuilder.java`、`DiagnosisTemplateFactory.java`、`CapabilityProfileSummaryGenerator.java` |
| 持久化 | `backend/.../infrastructure/persistence/JdbcDiagnosisSessionRepository.java`、`JdbcDiagnosisAnswerRepository.java`、`JdbcCapabilityProfileRepository.java` |
| DB 迁移 | `backend/src/main/resources/db/migration/V19__create_diagnosis_and_capability_profile.sql` |
