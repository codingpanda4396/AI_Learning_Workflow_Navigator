# Mock Removal Result

## 1. 删除了哪些 mock / fallback

- 删除 `frontend/src/api/modules/diagnosis.ts` 中基于 `VITE_ENABLE_DIAGNOSIS_API` 的 mock 分支。
- 删除 `frontend/src/api/modules/diagnosis.ts` 中请求失败后返回 mock 诊断题目和 mock 能力画像的逻辑。
- 删除 `frontend/src/mocks/learningPlan.ts`，停止使用 `createMockLearningPlanPreview()`。
- 首页移除了 `frontend/src/mocks/home.ts` 提供的 workflow、模块导航、成长摘要、最近学习、最近评估、新增知识点等假数据。

## 2. 哪些页面因此改为真实错误态

- `DiagnosisView` 在题目生成失败或提交失败时显示真实错误态，重试按钮仅重新发起真实接口请求。
- `LearningPlanView` 在 preview 初次生成失败时显示真实错误态；重新生成和确认失败时保留当前真实数据并展示真实错误信息。
- `HomeView` 在获取当前 session 失败时显示真实错误态和真实重试入口。

## 3. 哪些页面因此减少了展示内容

- 首页删除了 workflow 流程展示、模块导航、成长摘要、最近学习、最近评估、新增知识点，仅保留真实可获取的当前 session 与开始学习入口。
- 首页当前 session 卡片不再伪造当前任务摘要和进度；后端未返回时明确显示“暂不可用”。

## 4. 删除了哪些 mock 文件

- `frontend/src/mocks/home.ts`
- `frontend/src/mocks/learningPlan.ts`

同时删除了仅服务于首页假数据展示的组件：

- `frontend/src/components/home/GrowthSummaryPanel.vue`
- `frontend/src/components/home/ModuleNavPanel.vue`
- `frontend/src/components/home/WorkflowPipeline.vue`

## 5. 现在是否还存在任何 mock 相关逻辑

- 本次扫描与修改范围内，已删除诊断、学习计划、首页的 mock/fallback 假数据逻辑。
- 仍存在少量默认文案与空态提示，用于说明“暂无数据”或“接口未返回字段”，不承载伪造业务数据。
