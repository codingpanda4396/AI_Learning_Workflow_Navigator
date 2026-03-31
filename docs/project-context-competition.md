# 项目背景文档（比赛交付级）

> 本文档基于当前仓库前后端代码整理，作为后续页面改版、执行页重构、前后端对齐、Cursor/Codex 协作的**统一上下文**。  
> 技术栈摘要：前端 Vue 3 + TypeScript + Pinia；后端 Spring Boot + 内存存储 `InMemoryStore`。

---

## 【1. 项目一句话定义】

这是一个 **以规则与脚手架为主、LLM 为辅的计算机主题学习引导** 的系统，用于帮助 **学习者在固定工作流中完成「目标—诊断—规划—执行—报告」闭环**，通过 **阶段化脚手架（STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION）与结构化任务编排** 达成 **可演示、可复现的单次高质量学习体验**（而非开放式 AI 聊天）。

---

## 【2. 比赛目标与演示策略】

### 2.1 核心目标（计算机设计大赛语境）

在有限演示时间内，证明本系统能完成 **「从学习目标到可执行动作」的闭环**，且 **产品形态是「学习工作台 + 脚手架」**，而不是「模型对话能力展示」。

### 2.2 要证明的核心创新点（强调脚手架，而非模型）

- **阶段化学习脚手架**：同一知识点按 **结构建立 → 机制理解 → 表达训练 → 反思收束** 推进；后端对 DFS/BFS 有独立定义类与校验器（如 `DfsBfsStructureScaffoldDefinition`、`DfsBfsUnderstandingScaffoldDefinition`、`DfsBfsTrainingScaffoldDefinition`、`DfsBfsReflectionScaffoldDefinition`），由 `LearningScaffoldEngineService` 驱动状态机，而非依赖模型「即兴发挥」。
- **决策与展示分离**：诊断产出 `LearnerProfileSnapshot`、规划产出 `LearningPlanPreview`、执行沉淀 `TaskExecutionRecord` / `LearningScaffoldEngineState`；LLM 主要用于 tutor 话术与增强，**过关与阶段推进由规则与引擎保证**。
- **可讲清楚的「为什么先这一步」**：规划页采用决策型 UI（`LearningPlanDecisionView`），执行页用 `MainTaskWorkbenchCard` 等组件显式表达「本轮唯一任务 / 交付物」。

### 2.3 演示策略（必须执行）

| 策略 | 说明 |
|------|------|
| **只打透 DFS/BFS** | 首页 `HOME_CONFIGURED_TOPIC_KEYS`（`homeQuickStart.ts`）中 **`ds_dfs_bfs` 与 DFS/BFS 脚手架全链路一致**；后端 `LearningScaffoldEngineService` 仅在知识包为 `ds_dfs_bfs` 时启用引擎（见对 `DfsBfsStructureValidator.PACK_ID` 的校验）。 |
| **其他知识点作扩展入口** | 同集合内还有 `os_process_thread`、`net_tcp_handshake`、`arch_cache_locality` 等，用于展示「多主题可挂接」，演示主线仍收束在 DFS/BFS。 |
| **固定 demo 路径** | 使用首页 **学科 / 主题选择 + Quick Start 意图**（`buildHomeGoalRequest`）生成结构化目标文案；登录用户可通过 `auth.recentLearningEntry` **一键续跑**最近会话（`GoalInputView` 的「继续学习」）。比赛可约定账号预先跑通一轮，保证现场路径稳定。 |
| **单次高质量闭环** | 强调 **一轮会话内**：诊断 → 规划确认 → 执行任务（脚手架推进）→ 报告与 Next Action；避免展示「无限闲聊」。 |

---

## 【3. 主链路概览（系统核心流程）】

整体：**目标输入 → 诊断 → 学习规划 → 任务执行 → 报告收口**。

### 3.1 目标输入

| 项 | 内容 |
|----|------|
| **输入** | `LearningGoalInput`：原始目标文案、时间预算、自评水平、偏好标签、科目/主题提示等（`GoalController` `POST /api/goals`）。 |
| **输出** | `CreateGoalData`：含 **`goalId`**、结构化目标等。 |
| **前端** | `GoalInputView.vue` → `createGoal`（`@/api/goals`）；`useWorkflowStore` 持久化 **`goalId`**、`structuredGoal`（sessionStorage）。 |
| **存储** | `InMemoryStore.goals`、`goalContextSnapshots`。 |

### 3.2 诊断

| 项 | 内容 |
|----|------|
| **输入** | `POST /api/diagnosis/sessions`（body 含 `goalId`）创建诊断会话；`POST /api/diagnosis/submissions` 提交答案（含 **`diagnosisId`**）。 |
| **输出** | `LearnerProfileSnapshot`、`DiagnosisEvidenceSummary` 等（见 `SubmitDiagnosisData`）。 |
| **前端** | `DiagnosisView.vue`；store 中 **`diagnosisId`**、画像与证据摘要。 |
| **路由守卫** | 无 `goalId` 不能进入 `/diagnosis`（`router/index.ts`）。 |

### 3.3 学习规划

| 项 | 内容 |
|----|------|
| **输入** | `POST /api/learning-plans/preview`（`goalId` + `diagnosisId`）；`POST /api/learning-plans/commit`（**`planId`**）。 |
| **输出** | `PlanPreviewData`；`CommitPlanData` 含 **`sessionId`**（学习会话主键，贯穿执行与报告）。 |
| **前端** | `LearningPlanView.vue` 仅包装 **`LearningPlanDecisionView.vue`**；`previewPlan` / `commitPlan`，store 存 **`planId`**、`planPreview`。 |
| **路由守卫** | 进入 `/plan` 需 **`goalId` + `diagnosisId`**。 |

### 3.4 任务执行（含脚手架）

| 项 | 内容 |
|----|------|
| **输入** | `GET /api/sessions/{sessionId}/current-task`、`GET .../current-task-guidance`；任务维度 `GET /api/tasks/{taskId}/scaffold`、`POST .../messages`、`POST .../self-explanation`、`POST .../checkpoint`、`POST .../complete`；脚手架引擎 `GET/POST /api/tasks/{taskId}/learning-scaffold/...`（见 `LearningScaffoldController`）。 |
| **输出** | `CurrentTaskData`、`TaskScaffoldResponse`、`TaskMessageResponse`、脚手架 `StageScaffold` / `LearningScaffoldActionResult` 等。 |
| **前端** | 主界面 **`TaskRunView.vue`**（路由 `/task` 与 `/tasks/:taskId/run`）；`useWorkflowStore` 中 **`sessionId`**、`currentTaskId`、`currentTask`、`progress`。 |
| **路由守卫** | 进入执行或报告需 **`sessionId`**；访问 `/task` 时若已有 `currentTaskId` 会重写到 **`taskRun`** 命名路由。 |
| **存储** | `InMemoryStore.sessions`、`sessionTaskRecords`、`taskExecutionRuntimes`（`sessionId|taskId`）、`executableTaskSpecs`。 |

### 3.5 报告收口

| 项 | 内容 |
|----|------|
| **输入** | `GET /api/sessions/{sessionId}/report`；`POST /api/sessions/{sessionId}/next-action`。 |
| **输出** | `ReportData`、`NextActionConfirmData`；含结果状态、证据摘要、学习方法画像等。 |
| **前端** | `ReportView.vue`；store 中 `report`、`nextActionDecision`。 |

### 3.6 关键 ID 流转小结

```
goalId ──► diagnosisId ──► planId ──► commit 得 sessionId
                                      │
                                      ├── currentTaskId（执行页）
                                      └── taskId（URL 与 API 路径参数）
```

**注意**：`sessionId` 是学习会话核心外键；`taskId` 标识当前任务实例；脚手架运行时挂在 `TaskExecutionRuntime` 上，与 `sessionId`+`taskId` 唯一对应。

---

## 【4. 当前页面现状（重点模块）】

### 4.1 首页（目标输入 `/goal`）

| 维度 | 说明 |
|------|------|
| **当前目标** | 低门槛选择 **学科 → 主题 → Quick Start 意图**，生成结构化目标并创建 `goalId`，进入诊断。 |
| **实现情况** | `GoalInputView.vue`：流程说明、继续学习卡片（`recentLearningEntry`）、`HOME_SUBJECTS` 与主题卡片；未配置主题置灰（`isHomeTopicConfigured`）。 |
| **当前问题** | 信息块仍偏多（流程条、多卡片）；对路人仍可能像「选课的门户」而非「一轮学习的入口」。 |
| **改版方向** | **低负担入口**：首屏只保留一条主路径 + 弱化的「续跑」；**Quick Start** 与 `HOME_QUICK_STARTS` 对齐，减少并列说明。 |

### 4.2 诊断页（`/diagnosis`）

| 维度 | 说明 |
|------|------|
| **当前目标** | 基于 `goalId` 创建诊断会话，多题单选，提交后生成画像并进入规划。 |
| **实现情况** | `DiagnosisView.vue`：`FormCard` 逐题、单选 radio、提交后进入规划。 |
| **当前问题** | 交互形态接近 **测验问卷**（题号、选项列表），容易触发「考试」心智而非「轻量判断」。 |
| **改版方向** | **轻量判断，不做测验**：减少题感（合并维度、用场景判断/二选一），强调「为规划服务」而非得分。 |

### 4.3 规划页（`/plan`）

| 维度 | 说明 |
|------|------|
| **当前目标** | 预览计划并 **确认开跑**，产生 `sessionId`。 |
| **实现情况** | `LearningPlanView.vue` → `LearningPlanDecisionView.vue`：`PlanDecisionHero`、`PlanWhyAccordion`、`PlanStageRail`、`PlanFirstTaskCard`，`buildLearningPlanDecisionViewModel` 组装决策模型。 |
| **当前问题** | 虽已「决策化」，但用户仍可能扫到 **路径条与模块数量**，弱化了「唯一决策瞬间」。 |
| **改版方向** | **决策页，而不是任务列表**：强化「为何是这条路径 + 一键开跑」，路径细节默认收起或降级为辅助。 |

### 4.4 执行页（重点）

| 维度 | 说明 |
|------|------|
| **当前结构** | 实际执行入口为 **`TaskRunView.vue`**（`/task`、`/tasks/:taskId/run`）。**单列居中**（`max-w-3xl` 区域）：**`TaskRunPhaseHeader`** → **`MainTaskWorkbenchCard`**（本轮唯一任务 / 交付 / 怎么开始）→ **`TaskExpressionPanel`**（表达区、结构化字段、微检查）→ **`TaskFeedbackDeck`** → **`TaskRunDualActionBar`** 底部主副操作。`ExecutionView.vue` 仅为 **无 `sessionId` 时重定向**占位，不承载主 UI。 |
| **当前问题** | ① **主任务虽已单独成卡**，但与说明、反馈同列堆叠时，首屏仍可能被「多段文字」分摊注意力；② **阶段感**依赖 `emphasisPhase` 与文案，**视觉与信息架构上四阶段差异仍不够「换台」**；③ 整体气质仍接近 **「读说明 + 填写 + 提交」**，与「学习工作台」相比缺少稳定分区与进度锚点（对比规则中的「脚手架行动卡必须最显眼」仍有差距）。 |
| **改版目标** | **单主线**：**阶段头部 + 主任务卡 + 脚手架行动区 + 表达区 + 反馈区** 层次固定；四阶段 **STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION** 在版式、权重、文案语气上 **可感知差异**（与 `execution-page.mdc`、`buildExecutionPageModel.ts` 中 phase 配置一致）。 |

### 4.5 报告页（`/report`）

| 维度 | 说明 |
|------|------|
| **当前目标** | 展示 `GET /api/sessions/{sessionId}/report` 结果，并支持后续动作确认。 |
| **实现情况** | `ReportView.vue`：结果状态、目标回顾、进度与证据、摘要、学习方法画像、未解决问题等卡片列表。 |
| **当前问题** | 偏 **阅读型总结**，「下一步策略」与 **决策感** 可更强；与 `NextActionDecision` 的衔接可更突出。 |
| **改版方向** | **策略收口页 + 下一步决策**：首屏给「这一轮结论 + 我接下来具体做什么」，证据与细节二级展开。 |

---

## 【5. 前端结构现状】

### 5.1 路由（`frontend/src/router/index.ts`）

| 路径 | 名称 | 组件 | 说明 |
|------|------|------|------|
| `/` | — | redirect `/goal` | |
| `/goal` | `goal` | `GoalInputView.vue` | 目标输入 |
| `/diagnosis` | `diagnosis` | `DiagnosisView.vue` | 需登录、`goalId` |
| `/plan` | `plan` | `LearningPlanView.vue` | 需 `goalId`+`diagnosisId` |
| `/execution` | `execution` | `ExecutionView.vue` | 重定向用 |
| `/task` | `task` | `TaskRunView.vue` | 有 `currentTaskId` 时跳到 `taskRun` |
| `/tasks/:taskId/run` | `taskRun` | `TaskRunView.vue` | 显式 taskId |
| `/report` | `report` | `ReportView.vue` | 需 `sessionId` |
| `/auth/login`、`/auth/register` | — | `AuthView.vue` | 访客路由 |

### 5.2 关键 View 文件

- `GoalInputView.vue`、`DiagnosisView.vue`、`LearningPlanView.vue`（包装）**`LearningPlanDecisionView.vue`**、`TaskRunView.vue`、`ReportView.vue`、`AuthView.vue`。

### 5.3 Store

- **`useWorkflowStore`**（`stores/workflow.ts`）：全流程 ID 与业务对象（goal / diagnosis / plan / session / currentTask / progress / report / nextAction）。
- **`useAuthStore`**：登录态与 **`recentLearningEntry`**（续跑入口）。
- **`useAiTutorStore`**、`stores/toast.ts`：辅导浮层与全局提示。

### 5.4 Composable / 表现模型

- **`useLearningScaffoldEngine`**（`composables/useLearningScaffoldEngine.ts`）：脚手架引擎 API 封装。
- **`useStructureSkeletonFlow`** 等：结构阶段骨架生成流程。
- **`buildExecutionPageModel.ts`**：将 task、scaffold、guidance 等转为 **`ExecutionPageViewModel` / `TaskExecutionWorkbenchModel`**（含阶段 copy、`WORKBENCH_PHASE_SEQUENCE`）。
- **`learningPlanDecisionModel.ts`、`planPresentationModel.ts`**：规划决策页模型。

### 5.5 执行页核心文件（改动优先定位）

| 类型 | 路径 |
|------|------|
| 页面 | `views/TaskRunView.vue` |
| 布局/主卡 | `components/task-run/TaskRunPhaseHeader.vue`、`MainTaskWorkbenchCard.vue`、`TaskExpressionPanel.vue`、`TaskFeedbackDeck.vue`、`TaskRunDualActionBar.vue` |
| 模型构建 | `utils/buildExecutionPageModel.ts` |
| 常量 | `constants/executionWorkbenchContent.ts`、`constants/uiCopy.ts`、`constants/taskRunUi.ts`、`constants/dfsBfsStructureWorkbenchCopy.ts` 等 |
| API | `api/task.ts`、`api/learningScaffold.ts` |
| 类型 | `types/taskExecutionWorkbench.ts`、`types/dto.ts`、`types/scaffoldEngine.ts` |

---

## 【6. 后端结构现状】

### 6.1 API 一览（与前端对齐）

| 领域 | 方法与路径 |
|------|------------|
| 目标 | `POST /api/goals` |
| 诊断 | `POST /api/diagnosis/sessions`、`POST /api/diagnosis/submissions` |
| 规划 | `POST /api/learning-plans/preview`、`POST /api/learning-plans/commit` |
| 执行 | `GET /api/sessions/{sessionId}/current-task`、`GET .../current-task-guidance` |
| 任务 / 脚手架流 | `GET /api/tasks/{taskId}/scaffold`、`POST .../messages`、`POST .../self-explanation`、`POST .../checkpoint`、`POST .../complete` |
| 学习脚手架引擎 | `GET /api/tasks/{taskId}/learning-scaffold/stage/...`、`POST .../action`、`POST .../structure/skeleton`、`POST .../structure/complete`（`LearningScaffoldController`） |
| 报告 | `GET /api/sessions/{sessionId}/report`、`POST .../next-action` |

### 6.2 `TaskExecutionFlowService`

- 职责：**任务运行时** —— 加载/构建 `TaskScaffold`、状态机 `TaskExecutionState`（ORIENT / EXPLORE / SELF_EXPLAIN / CHECK / REMEDIAL / PASS 等）、`postMessage` 探索轮、`postSelfExplanation`、`postCheckpoint`、`getScaffold`、`getExecutionSummary`。
- 与 **Workbench 展示相位** 的映射见 `toWorkbenchPhase`：**ORIENT→STRUCTURE，EXPLORE→UNDERSTANDING，SELF_EXPLAIN/REMEDIAL→TRAINING，CHECK/PASS→REFLECTION**（与四阶段产品语义对齐）。
- **知识包特化**：`buildObservationBullets` 对 `ds_dfs_bfs`、`net_tcp_handshake`、`os_process_thread`、`arch_cache_locality` 等返回不同观察要点（DFS/BFS 为演示重点）。

### 6.3 `LearningScaffoldEngineService`（与 `TaskExecutionFlowService` 协作）

- 仅在 **`ds_dfs_bfs`** 知识包下启用（否则抛「需 DFS/BFS 知识点」）。
- 维护 **`LearningScaffoldEngineState`**：阶段键 **STRUCTURE → UNDERSTANDING → TRAINING → REFLECTION**，动作提交、结构阶段骨架生成与完成、反思组装（`ReflectionAssembler`）等。
- 注入 **DFS/BFS 专用校验与评估器**：`DfsBfsStructureValidator`、`DfsBfsUnderstandingValidator`、`DfsBfsTrainingEvaluator`、`DfsBfsReflectionEvaluator` 及各类 `DfsBfs*ScaffoldDefinition`。

### 6.4 报告相关数据

- `ReportApplicationService` + `SessionController`：`ReportData` 含结果状态、证据、学习方法画像等；与前端 `ReportView` 字段对应。

### 6.5 持久化说明

- 当前为 **内存存储** `InMemoryStore`，无数据库；演示与联调需注意进程重启丢数据，比赛环境可固定进程或使用约定账号会话。

---

## 【7. DFS/BFS 特化现状】

### 7.1 已支持的四阶段（数据与规则层）

| 阶段 | 后端定义 / 行为 |
|------|-----------------|
| **STRUCTURE** | `DfsBfsStructureScaffoldDefinition`：位置、前置、后续、边界等动作；结构骨架 `StructureSkeletonComposer`；校验 `DfsBfsStructureValidator`。 |
| **UNDERSTANDING** | `DfsBfsUnderstandingScaffoldDefinition`：DFS 步骤、BFS 层次等；`DfsBfsUnderstandingValidator`。 |
| **TRAINING** | `DfsBfsTrainingScaffoldDefinition`：如无权最短路、顺序差异等；`DfsBfsTrainingEvaluator`。 |
| **REFLECTION** | `DfsBfsReflectionScaffoldDefinition`：错误回忆、根因、决策规则等；`DfsBfsReflectionEvaluator`、`ReflectionAssembler`。 |

### 7.2 UI 产品化程度

- **已有**：`TaskRunView` + `buildExecutionPageModel` + `MainTaskWorkbenchCard` 等对 **主任务、表达、反馈** 有完整串联；常量层有 DFS/BFS 专用文案（如 `dfsBfsStructureWorkbenchCopy.ts`）。
- **缺口**：四阶段在 **视觉层级、交互节奏、首屏权重** 上仍 **未充分「换台」**；用户仍可能感到「同一套表单在不同文案间切换」，而非 **四种明确的学习气质**。这与规则文件 `execution-page.mdc` 的要求相比，是主要差距。

### 7.3 当前最大缺口（必须写清）

- **UI 未充分体现阶段差异**：后端与 `LearningScaffoldEngineState` 已能分阶段推进，但前端执行页 **仍需一次「工作台级」重构**，让 STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION **一眼可辨**。

---

## 【8. 当前最大问题清单（产品层）】

1. **执行页整体仍偏「作答页」**：说明、输入、反馈纵向堆叠，**学习工作台**的稳定分区与「脚手架最显眼」尚未完全达成。  
2. **四阶段缺少强 UI 差异**：阶段切换在文案上有，在 **版式与视觉权重** 上不足。  
3. **主线注意力易分散**：即使已有「本轮唯一任务」卡，用户仍可能在多段说明间迷路。  
4. **任务/脚手架的「可执行感」可再加强**：行动句、完成标准与 **单一路径提交** 需更 ruthlessly 简短。  
5. **反馈区对「本轮结论」的统领性**：`TaskFeedbackDeck` 已有，但与主任务的 **主次关系** 仍可优化。  
6. **首页工具感**：学科/主题/意图选择完整，但对赛评委仍可能像「配置向导」而非「开始学习」。  
7. **诊断测验感**：题型与版式偏问卷，弱于「轻量判断」。  
8. **报告页偏总结罗列**：策略收口与 **下一步决策** 可提到首屏。

（以上均为产品体验判断，不涉及具体框架版本。）

---

## 【9. 接下来两周优先级】

### S 级

- **执行页重构**：以 `TaskRunView.vue` 与 `buildExecutionPageModel.ts` 为核心，落实 **单主线 + 四阶段差异化气质**，脚手架区压过解释性文字。

### A 级

- **规划页强化决策感**：在现有 `LearningPlanDecisionView` 基础上，进一步突出 **一键开跑** 与「为什么选这条路径」。  
- **报告页强化收口**：`ReportView` 首屏 **策略句 + 下一步**，证据后置。

### B 级

- **首页优化**：`GoalInputView` 减负担、强 Quick Start 与续跑。  
- **诊断页轻量化**：`DiagnosisView` 弱化测验感，对齐轻量判断。

---

## 【10. 设计原则与禁区】

### 设计原则

- **首页必须低负担**：一眼知道「怎么开始」或「怎么续跑」。  
- **规划页必须做决策**：用户带走的是 **认可能力路径**，不是任务清单阅读。  
- **执行页必须是学习工作台**：**当前任务** 首屏中心，**脚手架** 比大段说明更显眼。  
- **报告页必须是策略收口**：可迁移的 **下一步行动**，而不是仅罗列学了什么。

### 禁区

- **不做聊天页**：主流程不是 thread UI；`postMessage` 探索轮是执行机制之一，但产品表达上不能做成「和 AI 聊学」。  
- **不做大而全平台**：演示只打穿 **DFS/BFS**，其他主题为扩展叙事。  
- **不让 AI 成为主界面**：导师/浮层（如 `AiTutor`）只能是辅助，不能抢 **脚手架与当前任务**。  
- **不堆信息、不堆模块**：同一屏避免多个「主说明区」并列。  
- **不出现多个主操作按钮**：单一主行动（与 `TaskRunDualActionBar` 的 primary/secondary 分工一致，避免再增加并列 primary）。

---

**文档维护**：当路由、store 字段、`LearningScaffoldEngineService` 启用条件或 DFS/BFS 定义类变更时，应同步更新本节与第 3、5、6、7 章。
