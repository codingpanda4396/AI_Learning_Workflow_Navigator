CREATE TABLE IF NOT EXISTS learner_feature_signal (
  id BIGSERIAL PRIMARY KEY,
  diagnosis_session_id BIGINT NOT NULL REFERENCES diagnosis_session(id) ON DELETE CASCADE,
  learning_session_id BIGINT REFERENCES learning_session(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  question_id VARCHAR(64) NOT NULL,
  feature_key VARCHAR(128) NOT NULL,
  feature_value VARCHAR(128) NOT NULL,
  score_delta NUMERIC(6, 3) NOT NULL DEFAULT 0,
  confidence NUMERIC(5, 4) NOT NULL DEFAULT 0,
  evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
  source VARCHAR(64) NOT NULL DEFAULT 'RULE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS learner_profile_snapshot (
  id BIGSERIAL PRIMARY KEY,
  diagnosis_session_id BIGINT NOT NULL UNIQUE REFERENCES diagnosis_session(id) ON DELETE CASCADE,
  learning_session_id BIGINT REFERENCES learning_session(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  profile_version INT NOT NULL DEFAULT 1,
  feature_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  strategy_hints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  explanations_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_learner_feature_signal_diag_feature
  ON learner_feature_signal(diagnosis_session_id, feature_key);

CREATE INDEX IF NOT EXISTS idx_learner_feature_signal_learning_session
  ON learner_feature_signal(learning_session_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_learner_profile_snapshot_learning_session_created
  ON learner_profile_snapshot(learning_session_id, created_at DESC, id DESC);
