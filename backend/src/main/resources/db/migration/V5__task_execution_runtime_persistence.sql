-- Sprint 3.1: Persist task execution runtime and execution-time evidences.
-- Keep TEXT for JSON-ish fields for cross-DB compatibility.

CREATE TABLE IF NOT EXISTS task_execution_runtime (
    id                        BIGINT       PRIMARY KEY,
    session_key               VARCHAR(64)  NOT NULL,
    session_id                BIGINT,
    task_id                   BIGINT,
    task_code                 VARCHAR(64)  NOT NULL,
    scaffold_id               VARCHAR(32),
    scaffold_json             TEXT         NOT NULL,
    current_state             VARCHAR(32)  NOT NULL,
    explore_turn_count        INT          NOT NULL,
    checkpoint_question       TEXT,
    self_explanation_evaluation VARCHAR(32),
    action_history_json       TEXT,
    created_at                TIMESTAMP    NOT NULL,
    updated_at                TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_execution_runtime_session_task
    ON task_execution_runtime (session_key, task_code);

CREATE INDEX IF NOT EXISTS idx_task_execution_runtime_session
    ON task_execution_runtime (session_key);

CREATE TABLE IF NOT EXISTS task_state_transition (
    id            BIGINT       PRIMARY KEY,
    session_key   VARCHAR(64)  NOT NULL,
    task_code     VARCHAR(64)  NOT NULL,
    from_state    VARCHAR(32)  NOT NULL,
    to_state      VARCHAR(32)  NOT NULL,
    reason        VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_state_transition_session_task
    ON task_state_transition (session_key, task_code, created_at);

CREATE TABLE IF NOT EXISTS task_message (
    id            BIGINT       PRIMARY KEY,
    session_key   VARCHAR(64)  NOT NULL,
    task_code     VARCHAR(64)  NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    content       TEXT         NOT NULL,
    detected_action VARCHAR(64),
    state_before  VARCHAR(32),
    state_after   VARCHAR(32),
    fallback_mode VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_message_session_task
    ON task_message (session_key, task_code, created_at);

CREATE TABLE IF NOT EXISTS task_checkpoint_result (
    id            BIGINT       PRIMARY KEY,
    session_key   VARCHAR(64)  NOT NULL,
    task_code     VARCHAR(64)  NOT NULL,
    question      TEXT         NOT NULL,
    answer        TEXT,
    result        VARCHAR(16)  NOT NULL,
    reason        TEXT,
    suggested_remedial_action TEXT,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_checkpoint_session_task
    ON task_checkpoint_result (session_key, task_code, created_at);

CREATE TABLE IF NOT EXISTS task_method_profile (
    id            BIGINT       PRIMARY KEY,
    session_key   VARCHAR(64)  NOT NULL,
    task_code     VARCHAR(64)  NOT NULL,
    profile_json  TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_method_profile_session
    ON task_method_profile (session_key, created_at);

