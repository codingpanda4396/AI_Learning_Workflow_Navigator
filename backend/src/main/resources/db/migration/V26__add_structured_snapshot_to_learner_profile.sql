-- 诊断结构化画像快照落库，供规划页消费
ALTER TABLE learner_profile_snapshot
  ADD COLUMN IF NOT EXISTS structured_snapshot_json JSONB DEFAULT NULL;

COMMENT ON COLUMN learner_profile_snapshot.structured_snapshot_json IS 'v1 结构化画像：profileVersion, foundationLevel, primaryBlocker, planHints, summary 等';
