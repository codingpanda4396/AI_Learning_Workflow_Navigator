CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_username ON app_user (username);

CREATE TABLE IF NOT EXISTS user_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_session_token_hash ON user_session (token_hash);
CREATE INDEX IF NOT EXISTS idx_user_session_user_id ON user_session (user_id);

ALTER TABLE learning_goal
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE learning_session
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE diagnosis_session
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE learning_plan
    ADD COLUMN IF NOT EXISTS user_id BIGINT;
