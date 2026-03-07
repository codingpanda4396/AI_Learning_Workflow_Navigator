-- Evidence records for evaluator outputs and decision traces
CREATE TABLE IF NOT EXISTS evidence (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  evidence_type VARCHAR(64) NOT NULL,
  content_json JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evidence_task_created ON evidence(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evidence_type_created ON evidence(evidence_type, created_at DESC);
