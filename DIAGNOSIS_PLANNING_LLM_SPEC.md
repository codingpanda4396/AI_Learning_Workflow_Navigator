# 诊断页与规划页 LLM 协作约束（内部规范）

## 背景与目标

本规范用于约束诊断页（`/api/diagnosis` + `DiagnosisView.vue`）与规划页（`/api/learning-plans` + `LearningPlanView.vue`）中规则层与 LLM 的职责边界，防止后续开发将业务决策下放给 LLM，导致行为不可控、不可复现、不可回归测试。

适用范围：

- 后端：`DiagnosisService`、`DiagnosisQuestionCopyLlmService`、`CapabilityProfileSummaryLlmService`、`PlanningContextAssembler`、`RuleBasedPlanBuilder`、`LearningPlanOrchestrator`、`LearningPlanService`
- 前端：`stores/diagnosis.ts`、`stores/learningPlan.ts`、`types/diagnosis.ts`、`types/learningPlan.ts`、`DiagnosisView.vue`、`LearningPlanView.vue`

## 总体设计原则

## **核心结论：LLM 不负责业务决策，只负责个性化解释与表达。**

强制原则：

1. 诊断题结构、能力画像、学习路径、节点顺序、节奏建议等属于规则层。
2. 题目文案、能力总结、规划说明、`whyStartHere` 等属于 LLM 解释层。
3. 所有解释都必须有结构化事实支撑。
4. 所有 LLM 输出都必须有 fallback。
5. API 响应必须显式返回内容来源（`contentSource`/`strategy`）和 fallback 状态，前端不得隐式猜测。

## 诊断页：规则层职责

规则层在诊断流程中必须负责以下内容，LLM 不得覆盖：

- 问卷骨架生成：`DiagnosisTemplateFactory.buildQuestions()` 负责题目集合、`questionId`、`dimension`、`type`、`required`、选项集合与顺序。
- 答案合法性校验：`DiagnosisService.validateAnswers()/validateAnswerValue()/assertAllowedOption()` 负责必答、重复、选项合法性和类型一致性。
- 选项标准化：`ContractCatalog.diagnosisOptionCode()/diagnosisOptionLabel()` 负责 code-label 映射，防止自由文本污染领域模型。
- 能力画像决策：`CapabilityProfileBuilder.build()` 负责 `currentLevel/strengths/weaknesses/learningPreference/timeBudget/goalOrientation` 的业务推导。
- 可解释事实组装：`DiagnosisExplanationAssembler.assemble()` 负责 `reasoningSteps/strengthSources/weaknessSources`，作为诊断解释的事实证据层。
- 跳转动作控制：`DiagnosisService.buildNextAction()` 负责固定业务动作（当前为 `PATH_PLAN` + `/plan`）。
- 版本与持久化：`CapabilityProfile.version`、`DiagnosisSession.status` 状态流转（`GENERATED -> SUBMITTED -> PROFILED`）由规则层单点维护。

## 诊断页：LLM 职责

诊断页 LLM 仅可处理文案表达，不得进行业务决策：

- 题目文案润色：`DiagnosisQuestionCopyLlmService` 仅允许修改 `title/description/placeholder/submitHint/sectionLabel/options.label`。
- 能力摘要文案：`CapabilityProfileSummaryLlmService` 仅输出 `summary/planExplanation` 自然语言表达。
- 约束要求：
  - 不得修改题目结构字段：`questionId/dimension/type/required`
  - 不得增删选项，不得改变选项 code
  - 不得直接输出能力等级、规划路径、下一步动作等决策字段
  - 必须输出 JSON，禁止夹带解释性前后文

## 规划页：规则层职责

规则层在规划流程中必须负责以下内容，LLM 不得覆盖：

- 规划上下文组装：`PlanningContextAssembler.assemble()` 负责 `nodes/recentErrorTags/recentScores/weakPointLabels/learnerProfileSummary/adjustments`。
- 路径与起点决策：`RuleBasedPlanBuilder.build()` 负责 `recommendedStartNodeId`、节点窗口、路径顺序、时长估算、任务阶段模板。
- 业务不可变结构：
  - `pathPreview` 节点集合与顺序由规则层决定
  - `taskPreview` 阶段必须为 `STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION`，各 1 条
  - 节奏（`recommendedPace`）、总时长、节点数、阶段数由规则层计算
- 计划落库与会话创建：`LearningPlanService.confirm()` 负责 `LearningSession`、`Task` 创建与状态推进。
- 文案兜底解释：`LearningPlanExplanationAssembler` 基于结构化上下文生成 `whyStartHere/keyWeaknesses/priorityNodes` 的规则版本解释。

## 规划页：LLM 职责

规划页 LLM 仅可对“既定路径”做个性化表达，不得改动路径决策：

- 编排入口：`LearningPlanOrchestrator.preview()` 先产出 `rulePreview`，LLM 仅在其上“润色”。
- 允许 LLM 输出字段（经校验后合并）：`headline/reasons/focuses/task_preview`。
- 明确禁止：
  - 新增或替换路径节点
  - 修改 `recommendedStartNodeId`、`pathPreview` 顺序、节点集合
  - 修改阶段数量与顺序（必须是四阶段）
  - 输出与 schema 不符的自由文本
- 合并策略：`LearningPlanOrchestrator.merge()` 只采用 LLM 的解释性字段，路径与结构字段继续使用规则层结果。

## 新增解释字段说明

为保证“解释可追溯到事实”，诊断与规划侧统一采用“解释字段 + 事实字段”配对策略。

诊断侧（已存在）：

- 解释字段：`insights.summary`、`insights.planExplanation`
- 事实字段：`reasoningSteps[]`、`strengthSources[]`、`weaknessSources[]`
- 约束：前端展示解释文案时，必须同时可访问至少一个事实字段（当前 `DiagnosisResultView` 已传入三类 source/step 字段）。

规划侧（已存在）：

- 解释字段：`whyStartHere`、`reasons[]`、`summary.personalizedHeadline`
- 事实字段：`context.diagnosisSummary`、`keyWeaknesses[]`、`priorityNodes[]`、`pathNodes[]`、`taskPreviews[]`
- 约束：`whyStartHere` 必须能够被 `keyWeaknesses` 或 `reasons` 或路径事实解释，不允许“无证据结论”。

推荐后续补充（规划 API vNext）：

- `whyStartHereEvidence[]`：每条包含 `type`（`WEAKNESS|PATH|RECENT_ERROR|SCORE`）、`sourceId`、`text`
- `reasonEvidenceMap`：`reason.key -> evidence[]`

## fallback 规范

任何 LLM 参与点必须遵循“失败可降级、降级可识别、降级不阻断主链路”。

统一要求：

1. 先产出规则结果，再尝试 LLM，失败回退规则结果。
2. 返回体必须携带 fallback 信号：
   - 诊断：`fallback.applied`、`fallback.reasons`、`fallback.contentSource`
   - 规划：`fallbackApplied`、`fallbackReasons`、`contentSource`
3. fallback reason 必须使用标准枚举（`LlmFallbackReason`）或可映射等价码。
4. fallback 不得影响核心业务流程（诊断提交、计划确认、任务创建）。

当前典型 fallback 场景：

- LLM 不可用（`LLM_NOT_READY`）
- 输出截断（`OUTPUT_TRUNCATED`）
- JSON 解析失败（`JSON_PARSE_ERROR/JSON_EXTRA_TEXT/JSON_EMPTY_RESPONSE`）
- 结构校验失败（`JSON_SCHEMA_MISMATCH/MISSING_REQUIRED_FIELDS`）
- 超时或网关异常（`LLM_TIMEOUT/LLM_API_ERROR/UNKNOWN_ERROR`）

## observability 规范

LLM 可观测性必须覆盖“是否调用、是否成功、为何回退、消耗多少 token、来源是什么”。

后端日志与指标：

- 统一使用 `LlmCallLogger` 记录：
  - `LLM_CALL_START`
  - `LLM_CALL_SUCCESS`（含 `promptTokens/completionTokens/totalTokens/finishReason/truncated`）
  - `LLM_CALL_FALLBACK`（含 `reason` 与 `latencyMs`）
  - `LLM_STRUCTURED_OUTPUT_FAILURE`
- 统一透传 `traceId/requestId`（`TraceContext` + `ResponseMetadata`）。
- Micrometer 指标至少保留：
  - `llm.call.total`
  - `llm.call.success`
  - `llm.call.fallback`
  - `llm.call.failure`
  - `llm.call.latency`

接口返回约束：

- 诊断接口 `ApiEnvelope.metadata.strategy` 使用 `fallback.contentSource`。
- 规划接口 `ApiEnvelope.metadata.strategy` 使用 `LearningPlanPreviewResponse.contentSource.code`。
- 前端禁止仅凭“是否有文案”判断是否来自 LLM，必须读取 `contentSource` 与 fallback 字段。

敏感数据约束：

- 日志记录 token 与错误摘要，不记录完整用户原始作答文本。
- `requestPayload/responsePayload` 若落库或输出调试日志，必须遵循脱敏策略（用户输入、鉴权信息、可能的个人标识字段）。

## 前端展示约束

诊断页（`DiagnosisView.vue` + `stores/diagnosis.ts`）：

- 必须展示或可触达 fallback 信息（`fallback.applied/reasons`）。
- 诊断结果说明组件不得只显示 `insights`，应同时保留 `reasoningSteps/strengthSources/weaknessSources` 的展示入口。
- 当 `contentSource=RULE_FALLBACK` 时，文案可简化，但不得改变能力画像与下一步动作。

规划页（`LearningPlanView.vue` + `stores/learningPlan.ts`）：

- 当 `fallbackApplied=true` 时，必须显示降级提示（当前 `fallbackBannerText` 已覆盖）。
- 页面主流程（开始学习）不得依赖 LLM 文案字段，必须依赖 `taskPreviews`/`confirm` 返回。
- `whyStartHere`、`reasons` 为空时，前端应使用规则兜底文案，不得展示空白解释区。

## 后续扩展建议

1. 增加契约测试：为诊断与规划分别建立“LLM 返回异常 JSON/缺字段/超时”测试，断言 fallback 字段与业务结果稳定。
2. 增加解释一致性校验：对 `whyStartHere` 与 `keyWeaknesses/priorityNodes` 建立轻量规则校验器，避免解释与事实冲突。
3. 统一 `contentSource` 字典：诊断和规划字段保持同名同语义（建议收敛到 `CodeLabelDto`）。
4. 引入“解释证据对象”标准结构（`evidence[]`），让前端可统一渲染“结论-证据”链路。
5. 为 LLM 结果引入质量守门指标（空泛率、重复率、与规则字段冲突率），作为回归检查项。

---

执行口径：本文件是诊断页与规划页后续改动的强制约束。任何将业务决策迁移到 LLM、或删除 fallback/来源标记的改动，均视为违背架构边界。
