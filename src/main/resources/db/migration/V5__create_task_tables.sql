-- Task and task attempts
CREATE TABLE IF NOT EXISTS task (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  stage task_stage NOT NULL,
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE RESTRICT,
  objective TEXT NOT NULL,
  input_json JSONB,
  expected_output_schema JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_task_session_stage_created ON task(session_id, stage, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_node ON task(node_id);

CREATE TABLE IF NOT EXISTS task_attempt (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  status run_status NOT NULL DEFAULT 'PENDING',
  run_input_json JSONB,
  output_json JSONB,
  score INT,
  error_tags JSONB NOT NULL DEFAULT '[]'::jsonb,
  feedback_json JSONB,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_attempt_task_created ON task_attempt(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_attempt_status_created ON task_attempt(status, created_at);
