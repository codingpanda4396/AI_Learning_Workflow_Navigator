# 持久化设计（Sprint 0 最小建议）

当前 Sprint 0 后端**仅使用内存存储**（`InMemoryStore`），不连接数据库。本文档与 `src/main/resources/schema.sql` 定义最小 10 张表结构，供后续接库时使用。

## 表清单

| 表名 | 作用 | 核心字段 |
|------|------|----------|
| `learning_goal` | 原始目标输入 | id, raw_goal_text, time_budget, self_reported_level, preference_tags_json, ... |
| `structured_learning_goal` | 结构化目标 | id, goal_id, structured_goal_json |
| `goal_context_snapshot` | 目标快照（规划用） | id, goal_id, snapshot_json, planning_mode, entry_granularity, ... |
| `diagnosis_session` | 诊断会话 | id, goal_id, session_id, status, generation_mode |
| `diagnosis_answer` | 诊断答案 | id, diagnosis_id, question_id, answer_json |
| `learner_profile_snapshot` | 用户画像快照 | id, diagnosis_id, session_id, profile_json |
| `learning_plan` | 学习计划 | id, goal_id, diagnosis_id, status, preview_json, committed_at |
| `learning_session` | 学习会话 | id, goal_id, plan_id, status, current_task_index, task_sequence_json |
| `task_execution_record` | 任务执行记录 | id, session_id, task_id, completion_status, duration_minutes, ... |
| `learning_report` | 学习报告 | id, session_id, result_status, report_json, next_action_json |

复杂对象（如 `StructuredLearningGoal`、`LearnerProfileSnapshot`、`LearningReport`）以 JSON 存入对应 `*_json` 字段，便于回溯且与冻结领域对象一致。

## 后续接库步骤

1. 在 `pom.xml` 中增加 Spring Data JPA 与所选数据库驱动（如 H2/PostgreSQL）。
2. 在 `infrastructure.entity` 下新增与上表对应的 JPA 实体。
3. 在 `infrastructure.repository` 下新增 JpaRepository 接口。
4. 通过 Assembler 将 Entity 与 Domain 互转，在 ApplicationService 中逐步用 Repository 替换 `InMemoryStore` 的 Map 访问。
