ALTER TABLE task_attempt
ADD COLUMN IF NOT EXISTS invocation_profile VARCHAR(32),
ADD COLUMN IF NOT EXISTS reasoning_tokens INT,
ADD COLUMN IF NOT EXISTS finish_reason VARCHAR(64),
ADD COLUMN IF NOT EXISTS timeout_flag BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS truncated_flag BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE llm_call_log
ADD COLUMN IF NOT EXISTS invocation_profile VARCHAR(32),
ADD COLUMN IF NOT EXISTS input_tokens INT,
ADD COLUMN IF NOT EXISTS output_tokens INT,
ADD COLUMN IF NOT EXISTS reasoning_tokens INT,
ADD COLUMN IF NOT EXISTS finish_reason VARCHAR(64),
ADD COLUMN IF NOT EXISTS timeout_flag BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS parse_success BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS schema_valid BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS truncated_flag BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE llm_call_log
SET input_tokens = COALESCE(input_tokens, token_input),
    output_tokens = COALESCE(output_tokens, token_output)
WHERE input_tokens IS NULL OR output_tokens IS NULL;

CREATE OR REPLACE VIEW llm_call_log_summary_vw AS
SELECT
    biz_type,
    COALESCE(invocation_profile, 'UNKNOWN') AS invocation_profile,
    COALESCE(provider, 'UNKNOWN') AS provider,
    COALESCE(model, 'UNKNOWN') AS model,
    COUNT(*) AS total_calls,
    ROUND(AVG(latency_ms)::numeric, 2) AS avg_latency_ms,
    ROUND(AVG(output_tokens)::numeric, 2) AS avg_output_tokens,
    ROUND(AVG(reasoning_tokens)::numeric, 2) AS avg_reasoning_tokens,
    SUM(CASE WHEN fallback_used THEN 1 ELSE 0 END) AS fallback_count,
    SUM(CASE WHEN NOT parse_success THEN 1 ELSE 0 END) AS parse_fail_count,
    SUM(CASE WHEN timeout_flag THEN 1 ELSE 0 END) AS timeout_count,
    SUM(CASE WHEN truncated_flag THEN 1 ELSE 0 END) AS truncated_count
FROM llm_call_log
GROUP BY biz_type, COALESCE(invocation_profile, 'UNKNOWN'), COALESCE(provider, 'UNKNOWN'), COALESCE(model, 'UNKNOWN');
