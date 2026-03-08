ALTER TABLE task_attempt
ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(64),
ADD COLUMN IF NOT EXISTS llm_model VARCHAR(128),
ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(32),
ADD COLUMN IF NOT EXISTS token_input INT,
ADD COLUMN IF NOT EXISTS token_output INT,
ADD COLUMN IF NOT EXISTS latency_ms INT,
ADD COLUMN IF NOT EXISTS generation_mode VARCHAR(32);

CREATE TABLE IF NOT EXISTS llm_call_log (
  id BIGSERIAL PRIMARY KEY,
  task_attempt_id BIGINT REFERENCES task_attempt(id) ON DELETE SET NULL,
  biz_type VARCHAR(32) NOT NULL,
  provider VARCHAR(64),
  model VARCHAR(128),
  prompt_template_key VARCHAR(64),
  prompt_version VARCHAR(32),
  request_payload JSONB,
  response_payload JSONB,
  parsed_json JSONB,
  status VARCHAR(24) NOT NULL,
  latency_ms INT,
  token_input INT,
  token_output INT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_llm_call_log_attempt ON llm_call_log(task_attempt_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_llm_call_log_biz ON llm_call_log(biz_type, created_at DESC);

