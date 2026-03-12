CREATE TABLE IF NOT EXISTS diagnosis_session (
  id BIGSERIAL PRIMARY KEY,
  learning_session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  status VARCHAR(32) NOT NULL,
  generated_questions_json JSONB NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS diagnosis_answer (
  id BIGSERIAL PRIMARY KEY,
  diagnosis_session_id BIGINT NOT NULL REFERENCES diagnosis_session(id) ON DELETE CASCADE,
  question_id VARCHAR(64) NOT NULL,
  dimension VARCHAR(32) NOT NULL,
  answer_type VARCHAR(32) NOT NULL,
  answer_value_json JSONB NOT NULL,
  raw_text TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS capability_profile (
  id BIGSERIAL PRIMARY KEY,
  learning_session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  source_diagnosis_id BIGINT NOT NULL REFERENCES diagnosis_session(id) ON DELETE CASCADE,
  current_level VARCHAR(32) NOT NULL,
  strengths_json JSONB NOT NULL,
  weaknesses_json JSONB NOT NULL,
  preferences_json JSONB NOT NULL,
  constraints_json JSONB NOT NULL,
  summary_text TEXT,
  version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_diagnosis_session_learning_session
  ON diagnosis_session(learning_session_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_diagnosis_session_source_user
  ON diagnosis_session(user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_diagnosis_answer_session
  ON diagnosis_answer(diagnosis_session_id, created_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_capability_profile_learning_session
  ON capability_profile(learning_session_id, version DESC, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_capability_profile_source_diagnosis
  ON capability_profile(source_diagnosis_id);
