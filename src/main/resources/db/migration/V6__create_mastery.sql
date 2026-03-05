-- Mastery
CREATE TABLE IF NOT EXISTS mastery (
  user_id VARCHAR(64) NOT NULL,
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE CASCADE,
  mastery_value NUMERIC(4,3) NOT NULL DEFAULT 0.000,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, node_id)
);

CREATE INDEX IF NOT EXISTS idx_mastery_user_updated ON mastery(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_mastery_node ON mastery(node_id);
