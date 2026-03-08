CREATE TABLE IF NOT EXISTS app_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(256) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_at TIMESTAMPTZ,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT uk_app_user_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_user_username ON app_user(username);

ALTER TABLE learning_session
  ADD COLUMN IF NOT EXISTS user_pk BIGINT,
  ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE learning_session
  ADD CONSTRAINT fk_learning_session_user_pk
  FOREIGN KEY (user_pk) REFERENCES app_user(id);

ALTER TABLE learning_session DROP CONSTRAINT IF EXISTS learning_session_user_id_chapter_id_key;

CREATE INDEX IF NOT EXISTS idx_session_user_created ON learning_session(user_pk, created_at DESC);

CREATE TABLE IF NOT EXISTS learning_event (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  event_type VARCHAR(32) NOT NULL,
  event_data JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_session ON learning_event(session_id);
CREATE INDEX IF NOT EXISTS idx_event_user ON learning_event(user_id);
