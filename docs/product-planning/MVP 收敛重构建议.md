# MVP 收敛重构建议

## 1. 当前流程问题清单（基于现有代码）
1. `POST /api/session/goal-diagnose` 在无题目/无历史行为前提下输出评分与风险，属于伪评估。涉及：
   - `backend/src/main/java/com/pandanav/learning/api/controller/SessionController.java`
   - `backend/src/main/java/com/pandanav/learning/application/service/GoalDiagnosisService.java`
   - `backend/src/main/java/com/pandanav/learning/api/dto/session/GoalDiagnosisResponse.java`
2. `POST /api/session/path-options` 返回固定三路线，且前端选择结果不参与后续任务规划，属于伪个性化。涉及：
   - `backend/src/main/java/com/pandanav/learning/application/service/PathOptionsService.java`
   - `frontend/src/views/SessionView.vue`（Step 2）
   - `frontend/src/stores/session.ts`（`selectedPathId`）
3. `planSession` 返回 `plan_source/plan_reasoning_summary/risk_flags`，但对用户主链路无必要，强化“包装感”。涉及：
   - `backend/src/main/java/com/pandanav/learning/api/dto/session/PlanSessionResponse.java`
   - `backend/src/main/java/com/pandanav/learning/application/service/PlanSessionTasksService.java`
4. 阶段内容 fallback 文案是硬编码模板，存在“固定总结/固定建议”风险。涉及：
   - `backend/src/main/java/com/pandanav/learning/application/service/TaskRunnerService.java`（`generateByStage`）
5. Session 页面四步中前两步（诊断/路线）不驱动状态推进，造成伪闭环。涉及：
   - `frontend/src/views/SessionView.vue`
   - `frontend/src/stores/session.ts`

## 2. 建议删除/下线项

### A. 伪评估
- 删除接口：`POST /api/session/goal-diagnose`
- 删除服务与 DTO：`GoalDiagnosisService`、`GoalDiagnosisRequest/Response`
- 下线前端依赖：`diagnoseGoal` API、`goalDiagnosis` store 字段、Session Step1 诊断展示
- 原因：无证据输入却输出能力结论，误导用户
- 替代：在创建会话后仅显示“目标理解摘要（可选）”，不输出评分/强弱项诊断

### B. 写死三路线
- 删除接口：`POST /api/session/path-options`
- 删除服务与 DTO：`PathOptionsService`、`PathOptionsResponse`
- 下线前端依赖：`pathOptions/selectedPathId` 相关状态和 Step2 UI
- 原因：与后续计划生成无因果关系，且不可演进
- 替代：直接由 `planSession` 生成初始学习步骤，前端展示步骤清单

### C. 写死总结/建议
- 删除或降级 `TaskRunnerService.generateByStage` 中固定总结语气（特别是 REFLECTION 固定建议）
- 原因：非过程驱动，容易暴露“模板感”
- 替代：统一由学习过程状态生成“阶段反馈”，措辞仅陈述已完成步骤与覆盖范围

### D. 无状态意义包装层
- 下线 `plan-preview` 与相关 `PlanMode/PlanSource` 在 MVP 主流程中的暴露
- 下线 `/session/{id}/path` 在 MVP 页面的强依赖（可保留内部调试）
- 精简 `PlanSessionResponse` 非必需字段
- 原因：增加心智负担且不推进主链路

## 3. 保留后的最小主链路
1. 输入 `course/chapter/target(goal_text)`
2. 创建 `learning_session`
3. 生成 3~5 个初始学习步骤（`title/objective/stage_type/content_summary/expected_output`）
4. 用户按步骤学习：获取当前 step、执行 step、next/complete
5. 基于真实 session/task/stage 完成记录生成阶段反馈（不做“掌握度”断言）

## 4. 下一阶段重建重点
1. 先把“过程反馈”做真：只基于 `task.status + task_attempt + node_mastery`
2. 再接“评估”：必须引入题目作答或行为证据后再开放诊断接口
3. 再做个性化：先有规则与画像数据，再恢复“路径选择”

## 5. 最小重构实施方案（代码减法优先）

### 建议删除
- `backend/src/main/java/com/pandanav/learning/application/service/GoalDiagnosisService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/PathOptionsService.java`
- `backend/src/main/java/com/pandanav/learning/api/dto/session/GoalDiagnosisRequest.java`
- `backend/src/main/java/com/pandanav/learning/api/dto/session/GoalDiagnosisResponse.java`
- `backend/src/main/java/com/pandanav/learning/api/dto/session/PathOptionsResponse.java`

### 建议改名
- `LearningFeedbackController` -> `SessionSummaryController`
- `WeakPointDiagnosisService` -> `SessionSummaryService`
- `WeakPointDiagnosisResponse` -> `LearningSummaryResponse`

### 建议下线
- `SessionController` 中：`/goal-diagnose`、`/path-options`、`/plan-preview`
- 前端 `SessionView` 四步改三步：`学习计划 -> 分步学习 -> 阶段反馈`

### 改完效果
- 主流程只保留“输入 -> 计划 -> 执行 -> 反馈”
- 每个接口都对应真实状态推进或真实状态读取
- 后续扩展点（训练评估、动态调度）可在不推倒重来的前提下增加

## 一句话产品新表述
面向大学生的 AI 学习任务拆解与分步引导系统（基于学习过程生成阶段反馈）。
