# AI Learning Workflow Navigator 项目进展总结

> 更新日期：2025-03-17

本文档从**业务**与**技术**两个维度，总结当前项目的开发进展。

---

## 一、业务进展

### 1.1 产品定位（已冻结）

- **核心价值**：帮助用户把 LLM 转化为真正有效的学习工具，而非停留在“问答案、生成作业”的浅层使用。
- **目标用户**：有明确短期学习目标、具备 LLM 使用入口、在计算机/408/数据结构等学习中“会问答案、不会系统推进”的学生。
- **主链路**：目标输入 → 用户诊断 → 学习规划 → 任务执行 → 反馈与 Next Action
- **非目标**：不做通用聊天工具、不做大而全课程平台、不追求完全自由生成的学习路径

### 1.2 MVP 范围（已冻结）

根据 `MVP开发冻结清单.md`，MVP 已明确：

| 类别 | 内容 |
|------|------|
| **要做** | 单轮目标输入、轻量诊断(4~6题)、规则主导+LLM辅助的计划预览、单会话任务执行、最小执行证据采集、单轮报告、下一步动作决策 |
| **不做** | 长周期课程运营、复杂知识图谱、多学科通用规划、高自由度诊断访谈、端到端 LLM 自由生成、复杂学习风格人格分析 |

### 1.3 主链路闭环完成度

| 环节 | 状态 | 说明 |
|------|------|------|
| **目标输入** | ✅ 已打通 | 支持 rawGoalText、timeBudget、selfReportedLevel、preferenceTags 等字段，产出 StructuredLearningGoal + GoalContextSnapshot |
| **用户诊断** | ✅ 已打通 | 创建诊断会话、固定 4~6 题模板、规则引擎产出 LearnerProfileSnapshot + DiagnosisEvidenceSummary |
| **学习规划** | ✅ 已打通 | 基于 PlanningContext 生成 LearningPlanPreview、推荐入口、策略、TaskBlueprint[]，支持预览与确认 |
| **任务执行** | ✅ 已打通 | 获取当前任务、记录交互、完成任务，产出 TaskExecutionRecord + ExecutionEvidenceSummary |
| **反馈与 Next Action** | ✅ 已打通 | 基于执行证据生成 LearningReport、NextActionDecision，支持确认下一步继续链路 |

### 1.4 核心对象流转（已实现）

```
Goal Input        → StructuredLearningGoal + GoalContextSnapshot
Diagnosis         → LearnerProfileSnapshot + DiagnosisEvidenceSummary
Planning          → LearningPlanPreview + TaskBlueprint[]
Execution         → TaskExecutionRecord + ExecutionEvidenceSummary
Feedback          → LearningReport + NextActionDecision
```

### 1.5 业务规则体系

- **规则主导、LLM 辅助**：核心决策（诊断、规划、Next Action）由规则系统产出；LLM 仅用于解释增强、文案润色（当前 MVP 阶段尚未接入 LLM，采用模板化表达）。
- **冻结枚举**：GoalType、PlanningMode、StrategyCode、TaskType、ResultStatus、NextActionType 等已按 MVP 文档统一。
- **固定样例**：内置“理解链表”演示链路，覆盖从目标到报告的完整流程。

---

## 二、技术进展

### 2.1 技术栈

| 层次 | 技术选型 |
|------|----------|
| **后端** | Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.5、PostgreSQL、Flyway 10.x |
| **前端** | Vue 3、Vue Router、Pinia、Vite 5、Tailwind CSS、Axios |
| **API** | RESTful，统一 GlobalResponse 格式 `{ code, message, data }` |

### 2.2 后端架构

```
backend/
├── api/                    # Controller、DTO、GlobalResponse、WebConfig
├── application/            # 应用服务、规则引擎、Assembler、Guard
├── domain/                 # 领域模型、枚举
└── infrastructure/
    ├── memory/             # InMemoryStore（会话状态、部分运行时缓存）
    └── persistence/        # 实体、Mapper、Repository（PostgreSQL）
```

### 2.3 已实现 API

| 模块 | 接口 | 说明 |
|------|------|------|
| **Goal** | `POST /api/goals` | 创建学习目标，返回结构化目标 |
| **Diagnosis** | `POST /api/diagnosis/sessions` | 创建诊断会话 |
| | `POST /api/diagnosis/submissions` | 提交诊断答案 |
| **LearningPlan** | `POST /api/learning-plans/preview` | 生成计划预览 |
| | `POST /api/learning-plans/commit` | 确认计划，创建学习会话 |
| **Task** | `GET /api/sessions/{sessionId}/current-task` | 获取当前任务 |
| | `POST /api/tasks/{taskId}/interactions` | 记录任务交互 |
| | `POST /api/tasks/{taskId}/complete` | 完成任务 |
| **Session** | `GET /api/sessions/{sessionId}/report` | 获取学习报告 |
| | `POST /api/sessions/{sessionId}/next-action` | 确认下一步动作 |

### 2.4 持久化

- **数据库**：PostgreSQL（配置在 application.yml）
- **迁移**：Flyway，已包含 V1 核心表、V2 索引与约束
- **核心表**：learning_goal、learning_session、diagnosis_session、diagnosis_answer、learner_profile_snapshot、learning_plan、session_task、task_interaction、task_completion
- **混合存储**：部分运行时状态（如会话进度、计划状态）仍使用 InMemoryStore，与持久化层并存

### 2.5 前端页面

| 页面 | 路由/视图 | 状态 |
|------|-----------|------|
| 目标输入 | GoalInputView.vue | 已实现 |
| 诊断 | DiagnosisView.vue | 已实现 |
| 学习规划 | LearningPlanView.vue | 已实现 |
| 任务执行 | TaskRunView.vue | 已实现 |
| 学习报告 | ReportView.vue | 已实现 |

前端具备 5 个主链路页面，路由可达，与后端接口对接。

### 2.6 规则引擎与核心逻辑

| 组件 | 职责 |
|------|------|
| GoalRuleEngine | 目标类型、风险标签、规划模式判定 |
| GoalContextDeriver | 从目标推导 GoalContextSnapshot |
| GoalKeywordExtractor | 关键词抽取 |
| DiagnosisRuleEngine | 诊断答案归一化、画像产出 |
| DiagnosisEvidenceBuilder | 诊断证据摘要（模板化，未走 LLM） |
| PlanStrategySelector | 规划策略选择 |
| PlanTemplateFactory | 任务模板与阶段编排 |
| RecommendedEntryBuilder | 推荐入口点构建 |
| SessionEvidenceAggregator | 执行证据汇总 |
| ReportApplicationService | 报告生成与 NextAction 决策 |

### 2.7 测试覆盖

| 测试类型 | 文件 | 说明 |
|----------|------|------|
| 单元测试 | GoalContextDeriverTest、GoalRuleEngineTest、PlanStrategySelectorTest | 规则逻辑验证 |
| 集成测试 | Sprint1IntegrationTest、Sprint2ReportAndNextActionIntegrationTest | 主链路端到端验证 |

### 2.8 尚未完成/待增强项

| 项 | 说明 |
|----|------|
| **LLM 接入** | 当前诊断证据、规划说明、报告文案均为模板化，未接入 LLM 做表达增强 |
| **内嵌导师** | 任务执行页的“与导师对话”入口需接入受约束的 LLM 对话 |
| **InMemoryStore 迁移** | 部分会话状态仍依赖内存，可逐步迁移至持久化 |
| **前端体验** | 按规则要求，P2 阶段再做页面视觉与文案优化 |
| **PERSISTENCE_DESIGN** | 原 persistence 设计文档已删除（git status 显示 D），可酌情补充 |

---

## 三、总结

### 业务侧

- MVP 主链路**已打通**：目标输入 → 诊断 → 规划 → 执行 → 反馈与 Next Action 可端到端跑通。
- 核心对象、枚举、接口合同已按冻结清单实现。
- 规则主导的决策逻辑已落地，LLM 增强尚未接入。

### 技术侧

- 后端主链路、持久化、规则引擎已实现，API 与 MVP 文档对齐。
- 前端 5 个主页面已就绪，可与后端联调演示。
- 测试覆盖规则核心与集成主路径。

### 推荐后续优先级

1. **P0**：确保全链路手工演示稳定（目标 → 诊断 → 规划 → 执行 → 报告 → 下一步）。
2. **P1**：接入 LLM，增强解释与导师对话，保持规则主决策不变。
3. **P2**：前端视觉与文案优化、执行证据更完整、Next Action 更一致。

---

*文档依据代码库、MVP 文档、CLAUDE.md 及项目规则整理。*
