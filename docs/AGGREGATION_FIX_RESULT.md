# Aggregation Fix Result

## 1. Report 聚合如何修复
- 新增后端接口 `GET /api/sessions/{sessionId}/report`。
- 后端在 `LearningInsightQueryService` 中统一聚合报告主信息、题目结果、弱点列表、推荐动作、下一步建议和已选动作。
- 前端 `ReportView` 不再并行请求 `/feedback`、`/learning-feedback/report`、`/learning-feedback/weak-points`，只消费单一 `LearningReport` 模型。

## 2. SessionView 数据来源如何收敛
- 后端 `SessionOverviewResponse` 新增 `summary` 字段。
- `summary` 直接提供当前任务标题、当前任务说明、下一步提示、主入口按钮文案/路径、最近报告摘要。
- 前端 `SessionView` 改为只渲染 `overview.summary`，不再在页面里用大量 `switch` 自行推导业务结论。

## 3. 首页如何收敛
- 首页保留两个真实能力：展示当前 session、创建新 session 并进入 diagnosis。
- 去掉假导航和半残占位，只保留真实可用入口。
- 当前 session 面板只展示真实 session 信息与进入 `/sessions/:sessionId` 的入口。
