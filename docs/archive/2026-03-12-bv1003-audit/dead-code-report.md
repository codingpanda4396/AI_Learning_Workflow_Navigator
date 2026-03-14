# Frontend Dead Code Report

基于 `frontend/src` 的静态引用扫描结果，仅供清理建议，不包含自动删除。

## 1. 未被引用的组件

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/components/cards/ProgressCard.vue` | 未发现从 `main.ts` 可达的引用；当前会话页已改用 `ProgressSummary.vue` | 可以，建议删除 |
| `frontend/src/components/cards/ReportSummaryCard.vue` | 未发现任何导入引用 | 可以，建议删除 |
| `frontend/src/components/common/StageBadge.vue` | 仅被 `TaskTimeline.vue` 引用，而 `TaskTimeline.vue` 本身未接入任何页面 | 可以，建议和 `TaskTimeline.vue` 一起删除 |
| `frontend/src/components/panels/NextActionPanel.vue` | 未发现任何导入引用 | 可以，建议删除 |
| `frontend/src/components/panels/TaskTimeline.vue` | 未发现从页面入口可达的引用，疑似旧版时间线面板 | 可以，建议删除 |
| `frontend/src/components/panels/WeakPointList.vue` | 未发现任何导入引用 | 可以，建议删除 |

## 2. 未被引用的 store 字段

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/stores/auth.ts` `token` | 未发现 store 外部读取；路由守卫和请求鉴权均直接使用 `storage` 中的 token | 谨慎，可以评估删除并改为纯持久化读取 |
| `frontend/src/stores/auth.ts` `isAuthenticated` | 未发现任何外部引用 | 可以，建议删除 |
| `frontend/src/stores/session.ts` `currentSessionId` | 仅在 store 内部赋值，未发现页面或组件读取 | 谨慎，可以评估删除 |

## 3. 未被使用的 API 封装

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/api/modules/*` | 本轮未发现未被使用的导出 API 封装 | 否 |

## 4. 未被使用的 utils

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/utils/*` | 本轮未发现未被使用的导出工具函数 | 否 |

## 5. mock 数据文件

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/mocks/home.ts` | 首页仍直接引用这份 mock 内容，当前属于“在线页面使用中的静态 mock 数据” | 暂不建议，除非首页改为真实接口/常量来源 |
| `frontend/src/mocks/learningPlan.ts` | 学习路径 API 失败时仍会 fallback 到该 mock 预览数据 | 暂不建议，需先移除 fallback |

## 6. 可能的历史版本代码

| 文件路径 | 原因 | 是否可以删除 |
| --- | --- | --- |
| `frontend/src/views/PlaceholderView.vue` | 未注册到路由，也未被任何页面引用，命名上也明显是占位页 | 可以，建议删除 |
| `frontend/src/components/panels/NextActionPanel.vue` | 与当前 `common` 卡片体系并行存在，但未接入页面，疑似旧版面板实现 | 可以，建议删除 |
| `frontend/src/components/panels/TaskTimeline.vue` | 与当前 `SessionView.vue` 中的简化时间线实现重复，且未接入页面 | 可以，建议删除 |
| `frontend/src/components/panels/WeakPointList.vue` | 未接入任何页面，命名风格也属于早期面板分组 | 可以，建议删除 |
| `frontend/src/components/cards/ProgressCard.vue` | 与当前 `ProgressSummary.vue` 职责重叠，未接入页面 | 可以，建议删除 |
| `frontend/src/components/cards/ReportSummaryCard.vue` | 与当前 `ReportBlock.vue` 职责重叠，未接入页面 | 可以，建议删除 |
| `frontend/src/types/common.ts` | `ApiError` / `SelectOption` / `AsyncState` 未发现任何引用，疑似预留或遗留类型文件 | 可以，建议删除 |
