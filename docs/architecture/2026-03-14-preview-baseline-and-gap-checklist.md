# Preview 主链路基线与缺口清单（V1.5）

## 1. 后端主链路基线（已固化）

- 入口：`/api/learning-plans/preview` -> `LearningPlanController.preview(...)`
- 编排主链：
  - `LearningPlanService.preview(...)`
  - `PlanningContextAssembler.assemble(...)`
  - `LearningPlanOrchestrator.preview(...)`
  - `LearningPlanPreviewViewAssembler.assemble(...)`
  - `LearningPlanService.toResponse(...)`
- 存储快照：
  - `LearningPlan.summaryJson / reasonsJson / focusesJson / pathPreviewJson / taskPreviewJson / planningContextJson`

## 2. 前端渲染链路基线（已固化）

- 页面入口：`LearningPlanView.vue`
- Store：`learningPlanStore.generatePreview(...)`（请求 `preview` 接口）
- 标准化：`normalizeLearningPlanPreview(...)`
- 视图模型：`buildPreviewViewModel(...)`
- 页面首屏依赖：
  - 主任务（hero）
  - 证据块（aiObserved）
  - 策略说明（strategy）
  - 开始引导（kickoff）

## 3. 当前已稳定字段（后端 -> 前端）

- 已稳定核心字段：
  - `planId/status/previewOnly/committed/goal`
  - `recommendedEntry`
  - `learnerSnapshot`
  - `recommendedStrategy`
  - `alternatives`
  - `nextActions`
  - `adjustments`
  - `startGuide/explanationGenerated/generatedAt/traceId`

## 4. 缺口清单（V1.5 待补）

- 缺口 A：解释字段缺少稳定首屏语义
  - 需要新增：`whyThisStep`、`keyEvidence`、`skipRisk`、`expectedGain`、`confidenceHint`
  - 原因：当前解释信息散落在 `recommendedEntry.reason`、`learnerSnapshot.evidence`、`reasons`，首屏结构不稳定。

- 缺口 B：证据聚合逻辑分散，机器决策与前端解释未统一
  - 需要引入：统一 evidence 聚合器（同时产出机器视图和解释视图）。

- 缺口 C：用户画像分化不足
  - 候选动作模板仍偏通用，需要按能力缺口形成最少 4 类可分化模板。

- 缺口 D：信号层仍以 `LearnerStateSnapshot` 为单层表达
  - 需要并行引入“原子信号层”，与旧状态并行输出，避免替换式改造风险。

## 5. 兼容性约束

- 不改现有接口路径，不改主请求流程。
- 旧字段继续可用，新字段以增强形式增加。
- 解释增强失败时保留模板兜底。
