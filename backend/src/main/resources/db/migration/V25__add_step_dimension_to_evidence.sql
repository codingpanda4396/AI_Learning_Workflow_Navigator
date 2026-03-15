ALTER TABLE evidence
  ADD COLUMN IF NOT EXISTS step_id BIGINT REFERENCES learning_step(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS step_index INT;

CREATE INDEX IF NOT EXISTS idx_evidence_step_created
  ON evidence(step_id, created_at DESC);
