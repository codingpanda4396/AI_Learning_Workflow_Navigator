CREATE TABLE IF NOT EXISTS node_mastery (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  session_id BIGINT REFERENCES learning_session(id) ON DELETE SET NULL,
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE CASCADE,
  mastery_score NUMERIC(5,2) NOT NULL DEFAULT 0,
  training_accuracy NUMERIC(5,2) NOT NULL DEFAULT 0,
  recent_error_tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  latest_evaluation_score INT,
  attempt_count INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uk_node_mastery_user_node UNIQUE (user_id, node_id),
  CONSTRAINT ck_node_mastery_score_range CHECK (mastery_score >= 0 AND mastery_score <= 100),
  CONSTRAINT ck_node_mastery_accuracy_range CHECK (training_accuracy >= 0 AND training_accuracy <= 100),
  CONSTRAINT ck_node_mastery_attempt_non_negative CHECK (attempt_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_node_mastery_user_updated
  ON node_mastery(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_node_mastery_session_updated
  ON node_mastery(session_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_node_mastery_user_mastery
  ON node_mastery(user_id, mastery_score ASC, updated_at DESC);
