-- Sprint 4: structured message metadata + runtime evidence snapshot

ALTER TABLE task_message
    ADD COLUMN IF NOT EXISTS metadata_json TEXT;

ALTER TABLE task_execution_runtime
    ADD COLUMN IF NOT EXISTS guidance_phase VARCHAR(32);
ALTER TABLE task_execution_runtime
    ADD COLUMN IF NOT EXISTS evidence_snapshot_json TEXT;
