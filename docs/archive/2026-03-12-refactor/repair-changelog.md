# Repair Changelog

1. 扫描前端 `views/components/stores/types/api` 与后端 report / overview 相关 controller、service、dto。
2. 确认前端 report 仍在并行拉取三份数据，后端已有聚合基础服务但缺少统一 session report 出口。
3. 新增后端 `GET /api/sessions/{sessionId}/report`，在 `LearningInsightQueryService` 中统一收口报告数据。
4. 为 `SessionOverviewResponse` 增加 `summary`，把当前任务、下一步入口、最近报告摘要后移到后端。
5. 改造前端 normalizer、feedback api、report/session/home 三个页面，切到单一聚合模型。
6. 清理首页为最小可用入口，保留当前 session 与创建 session 两个真实能力。
7. 删除 review 已明确可删的未使用组件与占位页面。
8. 清理冗余 store state 与 last-session 本地存储残留。
9. 执行 `mvn -q -DskipTests compile` 与 `corepack pnpm build` 验证。
