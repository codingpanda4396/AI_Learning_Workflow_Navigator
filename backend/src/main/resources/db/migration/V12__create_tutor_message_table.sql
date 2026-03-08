CREATE TABLE IF NOT EXISTS tutor_message (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  llm_provider VARCHAR(64),
  llm_model VARCHAR(128),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_tutor_message_role CHECK (role IN ('USER', 'ASSISTANT')),
  CONSTRAINT fk_tutor_message_session
    FOREIGN KEY (session_id) REFERENCES learning_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_tutor_message_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tutor_message_session_task_user_created
  ON tutor_message(session_id, task_id, user_id, created_at ASC, id ASC);
