# 诊断画像中台 + 画像驱动规划：验收与答辩技术说明

## 1. 文档目标与范围

- 目标：把当前系统整理为一条可展示、可验证的证据链，覆盖从诊断提交到学习计划预览的关键环节。
- 范围：仅基于现有后端代码与接口行为说明，不新增功能、不改架构。
- 读者：验收方、答辩评审、联调同学。

---

## 2. 全链路总览（端到端证据链）

### 2.1 链路步骤

1. **diagnosis session create**  
   - 接口：`POST /api/diagnosis/sessions`  
   - 入口：`DiagnosisController#createSession`  
   - 服务：`DiagnosisService#createDiagnosisSession`  
   - 关键输出：诊断问题集（带 `optionSignalMapping`）、`diagnosisId`、`nextAction`。

2. **diagnosis submissions**  
   - 接口：`POST /api/diagnosis/sessions/{diagnosisId}/submissions`  
   - 入口：`DiagnosisController#submitSession`  
   - 服务：`DiagnosisService#submitDiagnosisSession`  
   - 关键动作：答案校验、归一化、能力画像草稿生成、reasoning/evidence 输出。

3. **learner_feature_signal 落库**  
   - 提取：`LearnerFeatureExtractor#extract`（按题目选项映射到结构化 signals）  
   - 聚合前实体：`LearnerFeatureSignal`（`featureKey/featureValue/scoreDelta/confidence/evidence/source`）  
   - 持久化：`JdbcLearnerFeatureSignalRepository#saveAll` → 表 `learner_feature_signal`。

4. **learner_profile_snapshot 生成与落库**  
   - 聚合：`LearnerFeatureAggregator#aggregate`  
     - 输出 `featureSummary`、`strategyHints`、`constraints`、`explanations`  
   - 构建：`LearnerProfileSnapshotBuilder#build`  
   - 持久化：`JdbcLearnerProfileSnapshotRepository#saveOrUpdate` → 表 `learner_profile_snapshot`（按 `diagnosis_session_id` upsert）。

5. **learning plan preview**  
   - 接口：`POST /api/learning-plans/preview`  
   - 入口：`LearningPlanController#preview`  
   - 上下文拼装：`PlanningContextAssembler#assemble`（按 `diagnosisId` 读取 snapshot）  
   - 决策主链：`LearningPlanOrchestrator#preview`（候选集 → 默认决策/LLM 决策 → 校验回退）  
   - 预览视图：`LearningPlanPreviewViewAssembler#assemble` 生成结构化 summary/reasons/tasks。

6. **输出关键展示字段**  
   - DTO：`LearningPlanPreviewResponse`  
   - 核心字段：
     - `recommendedStrategy`（`code/label/explanation`）
     - `profileDrivenReasoning`
     - `riskFlags`
     - `profileConflicts`
   - 关键约束：`recommendedStrategy.code` 绑定最终结构化决策（非文案推断）。

---

## 3. 字段级证据映射（从答案到预览）

### 3.1 诊断问题到 signals（规则映射）

- 映射来源：`DiagnosisTemplateFactory#optionSignalMapping`。
- 例如：
  - `FOUNDATION=BEGINNER` → `foundation_level=BEGINNER`、`review_depth=HIGH`
  - `EXPERIENCE=NO_EXPERIENCE` → `practice_experience=NONE`
  - `LEARNING_PREFERENCE=PRACTICE_FIRST` → `learning_preference=PRACTICE_FIRST`
  - `TIME_BUDGET=LIGHT` → `time_budget=LIGHT`、`learning_intensity=LIGHT`

### 3.2 signals 到 snapshot（结构化聚合）

- 聚合入口：`LearnerFeatureAggregator#aggregate`  
- 关键聚合结果：
  - `featureSummary.features[]`：保留 feature 维度、置信度、证据计数
  - `strategyHints`：如 `learningPreference`、`goalOrientation`
  - `constraints`：如 `timeBudget`、`learningIntensity`

### 3.3 snapshot 到 preview 决策与解释

- 上下文注入：`PlanningContextAssembler#assemble` 通过 `diagnosisId` 读取 `LearnerProfileSnapshot`。
- 策略决策：`PlanCandidatePlanner` + `DefaultDecisionFactory` + `LearningPlanDecisionValidator`。
- 预览组装：`LearningPlanPreviewViewAssembler` 将最终策略写入 `LearningPlanSummary.selectedStrategyCode`。
- 响应出参：`LearningPlanService#toResponse`
  - `recommendedStrategy.code`：优先取 `summary.selectedStrategyCode`
  - `profileDrivenReasoning`：从 snapshot 的 `strategyHints/constraints` 注入
  - `riskFlags/profileConflicts`：由结构化特征和信号聚合逻辑判定。

---

## 4. 三个典型演示场景

> 说明：以下为“验收演示样例”，字段命名和决策逻辑与当前代码一致；具体文本会因 LLM 文案增强与上下文细节有轻微差异。

### 场景 A：基础薄弱 + 例子驱动 + 时间短

#### 输入答案摘要

- `FOUNDATION = BEGINNER`
- `EXPERIENCE = COURSEWORK`
- `GOAL_STYLE = COURSE`
- `TIME_BUDGET = LIGHT`
- `LEARNING_PREFERENCE = EXAMPLE_FIRST`

#### 关键 feature signals（示例）

- `foundation_level=BEGINNER`（self_assessment）
- `review_depth=HIGH`
- `practice_experience=COURSEWORK`
- `time_budget=LIGHT`
- `learning_intensity=LIGHT`
- `learning_preference=EXAMPLE_FIRST`

#### profile snapshot 关键字段（示例）

- `featureSummary.features`: 包含上述 feature 及 `confidence/evidenceCount`
- `strategyHints.learningPreference = EXAMPLE_FIRST`
- `constraints.timeBudget = LIGHT`
- `constraints.learningIntensity = LIGHT`

#### preview 决策结果（示例）

- `recommendedStrategy.code = FOUNDATION_FIRST`
- `recommendedStrategy.label = 先补基础`
- `adjustments.intensity = LIGHT`

#### explanation / risk 输出（示例）

- `profileDrivenReasoning`：
  - 画像显示学习偏好为 `EXAMPLE_FIRST`
  - 画像约束时间预算为 `LIGHT`
- `keyEvidence`：同时含历史证据与画像证据
- `riskFlags = []`
- `profileConflicts = []`

#### 为什么结果合理

- 基础薄弱信号优先级高，策略应先稳前置理解链路。
- 时间短约束推动节奏轻量化，但不应直接走高风险快推。

---

### 场景 B：基础中等 + 偏练习 + 目标偏应用

#### 输入答案摘要

- `FOUNDATION = BASIC`
- `EXPERIENCE = PROJECTS`
- `GOAL_STYLE = PROJECT`
- `TIME_BUDGET = STANDARD`
- `LEARNING_PREFERENCE = PRACTICE_FIRST`

#### 关键 feature signals（示例）

- `foundation_level=BASIC`
- `practice_experience=PROJECTS`
- `transfer_experience=HIGH`
- `goal_orientation=PROJECT`
- `learning_preference=PRACTICE_FIRST`
- `time_budget=STANDARD`

#### profile snapshot 关键字段（示例）

- `strategyHints.learningPreference = PRACTICE_FIRST`
- `strategyHints.goalOrientation = PROJECT`
- `constraints.timeBudget = STANDARD`
- `constraints.learningIntensity = STANDARD`

#### preview 决策结果（示例）

- `recommendedStrategy.code = PRACTICE_FIRST`
- `recommendedStrategy.label = 先练后学`
- `adjustments.learningMode = PRACTICE_DRIVEN`

#### explanation / risk 输出（示例）

- `profileDrivenReasoning`：明确指出偏练习偏好和预算匹配
- `keyEvidence`：包含迁移经验、偏好信号、近期训练证据
- `riskFlags = []`
- `profileConflicts = []`

#### 为什么结果合理

- 画像偏好与目标导向均指向应用与训练闭环，`PRACTICE_FIRST` 与当前画像一致。
- 基础非极弱，不需要一刀切基础优先。

---

### 场景 C：高自评 + 弱经验（OVERCONFIDENCE_RISK）

#### 输入答案摘要

- `FOUNDATION = ADVANCED`
- `EXPERIENCE = NO_EXPERIENCE`
- `GOAL_STYLE = EXAM`
- `TIME_BUDGET = LIGHT`
- `LEARNING_PREFERENCE = CONCEPT_FIRST`

#### 关键 feature signals（示例）

- `foundation_level=ADVANCED`
- `review_depth=LOW`
- `practice_experience=NONE`
- `assessment_pressure=HIGH`
- `time_budget=LIGHT`
- `learning_preference=CONCEPT_FIRST`

#### profile snapshot 关键字段（示例）

- `featureSummary.features` 中同时存在：
  - 高自评信号：`foundation_level=ADVANCED`
  - 弱经验信号：`practice_experience=NONE`
- `strategyHints.learningPreference = CONCEPT_FIRST`
- `constraints.timeBudget = LIGHT`

#### preview 决策结果（示例）

- `recommendedStrategy.code` 通常为稳健策略（如 `FOUNDATION_FIRST`）
- `adjustments.intensity` 受预算约束趋向轻量或标准

#### explanation / risk 输出（示例）

- `profileConflicts` 包含：`OVERCONFIDENCE_PROFILE_CONFLICT`
- `riskFlags` 包含：`OVERCONFIDENCE_RISK`
- `profileDrivenReasoning` 明确显示画像证据来源（偏好/预算）

#### 为什么结果合理

- 当前代码对“高自评 + 弱经验”执行结构化冲突判定，不依赖文案猜测。
- 风险可观测后，前端和运营可明确解释“为什么不盲目快推”。

---

## 5. 验收检查清单（可直接执行）

### 5.1 基本链路验收

1. 调 `POST /api/diagnosis/sessions` 获取 `diagnosisId` 与问题集。  
2. 调 `POST /api/diagnosis/sessions/{diagnosisId}/submissions` 提交答案。  
3. 验收 `SubmitDiagnosisSessionResponse.insights`：
   - `featureSummary` 非空
   - `strategyHints`/`constraints` 有值（与输入一致）
4. 调 `POST /api/learning-plans/preview`（携带 `diagnosisId`）。  
5. 验收 `LearningPlanPreviewResponse`：
   - `recommendedStrategy.code` 存在且与最终结构化决策一致
   - `profileDrivenReasoning` 至少 1 条来自 snapshot
   - `riskFlags/profileConflicts` 按场景返回（冲突场景应命中）

### 5.2 关键一致性验收

- `recommendedStrategy.code` 不依赖模板推断；应可追溯到决策结果（`selectedStrategyCode`）。
- `profileDrivenReasoning` 不应只来自历史错题；需可追溯到 snapshot 的结构化字段。
- `OVERCONFIDENCE_RISK` 需由结构化条件触发（非自由文本）。

---

## 6. 答辩可讲的架构总结

### 6.1 哪些部分由规则实现

- 诊断题到特征信号映射：`DiagnosisTemplateFactory`、`LearnerFeatureExtractor`
- 特征聚合与画像快照：`LearnerFeatureAggregator`、`LearnerProfileSnapshotBuilder`
- 规划候选集与默认决策：`PlanCandidatePlanner`、`DefaultDecisionFactory`
- 冲突与风险结构化判定：`LearningPlanService` 内冲突计算逻辑
- 兜底与回退：`LearningPlanDecisionValidator`、各服务 fallback 分支

### 6.2 哪些部分由 LLM 做增强

- 诊断题文案增强：`DiagnosisQuestionCopyLlmService`
- 能力总结文案：`CapabilityProfileSummaryLlmService`
- 规划决策增强：`LearningPlanDecisionLlmService`（经校验）
- 预览解释增强：`PreviewTemplateExplanationAssembler`
- 个性化叙事增强：`LlmEnhancedPersonalizedNarrativeGenerator`

### 6.3 为什么比“LLM 端到端直出”更稳

- **可验证**：关键决策字段结构化落地，能做一致性验收。
- **可回退**：LLM 不可用或输出不合规时自动回退规则链路。
- **可观测**：冲突风险（如 `OVERCONFIDENCE_RISK`）可结构化暴露。
- **可解释**：`profileDrivenReasoning` 把“画像证据”直接暴露给展示层与评审层。

---

## 7. 演示建议（5-8 分钟）

1. 用场景 C 先演示“风险可观测”（最有说服力）。  
2. 回到场景 A/B 展示“同链路下策略差异与解释差异”。  
3. 最后强调：系统不是“让 LLM 决定一切”，而是“结构化中台 + LLM 增强层”。

