# 规划页全景清单

> 本文档整理规划页（Learning Plan）的入口、接口契约、LLM 调用点、职责边界、事实源、规划结果结构、UI 展示及问题列表，便于后续改造与个性化增强。

---

## 一、规划页当前前端入口

### View

| 文件 | 说明 |
|------|------|
| `frontend/src/views/LearningPlanView.vue` | 规划页主视图，负责加载、展示、确认学习计划 |

### 组件

规划页主视图为单页结构，**未拆分独立 plan 子组件**。与 plan 相关的常量、类型及 mock 分布如下：

| 类型 | 文件 | 说明 |
|------|------|------|
| 常量 | `frontend/src/constants/learningPlan.ts` | STAGE_LABELS、INTENSITY_LABELS、PATH_STATUS_LABELS、DEFAULT_PLAN_ADJUSTMENTS 等 |
| 类型 | `frontend/src/types/learningPlan.ts` | LearningPlanPreview、PlanSummary、PlanReason、PlanPathNode、PlanTaskPreview、PlanConfirmResult 等 |
| 工具 | `frontend/src/utils/format.ts` | 含 STAGE_LABELS、LearningStage 的格式化逻辑 |

以下组件在代码中存在引用，**当前 LearningPlanView 未使用**（可能为旧版或预留）：

| 组件 | 路径 | 说明 |
|------|------|------|
| PlanPathPreviewPanel | `frontend/src/components/plan/PlanPathPreviewPanel.vue` | 路径预览 |
| PlanTaskPreviewPanel | `frontend/src/components/plan/PlanTaskPreviewPanel.vue` | 任务预览 |
| PlanReasonPanel | `frontend/src/components/plan/PlanReasonPanel.vue` | 规划依据 |
| PlanSummaryPanel | `frontend/src/components/plan/PlanSummaryPanel.vue` | 规划摘要 |
| PlanAdjustPanel | `frontend/src/components/plan/PlanAdjustPanel.vue` | 调整面板 |

### Store

| 文件 | 说明 |
|------|------|
| `frontend/src/stores/learningPlan.ts` | Pinia store，管理 preview、request、adjustments、loading、confirming、error，提供 `generatePreview`、`regeneratePreview`、`confirmPlan`、`reset` |

### API

| 文件 | 说明 |
|------|------|
| `frontend/src/api/modules/learningPlan.ts` | `fetchLearningPlanPreviewApi`、`regenerateLearningPlanApi`、`confirmLearningPlanApi` |
| `frontend/src/api/normalizers.ts` | `normalizeLearningPlanPreview` |
| `frontend/src/api/client.ts` | axios 实例，baseURL 来自 `VITE_API_BASE_URL`，默认 `http://localhost:8080` |

### 路由

| 路径 | 名称 | 组件 |
|------|------|------|
| `/plan` | plan | `LearningPlanView` |

**进入方式**：诊断页 `DiagnosisView` 根据 `diagnosisStore.nextAction?.target` 跳转 `/plan`，携带 query：`sessionId`、`diagnosisId`、`goal`、`course`、`chapter`。

---

## 二、规划页接口契约

### 1. 生成学习规划接口

| 项目 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/learning-plans/preview` |
| 控制器 | `LearningPlanController.preview()` |
| Request | `PreviewLearningPlanRequest` |
| Response | `ApiEnvelope<LearningPlanPreviewResponse>` |

**Request 字段**：`diagnosisId`（必填）、`sessionId`、`goalText`（必填）、`courseName`、`chapterName`、`adjustments`（intensity、learningMode、prioritizeFoundation）

### 2. 获取规划详情接口

| 项目 | 值 |
|------|-----|
| 方法 | `GET` |
| 路径 | `/api/learning-plans/{previewId}` |
| 控制器 | `LearningPlanController.get()` |
| Response | `ApiEnvelope<LearningPlanPreviewResponse>` |

**说明**：根据 previewId 和当前用户从持久化中加载已保存的 preview。

### 3. 确认开始学习接口

| 项目 | 值 |
|------|-----|
| 方法 | `POST` |
| 路径 | `/api/learning-plans/{previewId}/confirm` |
| 控制器 | `LearningPlanController.confirm()` |
| Response | `ApiEnvelope<ConfirmLearningPlanResponse>` |

**Response 字段**：`planId`、`sessionId`、`currentNodeId`、`firstTaskId`、`nextPage`（如 `/sessions/{sessionId}`）

### 4. next action / overview / report 是否影响规划页

| 接口/概念 | 是否影响规划页 | 说明 |
|-----------|----------------|------|
| **next action** | 间接 | 诊断生成的 `nextAction.target` 用于跳转到 `/plan`，不参与规划接口入参 |
| **session overview** | 否 | `SessionOverviewResponse` 用于会话详情页 `SessionView`，规划页不调用 |
| **report** | 否 | `LearningReportResponse` 用于 `ReportView`，规划页不调用 |

**结论**：规划页只依赖 `diagnosisId`、`sessionId`、`goalText`、`courseName`、`chapterName` 和 `adjustments`，next action / overview / report 不影响规划接口本身。

---

## 三、规划阶段 LLM 调用点

### 1. personalized path plan（个性化路径规划）

| 项目 | 说明 |
|------|------|
| 位置 | `PersonalizedPathPlannerService` |
| 调用链 | `PlanSessionTasksService` → `PersonalizedPathPlannerService.plan()` |
| 场景 | **会话内任务规划**（Session 已有，需规划任务顺序与插入任务），**非**规划页 preview 流程 |
| Stage | `LlmStage.PATH_PLAN` |
| 用途 | 对已有章节节点做个性化排序、插入补救任务 |

**结论**：`PersonalizedPathPlannerService` 不参与规划页的 preview / confirm 流程。

### 2. concept decompose（知识点拆解）

| 项目 | 说明 |
|------|------|
| 位置 | `ConceptNodeDecomposeService.decompose()` |
| 调用链 | **当前无调用方** |
| 用途 | 将概念拆解为可学习子节点（`concept_nodes`） |
| 结论 | 规划页及 CreateSession 均未使用；CreateSession 对空章节使用 `bootstrapConceptNodes` 硬编码节点 |

### 3. learning plan orchestrator（学习规划编排）

| 项目 | 说明 |
|------|------|
| 位置 | `LearningPlanOrchestrator.preview()` |
| 调用链 | `LearningPlanService.preview()` → `LearningPlanOrchestrator.preview()` |
| Stage | `LlmStage.LEARNING_PLAN` |
| 输入 | `LearningPlanPlanningContext` + `LearningPlanPreview`（rule 版） |
| 输出 | `OrchestratedPlan`（合并 rule 与 LLM 结果） |
| 作用 | 调用 LLM 生成 headline、reasons、focuses、task_preview；**path 固定由 rule 决定** |

### 4. fallback 逻辑

| 触发条件 | 行为 |
|----------|------|
| LLM 未就绪（`!llmProperties.isEnabled()` 或 `!llmProperties.isReady()`） | 使用 `RuleBasedPlanBuilder` 的 rule preview |
| 输出截断（`isTruncated(llmResult)`） | 回退 rule |
| JSON 解析失败（`LlmJsonParseException`） | 回退 rule |
| Schema 校验失败（`LearningPlanSchemaValidationException`） | 回退 rule |
| LLM 调用异常 | 由 `LlmFailureClassifier` 分类，回退 rule |
| 回退时 | 返回 `PlanSource.RULE_FALLBACK`，`fallbackApplied=true`，`fallbackReasons` 记录原因 |

### 5. parser

| 组件 | 说明 |
|------|------|
| `LlmJsonParser` | 解析 LLM 返回文本为 JsonNode，支持 code fence 剥离、修复等 |
| `LearningPlanResultValidator` | 解析 JsonNode → `LearningPlanLlmResult`，校验 schema，normalize |

### 6. prompt

| 组件 | 说明 |
|------|------|
| `LearningPlanPromptBuilder.build()` | 构建 `LlmPrompt`，system + user 模板，约束 JSON 输出格式 |
| 模板 Key | `LEARNING_PLAN_V1` |
| 传入 LLM 的上下文 | `goal_id`、`diagnosis_id`、`goal_text`、`learner_profile`、`weak_points`、`recent_error_tags`、`recent_scores`、`candidate_path`、`candidate_reasons` |

### 7. observability

| 组件 | 说明 |
|------|------|
| `LlmCallLogger` | 记录 LLM 调用 start、success、fallback、failure |
| `LlmFailureClassifier` | 对异常分类，返回 `LlmFallbackReason` |
| Micrometer | 通过 `MeterRegistry` 上报 `llm.call.*` 指标 |

---

## 四、规划阶段「LLM 参与的职责边界」

### LLM 负责

| 职责 | 是否由 LLM 生成 | 说明 |
|------|-----------------|------|
| 生成知识点拆解 | ❌ 否 | `ConceptNodeDecomposeService` 存在但未被规划流程调用 |
| 生成任务顺序（path） | ❌ 否 | path 由 `RuleBasedPlanBuilder` 规则固定，LLM 不参与 |
| 生成解释文案 | ✅ 是 | headline、reasons（type/title/description）、focuses |
| 生成节奏建议 | ❌ 否 | recommendedPace 来自 rule 的 `context.adjustments().intensity()` |

### 规则负责

| 职责 | 说明 |
|------|------|
| 节点选择与顺序 | `RuleBasedPlanBuilder.pickStartIndex()`、`resolveNodeWindow()` |
| 起点选择 | 基于 mastery、attemptCount、weakReasons、preferPrerequisite |
| recommendedPace | 直接取自 `PlanAdjustments.intensity` |
| pathPreview | 完全由 rule 生成 |
| 基础 headline / reasons / focuses / task_preview | rule 提供候选，LLM 可覆盖 |

### 总结

- **LLM**：只负责 headline、reasons、focuses、task_preview 的个性化改写，**不改变 path 与节奏**。
- **规则**：负责 path、起点、节奏、以及 fallback 时的完整计划。

---

## 五、规划页当前可用于「个性化解释」的事实源

| 事实源 | 是否进 planning context | 说明 |
|--------|-------------------------|------|
| **目标** | ✅ 是 | `goalText`、`goalId` |
| **诊断结果** | ✅ 是 | `diagnosisId`（仅 ID，诊断摘要通过 `learnerProfileSummary` 间接体现） |
| **能力画像** | ⚠️ 部分 | `learnerProfileSummary` 由 weakPointLabels 聚合，非完整能力画像 JSON |
| **历史表现** | ✅ 是 | `recentScores`、`recentErrorTags` |
| **当前掌握度** | ✅ 是 | 每个 `LearningPlanContextNode` 含 mastery、attemptCount |
| **前置知识** | ✅ 是 | `prerequisiteNodeIds` |
| **风险标签** | ⚠️ 部分 | 通过 `weakReasons`、`recentErrorTags` 体现，无独立 risk_tags 字段 |
| **错题/弱点** | ✅ 是 | `weakPointLabels`、`weakReasons` per node |

**PlanningContext 字段**：`userId`、`goalId`、`diagnosisId`、`courseId`、`chapterId`、`goalText`、`sourceSessionId`、`nodes`、`recentErrorTags`、`recentScores`、`weakPointLabels`、`learnerProfileSummary`、`adjustments`。

**未进 context**：诊断详情（洞察、题目级结果）、完整能力画像、report 中的 nextStepReason、错题原文等。

---

## 六、规划结果结构

### 已有字段

| 字段 | 位置 | 说明 |
|------|------|------|
| `headline` | summary | 个性化标题 |
| `recommendedStartNode` | summary | 推荐起点节点 |
| `recommendedPace` | summary | 节奏（来自 adjustments） |
| `reasons` | 顶层 | 规划依据列表（type、title、description） |
| `focuses` | 顶层 | 关注点列表 |
| `pathNodes` / `pathPreview` | 顶层 | 路径节点 |
| `taskPreviews` | 顶层 | 任务预览（4 阶段） |
| `nextStepNote` | 顶层 | 下一步说明（前端 normalizer 支持） |
| `diagnosisSummary` | context | 诊断摘要 |

### 缺失或未明确暴露的字段

| 字段 | 状态 |
|------|------|
| `whyStartHere` | ❌ 无独立字段，部分信息在 `reasons` 中 |
| `recommendedPace` | ✅ 有（来自 rule） |
| `keyWeaknesses` | ❌ 无，弱项在 context 的 `weakPointLabels`，未单独返回 |
| `priorityNodes` | ❌ 无，path 顺序即隐含优先级 |
| `recommendedStages` | ⚠️ 固定 4 阶段 STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION |
| `nextStepReason` | ⚠️ 有 `nextStepNote`，语义接近但不完全等同 |

---

## 七、规划页当前 UI 上展示了什么

| 展示项 | 是否显示 | 数据来源 |
|--------|----------|----------|
| **学什么** | ✅ 是 | `preview.context.goalText`、`courseName`、`chapterName`；首任务 `learningGoal`、`learnerAction`、`aiSupport` |
| **学习顺序** | ⚠️ 简化 | `stagePath`（Foundation → Algorithm → Implementation → Advanced）按当前任务 stage 高亮，未展示完整 pathNodes 列表 |
| **预计阶段** | ✅ 是 | `stagePath` 四阶段卡片 |
| **为什么从这里开始** | ⚠️ 折叠 | 在「为什么这样安排」 details 中，结合 `diagnosisSummary`、`reasons` |
| **为什么这样安排** | ✅ 是 | details 内展示 `diagnosisSummary` 与 `reasons` |
| **预计时间** | ✅ 是 | `nextTask.estimatedTaskMinutes`，默认「约 15 分钟」 |
| **fallback 提示** | ✅ 是 | `fallbackBannerText` 当 `fallbackApplied` 时显示 |

**未展示**：完整 pathNodes 列表、keyWeaknesses、priorityNodes、whyStartHere 独立区块、调节面板（PlanAdjustPanel 未接入）。

---

## 八、规划页最大问题列表

### 已有能力

- 支持基于诊断生成预览并确认创建 session
- 支持 rule + LLM 双源，fallback 可 degrade
- 有 headline、reasons、focuses、task_preview 的个性化改写
- 掌握度、弱点、错误标签已进 planning context
- 有 nextStepNote、reasons 支撑「为什么这样安排」

### 缺失能力

- 无独立的「为什么从这里开始」区块与 `whyStartHere` 字段
- 无 `keyWeaknesses`、`priorityNodes` 的显式展示
- path 完全由 rule 决定，LLM 不参与节点顺序
- 诊断详细结果、能力画像、report 等未进 planning context
- PlanPathPreviewPanel、PlanAdjustPanel 等组件未接入主视图

### 为什么不足以体现「个性化」

1. path 固定规则，无法根据诊断或目标做顺序调整
2. 事实源不完整，缺少诊断洞察、错题详情等
3. `whyStartHere`、`keyWeaknesses` 等未结构化输出与展示
4. 节奏建议来自用户选择，非模型个性化
5. reasons 虽有 LLM 改写，但约束强、与事实绑定弱

### 最小改造方案

| 优先级 | 改造项 | 说明 |
|--------|--------|------|
| P0 | 新增 `whyStartHere` | 在 summary 或顶层增加字段，由 LLM 生成，UI 单独展示 |
| P0 | 展示 keyWeaknesses | 从 context 或新字段取弱项列表，在「为什么这样安排」前增加弱项说明 |
| P1 | 接入 PlanPathPreviewPanel | 展示完整 pathNodes，增强「学习顺序」可见性 |
| P1 | 扩展 planning context | 纳入诊断摘要/洞察、能力画像摘要（如有） |
| P2 | 可选 path 个性化 | 在 rule path 基础上，允许 LLM 建议微调（需评估稳定性） |
| P2 | 接入 PlanAdjustPanel | 支持用户调节强度、模式后再重新生成 |

---

## 九、相关文件索引

| 层级 | 路径 |
|------|------|
| 前端 View | `frontend/src/views/LearningPlanView.vue` |
| 前端 Store | `frontend/src/stores/learningPlan.ts` |
| 前端 API | `frontend/src/api/modules/learningPlan.ts` |
| 前端路由 | `frontend/src/router/index.ts` |
| 后端 Controller | `backend/.../LearningPlanController.java` |
| 后端 Service | `backend/.../LearningPlanService.java` |
| 编排器 | `backend/.../LearningPlanOrchestrator.java` |
| 规则构建 | `backend/.../RuleBasedPlanBuilder.java` |
| 上下文组装 | `backend/.../PlanningContextAssembler.java` |
| Prompt | `backend/.../LearningPlanPromptBuilder.java` |
| 会话内路径规划 | `backend/.../PersonalizedPathPlannerService.java` |
| 知识点拆解 | `backend/.../ConceptNodeDecomposeService.java` |
