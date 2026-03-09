CREATE TABLE IF NOT EXISTS practice_item (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  stage task_stage NOT NULL DEFAULT 'TRAINING',
  question_type VARCHAR(32) NOT NULL,
  stem TEXT NOT NULL,
  options_json JSONB,
  standard_answer TEXT,
  explanation TEXT,
  difficulty VARCHAR(16),
  source VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
  prompt_version VARCHAR(64),
  token_input INT,
  token_output INT,
  latency_ms INT,
  trace_id VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_practice_item_session
    FOREIGN KEY (session_id) REFERENCES learning_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_item_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_item_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_item_node
    FOREIGN KEY (node_id) REFERENCES concept_node(id) ON DELETE RESTRICT,
  CONSTRAINT ck_practice_item_source
    CHECK (source IN ('RULE', 'LLM', 'MANUAL')),
  CONSTRAINT ck_practice_item_status
    CHECK (status IN ('GENERATED', 'ACTIVE', 'ANSWERED', 'ARCHIVED')),
  CONSTRAINT ck_practice_item_question_type
    CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'FILL_BLANK', 'SHORT_ANSWER')),
  CONSTRAINT ck_practice_item_stage
    CHECK (stage = 'TRAINING')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_item_identity
  ON practice_item(id, session_id, task_id, user_id);

CREATE INDEX IF NOT EXISTS idx_practice_item_session_task_created
  ON practice_item(session_id, task_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_item_user_node_created
  ON practice_item(user_id, node_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS practice_submission (
  id BIGSERIAL PRIMARY KEY,
  practice_item_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  user_answer TEXT,
  score INT,
  is_correct BOOLEAN,
  error_tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  feedback TEXT,
  judge_mode VARCHAR(32) NOT NULL DEFAULT 'RULE',
  prompt_version VARCHAR(64),
  token_input INT,
  token_output INT,
  latency_ms INT,
  trace_id VARCHAR(64),
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_practice_submission_item
    FOREIGN KEY (practice_item_id) REFERENCES practice_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_submission_session
    FOREIGN KEY (session_id) REFERENCES learning_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_submission_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_submission_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_submission_item_scope
    FOREIGN KEY (practice_item_id, session_id, task_id, user_id)
      REFERENCES practice_item(id, session_id, task_id, user_id)
      ON DELETE CASCADE,
  CONSTRAINT ck_practice_submission_judge_mode
    CHECK (judge_mode IN ('RULE', 'LLM', 'MANUAL'))
);

CREATE INDEX IF NOT EXISTS idx_practice_submission_session_task_submitted
  ON practice_submission(session_id, task_id, submitted_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_submission_item_submitted
  ON practice_submission(practice_item_id, submitted_at DESC, id DESC);
