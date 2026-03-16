-- Sprint 0 最小落库建议：10 张表。当前后端仍使用内存存储，此 DDL 供后续接库使用。
-- 复杂结构字段使用 JSON/Text 存储，字段命名与冻结清单一致。

-- 8.1 原始目标输入
CREATE TABLE IF NOT EXISTS learning_goal (
    id VARCHAR(64) PRIMARY KEY,
    raw_goal_text VARCHAR(1024) NOT NULL,
    time_budget VARCHAR(32),
    self_reported_level VARCHAR(32),
    preference_tags_json TEXT,
    goal_type_hint VARCHAR(32),
    subject_hint VARCHAR(128),
    topic_hints_json TEXT,
    source_context VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.2 结构化目标
CREATE TABLE IF NOT EXISTS structured_learning_goal (
    id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(64) NOT NULL,
    structured_goal_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.3 目标快照（规划消费）
CREATE TABLE IF NOT EXISTS goal_context_snapshot (
    id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(64) NOT NULL,
    snapshot_json TEXT,
    planning_mode VARCHAR(32),
    entry_granularity VARCHAR(32),
    risk_tags_json TEXT,
    strategy_hints_json TEXT,
    version INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.4 诊断会话
CREATE TABLE IF NOT EXISTS diagnosis_session (
    id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    status VARCHAR(32),
    generation_mode VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 8.5 诊断答案
CREATE TABLE IF NOT EXISTS diagnosis_answer (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_id VARCHAR(64) NOT NULL,
    question_id VARCHAR(64) NOT NULL,
    answer_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.6 用户画像快照
CREATE TABLE IF NOT EXISTS learner_profile_snapshot (
    id VARCHAR(64) PRIMARY KEY,
    diagnosis_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    profile_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.7 学习计划
CREATE TABLE IF NOT EXISTS learning_plan (
    id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(64) NOT NULL,
    diagnosis_id VARCHAR(64),
    status VARCHAR(32),
    preview_json TEXT,
    committed_at TIMESTAMP
);

-- 8.8 学习会话
CREATE TABLE IF NOT EXISTS learning_session (
    id VARCHAR(64) PRIMARY KEY,
    goal_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64) NOT NULL,
    status VARCHAR(32),
    current_task_index INT DEFAULT 0,
    task_sequence_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 8.9 任务执行记录
CREATE TABLE IF NOT EXISTS task_execution_record (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(32),
    completion_status VARCHAR(32),
    duration_minutes INT,
    interaction_count INT,
    user_summary_submitted BOOLEAN,
    micro_practice_result VARCHAR(64),
    detected_issue_tags_json TEXT,
    behavior_signals_json TEXT,
    learner_reflection TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8.10 学习报告
CREATE TABLE IF NOT EXISTS learning_report (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    result_status VARCHAR(32),
    report_json TEXT,
    next_action_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
