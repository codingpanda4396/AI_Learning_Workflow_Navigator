CREATE TABLE IF NOT EXISTS practice_quiz (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'GENERATING',
  question_count INT NOT NULL DEFAULT 0,
  answered_count INT NOT NULL DEFAULT 0,
  generation_source VARCHAR(16),
  prompt_version VARCHAR(64),
  failure_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_practice_quiz_session
    FOREIGN KEY (session_id) REFERENCES learning_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_quiz_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_quiz_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_quiz_node
    FOREIGN KEY (node_id) REFERENCES concept_node(id) ON DELETE RESTRICT,
  CONSTRAINT ck_practice_quiz_status
    CHECK (status IN ('GENERATING', 'QUIZ_READY', 'ANSWERED', 'FEEDBACK_READY', 'REVIEWING', 'NEXT_ROUND', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_practice_quiz_session_task_created
  ON practice_quiz(session_id, task_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS practice_feedback_report (
  id BIGSERIAL PRIMARY KEY,
  quiz_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  diagnosis_summary TEXT NOT NULL,
  strengths_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  weaknesses_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  review_focus_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  next_round_advice TEXT NOT NULL,
  recommended_action VARCHAR(32) NOT NULL,
  source VARCHAR(16) NOT NULL,
  prompt_version VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_practice_feedback_quiz
    FOREIGN KEY (quiz_id) REFERENCES practice_quiz(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_feedback_session
    FOREIGN KEY (session_id) REFERENCES learning_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_feedback_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_practice_feedback_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_practice_feedback_action
    CHECK (recommended_action IN ('REVIEW', 'NEXT_ROUND')),
  CONSTRAINT ck_practice_feedback_source
    CHECK (source IN ('RULE', 'LLM'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_feedback_quiz
  ON practice_feedback_report(quiz_id);

ALTER TABLE practice_item
  ADD COLUMN IF NOT EXISTS quiz_id BIGINT;

ALTER TABLE practice_item
  ADD CONSTRAINT fk_practice_item_quiz
  FOREIGN KEY (quiz_id) REFERENCES practice_quiz(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_practice_item_quiz_created
  ON practice_item(quiz_id, created_at ASC, id ASC);

ALTER TABLE practice_submission
  ADD COLUMN IF NOT EXISTS quiz_id BIGINT;

ALTER TABLE practice_submission
  ADD CONSTRAINT fk_practice_submission_quiz
  FOREIGN KEY (quiz_id) REFERENCES practice_quiz(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_practice_submission_quiz_submitted
  ON practice_submission(quiz_id, submitted_at DESC, id DESC);
