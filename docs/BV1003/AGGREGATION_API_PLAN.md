# Aggregation API Plan

## 0. 结论摘要

- 当前后端已经具备 `session overview`、`session quiz feedback`、`learning report`、`weak points`、`growth dashboard`、`current session`、`session history` 等基础查询能力。
- 真正存在明显前端拼装的是报告页：前端把 3 个接口合并成 1 个 `LearningReport` 视图模型。
- `users/me/knowledge-graph` 和 `users/me/growth-stats` 目前没有现成 API，首页仍在用 mock 数据；但后端已有足够的数据表和查询服务可复用。
- 建议新增 5 个面向页面的聚合接口，统一挂到 `/api/sessions` 与 `/api/users/me`，由后端负责拼装页面所需数据，前端只做展示。
- 另一个顺手应处理的问题是接口命名不一致：仓库里同时存在 `/api/session/...` 和 `/api/sessions/...`。

## 1. 当前已有 API

### 1.1 Session / Session History

- `POST /api/session/create`
- `POST /api/session/{sessionId}/plan`
- `GET /api/session/{sessionId}/overview`
- `GET /api/session/{sessionId}/path`
- `GET /api/session/current`
- `GET /api/session/history`
- `GET /api/session/{sessionId}`
- `POST /api/session/{sessionId}/resume`

### 1.2 Session Quiz / Feedback

- `POST /api/sessions/{sessionId}/quiz/generate`
- `GET /api/sessions/{sessionId}/quiz/status`
- `GET /api/sessions/{sessionId}/quiz`
- `POST /api/sessions/{sessionId}/quiz/submit`
- `GET /api/sessions/{sessionId}/feedback`
- `POST /api/sessions/{sessionId}/next-action`

### 1.3 Learning Insight

- `GET /api/session/{sessionId}/learning-feedback/report`
- `GET /api/session/{sessionId}/learning-feedback/weak-points`
- `GET /api/session/{sessionId}/growth-dashboard`

### 1.4 Diagnosis / Capability

- `GET /api/capability-profile/{sessionId}`

### 1.5 与目标接口的对应关系

| 目标接口 | 当前最接近的已有接口 |
| --- | --- |
| `GET /api/sessions/{sessionId}/overview` | `GET /api/session/{sessionId}/overview` |
| `GET /api/sessions/{sessionId}/report` | `GET /api/sessions/{sessionId}/feedback` + `GET /api/session/{sessionId}/learning-feedback/report` + `GET /api/session/{sessionId}/learning-feedback/weak-points` |
| `GET /api/users/me/knowledge-graph` | 无直接接口，可复用 `growth-dashboard`、`overview`、`path` 的底层数据源 |
| `GET /api/users/me/growth-stats` | 无直接接口，可复用 `current session`、`session history`、`growth-dashboard`、`report` 的底层数据源 |
| `GET /api/sessions/{sessionId}/next-action` | `POST /api/sessions/{sessionId}/next-action` 的推荐逻辑 + `LearningReportResponse.next_step` |

## 2. 可以复用的数据源

### 2.1 `GET /api/sessions/{sessionId}/overview`

- `GetSessionOverviewService`
- `SessionOverviewResponse`
- `SessionRepository.findByIdAndUserPk`
- `TaskRepository.findBySessionIdWithStatus`
- `MasteryRepository.findByUserIdAndChapterId`

### 2.2 `GET /api/sessions/{sessionId}/report`

- `SessionQuizController.GET /api/sessions/{sessionId}/feedback`
  - 提供 `overall_summary`、`question_results`、`suggested/recommended/selected_action`、`next_round_advice`
- `LearningInsightQueryService.getLearningReport`
  - 提供 `node/stage/score/accuracy/mastery/weak_points/next_step/growth_recorded`
- `LearningFeedbackController.GET /weak-points`
  - 提供更完整的弱点节点列表
- 相关 repository
  - `PracticeQuizRepository`
  - `PracticeFeedbackReportRepository`
  - `PracticeRepository`
  - `PracticeSubmissionRepository`
  - `NodeMasteryRepository`
  - `ConceptNodeRepository`
  - `TaskRepository`

### 2.3 `GET /api/users/me/knowledge-graph`

- `ConceptNodeRepository.findByChapterIdOrderByOrderNoAsc`
- `NodeMasteryRepository.findByUserIdAndChapterId`
- `MasteryRepository.findByUserIdAndChapterId`
- `SessionRepository.findLatestActiveByUserPk`
- `LearningInsightQueryService.getGrowthDashboard`
- `GetSessionPathService`
- 可选增强源：`CapabilityProfileQueryService`

### 2.4 `GET /api/users/me/growth-stats`

- `GetCurrentSessionService`
- `SessionHistoryService.listHistory`
- `SessionRepository.findHistoryByUserPk`
- `TaskRepository.findRecentTrainingAttempts`
- `NodeMasteryRepository.findByUserIdAndChapterId`
- `LearningInsightQueryService.getGrowthDashboard`
- `LearningInsightQueryService.getLearningReport`
- `CapabilityProfileQueryService`

### 2.5 `GET /api/sessions/{sessionId}/next-action`

- `LearningInsightQueryService.buildRecommendation` 产出的 `next_step`
- `SessionQuizController.GET /feedback` 中的 `recommended_action` / `suggested_next_action` / `selected_action`
- `SessionQuizService.applyNextAction` 已验证动作枚举和落库路径，可复用动作定义

## 3. 需要新增的字段

### 3.1 `GET /api/sessions/{sessionId}/overview`

当前 `SessionOverviewResponse` 偏“流程概览”，不足以直接驱动 `SessionView`，建议新增：

- `current_stage_label`
- `current_task_title`
- `current_task_goal`
- `next_step_text`
- `latest_report_summary`
- `latest_report_status`
- `latest_report_available`
- `recommended_entry_route`
- `recommended_entry_label`

说明：

- 现在这些文案大多在前端 `SessionView.vue` 里根据 `currentStage/nextTask/report` 自行推导。
- 若希望前端“不再拼数据”，这些页面级字段应由后端直接返回。

### 3.2 `GET /api/sessions/{sessionId}/report`

现有三接口数据基本够用，但缺一个统一 contract。建议聚合后补齐：

- `report_ready`
- `quiz_status`
- `action_applied`
- `primary_action`
- `primary_action_label`
- `primary_action_route`
- `secondary_actions`
- `page_title`
- `page_summary`

说明：

- `feedback` 接口偏“练习报告”。
- `learning-feedback/report` 偏“学习洞察”。
- 前端当前还要自己决定按钮标题、跳转去向和主动作。

### 3.3 `GET /api/users/me/knowledge-graph`

当前后端没有直接面向“知识图谱页面”的 DTO，建议新增：

- `chapters`
- `nodes`
  - `node_id`
  - `node_name`
  - `chapter_id`
  - `mastery_score`
  - `mastery_status`
  - `training_accuracy`
  - `latest_evaluation_score`
  - `attempt_count`
  - `is_current`
  - `is_recommended`
  - `is_weak_point`
- `edges`
  - 若数据库没有显式依赖边，可先按 `order_no` 生成顺序边
- `legend`
- `last_updated_at`

说明：

- `growth-dashboard` 里只有扁平 `mastery_nodes`，不足以表达图谱结构。
- 图谱页面通常需要章节分组、节点状态、推荐节点、薄弱节点、边关系。

### 3.4 `GET /api/users/me/growth-stats`

首页 mock 的内容目前后端没有一个统一接口承接，建议新增：

- `metrics`
  - `total_sessions`
  - `active_sessions`
  - `learned_node_count`
  - `mastered_node_count`
  - `average_accuracy`
- `recent_sessions`
- `recent_evaluation`
- `new_knowledge`
- `current_session_brief`
- `trend_summary`

说明：

- 这些字段在首页 `homeLearningSummary` 里已经有明确前端消费形态。
- 现有接口只能零散提供一部分，且跨 session、跨 chapter 的聚合仍缺失。

### 3.5 `GET /api/sessions/{sessionId}/next-action`

建议返回一个只读决策对象，而不是沿用提交动作接口：

- `recommended_action`
- `suggested_action`
- `selected_action`
- `reason`
- `target_node_id`
- `target_node_name`
- `target_task_type`
- `confidence`
- `route`
- `label`
- `available_actions`

说明：

- 前端当前从 `report.nextStep`、`suggestedNextAction`、`recommendedAction` 三处取值，再自己决定按钮文案和路由。

## 4. 前端当前是如何拼数据的

### 4.1 会话页 `SessionView`

- 先调 `sessionStore.fetchOverview(sessionId)` -> `/api/session/{sessionId}/overview`
- 再尝试调 `feedbackStore.fetchReport(sessionId)` -> 报告聚合链
- 页面自己基于 `overview.currentStage`、`overview.nextTask.stage` 生成：
  - 当前任务标题
  - 当前任务目标文案
  - 下一步提示文案
  - 继续学习跳转逻辑
- 页面还把 `report.overallSummary || report.diagnosisSummary` 当作“最近训练摘要”

结论：

- 会话页现在是在前端把 “overview + latest report summary + routing rules + stage copy” 拼成一个页面模型。

### 4.2 报告页 `ReportView`

`frontend/src/api/modules/feedback.ts` 中 `fetchReportApi(sessionId)` 并行调用：

- `GET /api/sessions/{sessionId}/feedback`
- `GET /api/session/{sessionId}/learning-feedback/report`
- `GET /api/session/{sessionId}/learning-feedback/weak-points`

然后在 `frontend/src/api/normalizers.ts` 中通过 `mergeReportPayloads()` 手工合并：

- `feedback.question_results` 与 `report.question_results` 二选一
- `report.weak_points` 与 `weakPoints.weak_nodes` 二选一
- `report.next_step` 与 `feedback.recommended_action/suggested_next_action` 混合使用
- `overallSummary`、`diagnosisSummary`、`nextRoundAdvice`、`selectedAction` 等字段做兜底整合

结论：

- 这是当前最典型、最明确的“前端拼数据”场景。
- `GET /api/sessions/{sessionId}/report` 应优先落地。

### 4.3 成长页 `GrowthDashboardView`

- 当前只调 `GET /api/session/{sessionId}/growth-dashboard`
- 这里没有多接口拼装，但接口仍是“某个 session 的 growth dashboard”，不是“用户的长期成长统计”

结论：

- 它更接近单页数据源，不是用户级聚合。
- 可作为 `users/me/knowledge-graph` 与 `users/me/growth-stats` 的底层输入之一。

### 4.4 首页 `HomeView`

- 只调用 `GET /api/session/current`
- 成长摘要、最近学习记录、最近评估、新增知识点全部来自 `frontend/src/mocks/home.ts`

结论：

- 首页还没有接真实聚合 API。
- `GET /api/users/me/growth-stats` 正好可以替换这块 mock 数据。

## 5. 如何合并为聚合接口

### 5.1 统一原则

- 以“页面消费模型”设计返回，而不是简单转发已有 DTO。
- 聚合接口内部复用现有 service/repository，不复制业务规则。
- 对外统一使用 `/api/sessions/...` 与 `/api/users/me/...`。
- 保留旧接口一段时间作为兼容层，新页面先切聚合接口。

### 5.2 `GET /api/sessions/{sessionId}/overview`

建议由新 `SessionAggregationQueryService` 组合：

- `GetSessionOverviewService.execute(sessionId)`
- 最新报告摘要
  - 优先取 `LearningInsightQueryService.getLearningReport(sessionId, userId)`
- 下一步动作摘要
  - 取 recommendation / selected action
- 页面文案
  - 根据 stage 在后端生成 `title/goal/helper/route`

建议返回结构：

- `session`
- `progress`
- `timeline`
- `next_task`
- `latest_report`
- `next_action`
- `ui_copy`

价值：

- `SessionView` 首屏只调一个接口即可。

### 5.3 `GET /api/sessions/{sessionId}/report`

建议直接把当前前端的 `mergeReportPayloads()` 迁到后端：

- 基础反馈：`SessionQuizService.getFeedback`
- 学习洞察：`LearningInsightQueryService.getLearningReport`
- 薄弱点兜底：`WeakPointDiagnosisService.diagnoseWeakPoints`

合并规则建议：

- `question_results`
  - 以 `LearningReportResponse.question_results` 为主
  - 缺失时回退 `SessionFeedbackResponse.question_results`
- `weak_points`
  - 以 `LearningReportResponse.weak_points` 为主
  - 缺失时回退 `WeakPointDiagnosisResponse.weak_nodes`
- `next_action`
  - 展示类字段以 `LearningReportResponse.next_step` 为主
  - 执行态字段以 `SessionFeedbackResponse.selected_action/recommended_action` 为主

建议返回结构：

- `report`
  - 页面标题、总结、优势、薄弱项、题目结果、弱点节点、成长记录状态
- `next_action`
  - 推荐动作、可选动作、原因、按钮文案、跳转路由
- `meta`
  - `report_ready`、`quiz_status`

价值：

- 报告页可以删除 3 次请求和 `mergeReportPayloads()`。

### 5.4 `GET /api/users/me/knowledge-graph`

建议定义为“用户知识状态图”，而不是“某个 session 的章节快照”。

聚合来源：

- 当前活跃 session：`GetCurrentSessionService`
- 章节节点：`ConceptNodeRepository`
- 节点掌握度：`NodeMasteryRepository`
- 推荐节点/当前节点：可复用 `LearningInsightQueryService.getGrowthDashboard`

建议第一期范围：

- 只覆盖当前活跃 session 所在 chapter
- 用章节内 `order_no` 生成线性 edges
- 节点状态包含 `mastery/current/recommended/weak`

后续扩展：

- 支持多 chapter 合并
- 支持真实 prerequisite edges

价值：

- 前端知识图谱页不需要再自己拼节点状态和推荐信息。

### 5.5 `GET /api/users/me/growth-stats`

建议作为首页聚合接口，直接替换 `mocks/home.ts`。

聚合来源：

- 当前会话：`GetCurrentSessionService`
- 最近会话：`SessionHistoryService.listHistory`
- 最近评估：优先最近一个可用 `LearningReportResponse`
- 节点成长统计：`NodeMasteryRepository`
- 平均正确率：最近 session 的 `TaskRepository.findRecentTrainingAttempts` 聚合

建议返回结构：

- `metrics`
- `current_session`
- `recent_sessions`
- `recent_evaluation`
- `new_knowledge`
- `knowledge_gain_summary`

价值：

- 首页可以完全去掉 mock。

### 5.6 `GET /api/sessions/{sessionId}/next-action`

建议从“执行动作接口”拆出“读取建议接口”：

- `GET /api/sessions/{sessionId}/next-action`
  - 只返回推荐动作和跳转信息
- `POST /api/sessions/{sessionId}/next-action`
  - 继续保留执行语义

聚合来源：

- `LearningReportResponse.next_step`
- `SessionFeedbackResponse.recommended_action`
- `SessionFeedbackResponse.selected_action`

建议返回结构：

- `recommended_action`
- `selected_action`
- `reason`
- `target`
- `route`
- `label`
- `available_actions`

价值：

- 报告页、会话页、首页卡片都可以复用同一个“下一步”只读模型。

## 6. 推荐落地顺序

1. `GET /api/sessions/{sessionId}/report`
2. `GET /api/sessions/{sessionId}/overview`
3. `GET /api/sessions/{sessionId}/next-action`
4. `GET /api/users/me/growth-stats`
5. `GET /api/users/me/knowledge-graph`

原因：

- 前三个直接覆盖当前真实页面的前端拼装。
- 后两个主要用于替换首页 mock 和未来知识图谱页。

## 7. 兼容性建议

- 新增聚合接口时，不立即删除旧接口。
- 前端优先切新接口，旧接口保留为内部数据源。
- 等页面稳定后，再评估是否下线：
  - `/api/session/{sessionId}/learning-feedback/report`
  - `/api/session/{sessionId}/learning-feedback/weak-points`
  - `/api/session/{sessionId}/growth-dashboard`
  - `/api/sessions/{sessionId}/feedback`

## 8. 最终建议

- 最适合作为第一批聚合接口的是：
  - `GET /api/sessions/{sessionId}/report`
  - `GET /api/sessions/{sessionId}/overview`
  - `GET /api/sessions/{sessionId}/next-action`
- 最适合作为第二批用户级聚合接口的是：
  - `GET /api/users/me/growth-stats`
  - `GET /api/users/me/knowledge-graph`
- 若只选一个最高优先级接口，应该先做 `GET /api/sessions/{sessionId}/report`，因为它已经存在明确的“三接口 + normalizer 合并”前端拼装成本。
