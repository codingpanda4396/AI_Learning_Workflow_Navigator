CREATE TABLE IF NOT EXISTS plan_instance (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  source_plan_id BIGINT REFERENCES learning_plan(id) ON DELETE SET NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_plan_instance_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_plan_instance_session_created
  ON plan_instance(session_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plan_instance_session_active
  ON plan_instance(session_id)
  WHERE status = 'ACTIVE';

ALTER TABLE learning_session
  ADD COLUMN IF NOT EXISTS current_plan_instance_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_learning_session_current_plan_instance'
  ) THEN
    ALTER TABLE learning_session
      ADD CONSTRAINT fk_learning_session_current_plan_instance
      FOREIGN KEY (current_plan_instance_id) REFERENCES plan_instance(id) ON DELETE SET NULL;
  END IF;
END $$;
