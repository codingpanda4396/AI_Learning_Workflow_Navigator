CREATE TABLE IF NOT EXISTS learning_plan (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  goal_id VARCHAR(64) NOT NULL,
  diagnosis_id VARCHAR(64) NOT NULL,
  session_id BIGINT REFERENCES learning_session(id) ON DELETE SET NULL,
  status VARCHAR(16) NOT NULL,
  summary_json JSONB NOT NULL,
  reasons_json JSONB NOT NULL,
  focuses_json JSONB NOT NULL,
  path_preview_json JSONB NOT NULL,
  task_preview_json JSONB NOT NULL,
  adjustments_json JSONB NOT NULL,
  planning_context_json JSONB NOT NULL,
  llm_trace_id VARCHAR(128),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_learning_plan_user_created ON learning_plan(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_learning_plan_goal_diag ON learning_plan(goal_id, diagnosis_id, created_at DESC);
