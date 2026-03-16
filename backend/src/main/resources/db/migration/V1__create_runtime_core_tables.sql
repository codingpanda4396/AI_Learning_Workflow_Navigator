-- Sprint 2 runtime core tables.
-- All JSON-like fields use TEXT for compatibility across H2/MySQL.

CREATE TABLE IF NOT EXISTS learning_goal (
    id            BIGINT       PRIMARY KEY,
    raw_goal_text TEXT         NOT NULL,
    time_budget          VARCHAR(32),
    self_reported_level  VARCHAR(64),
    preference_tags_json TEXT,
    goal_type_hint       VARCHAR(64),
    subject_hint         VARCHAR(64),
    topic_hints_json     TEXT,
    source_context       VARCHAR(255),
    structured_goal_json TEXT         NOT NULL,
    goal_context_json    TEXT         NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS learning_session (
    id                    BIGINT      PRIMARY KEY,
    goal_id               BIGINT      NOT NULL,
    diagnosis_session_id  BIGINT,
    plan_id               BIGINT,
    status                VARCHAR(32) NOT NULL,
    current_task_id       BIGINT,
    total_task_count      INT         NOT NULL,
    completed_task_count  INT         NOT NULL,
    started_at            TIMESTAMP,
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP   NOT NULL,
    updated_at            TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS diagnosis_session (
    id               BIGINT      PRIMARY KEY,
    session_id       BIGINT      NOT NULL,
    goal_id          BIGINT      NOT NULL,
    status           VARCHAR(32) NOT NULL,
    generation_mode  VARCHAR(32) NOT NULL,
    questions_json   TEXT        NOT NULL,
    submitted_at     TIMESTAMP,
    completed_at     TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS diagnosis_answer (
    id                   BIGINT      PRIMARY KEY,
    diagnosis_session_id BIGINT      NOT NULL,
    question_id          VARCHAR(64) NOT NULL,
    answer_json          TEXT        NOT NULL,
    created_at           TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS learner_profile_snapshot (
    id                   BIGINT      PRIMARY KEY,
    diagnosis_session_id BIGINT      NOT NULL,
    session_id           BIGINT      NOT NULL,
    profile_json         TEXT        NOT NULL,
    planning_mode        VARCHAR(64),
    entry_strategy       VARCHAR(64),
    entry_granularity    VARCHAR(64),
    feedback_mode        VARCHAR(64),
    risk_tags_json       TEXT,
    key_evidence_json    TEXT,
    created_at           TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS learning_plan (
    id                     BIGINT      PRIMARY KEY,
    session_id             BIGINT      NOT NULL,
    goal_id                BIGINT      NOT NULL,
    diagnosis_session_id   BIGINT      NOT NULL,
    status                 VARCHAR(32) NOT NULL,
    strategy_code          VARCHAR(64) NOT NULL,
    recommended_entry_json TEXT        NOT NULL,
    plan_snapshot_json     TEXT        NOT NULL,
    success_criteria_json  TEXT,
    risks_json             TEXT,
    key_evidence_json      TEXT,
    committed_at           TIMESTAMP,
    created_at             TIMESTAMP   NOT NULL,
    updated_at             TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS session_task (
    id                       BIGINT       PRIMARY KEY,
    session_id               BIGINT       NOT NULL,
    plan_id                  BIGINT       NOT NULL,
    stage_code               VARCHAR(64)  NOT NULL,
    task_code                VARCHAR(64)  NOT NULL,
    task_type                VARCHAR(64)  NOT NULL,
    order_index              INT          NOT NULL,
    title                    VARCHAR(255) NOT NULL,
    objective                TEXT         NOT NULL,
    task_snapshot_json       TEXT         NOT NULL,
    completion_criteria_json TEXT,
    estimated_minutes        INT,
    status                   VARCHAR(32)  NOT NULL,
    started_at               TIMESTAMP,
    completed_at             TIMESTAMP,
    last_interacted_at       TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS task_interaction (
    id                       BIGINT       PRIMARY KEY,
    session_id               BIGINT       NOT NULL,
    task_id                  BIGINT       NOT NULL,
    interaction_type         VARCHAR(32)  NOT NULL,
    user_input               TEXT,
    assistant_output_summary TEXT,
    extracted_signals_json   TEXT,
    created_at               TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS task_completion (
    id                       BIGINT      PRIMARY KEY,
    session_id               BIGINT      NOT NULL,
    task_id                  BIGINT      NOT NULL,
    completion_input_json    TEXT        NOT NULL,
    completion_status        VARCHAR(32) NOT NULL,
    quality_level            VARCHAR(32),
    detected_gap_tags_json   TEXT,
    risk_tags_json           TEXT,
    next_action_hints_json   TEXT,
    created_at               TIMESTAMP   NOT NULL
);

