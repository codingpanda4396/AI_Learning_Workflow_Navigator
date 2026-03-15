CREATE TABLE IF NOT EXISTS learning_step (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  stage task_stage NOT NULL,
  step_type VARCHAR(64) NOT NULL,
  step_order INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  objective TEXT NOT NULL,
  completion_rule JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_learning_step_status CHECK (status IN ('TODO', 'ACTIVE', 'DONE', 'FAILED', 'SKIPPED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_learning_step_task_order
  ON learning_step(task_id, step_order);

CREATE INDEX IF NOT EXISTS idx_learning_step_task_status
  ON learning_step(task_id, status);

