ALTER TABLE practice_quiz
  ADD COLUMN IF NOT EXISTS generation_status run_status NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS generation_started_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS generation_finished_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS token_input INT,
  ADD COLUMN IF NOT EXISTS token_output INT,
  ADD COLUMN IF NOT EXISTS latency_ms INT,
  ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(64);

UPDATE practice_quiz
SET generation_status = CASE
      WHEN status = 'FAILED' THEN 'FAILED'::run_status
      WHEN status = 'GENERATING' THEN 'RUNNING'::run_status
      ELSE 'SUCCEEDED'::run_status
    END,
    generation_started_at = COALESCE(generation_started_at, created_at),
    generation_finished_at = CASE
      WHEN status IN ('QUIZ_READY', 'ANSWERED', 'FEEDBACK_READY', 'REVIEWING', 'NEXT_ROUND', 'FAILED')
        THEN COALESCE(generation_finished_at, updated_at, created_at)
      ELSE generation_finished_at
    END
WHERE generation_started_at IS NULL
   OR generation_finished_at IS NULL
   OR generation_status = 'PENDING'::run_status;

ALTER TABLE practice_submission
  ADD COLUMN IF NOT EXISTS judging_status run_status NOT NULL DEFAULT 'SUCCEEDED',
  ADD COLUMN IF NOT EXISTS judging_started_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS judging_finished_at TIMESTAMPTZ;

UPDATE practice_submission
SET judging_status = CASE
      WHEN score IS NULL AND feedback IS NULL THEN 'PENDING'::run_status
      ELSE 'SUCCEEDED'::run_status
    END,
    judging_started_at = COALESCE(judging_started_at, submitted_at),
    judging_finished_at = CASE
      WHEN score IS NOT NULL OR feedback IS NOT NULL
        THEN COALESCE(judging_finished_at, submitted_at)
      ELSE judging_finished_at
    END
WHERE judging_started_at IS NULL
   OR judging_finished_at IS NULL;

ALTER TABLE practice_feedback_report
  ADD COLUMN IF NOT EXISTS report_status run_status NOT NULL DEFAULT 'SUCCEEDED',
  ADD COLUMN IF NOT EXISTS report_started_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS report_finished_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS selected_action VARCHAR(32),
  ADD COLUMN IF NOT EXISTS action_selected_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS token_input INT,
  ADD COLUMN IF NOT EXISTS token_output INT,
  ADD COLUMN IF NOT EXISTS latency_ms INT,
  ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(64);

UPDATE practice_feedback_report
SET report_status = 'SUCCEEDED'::run_status,
    report_started_at = COALESCE(report_started_at, created_at),
    report_finished_at = COALESCE(report_finished_at, created_at)
WHERE report_started_at IS NULL
   OR report_finished_at IS NULL
   OR report_status <> 'SUCCEEDED'::run_status;

DO $$
BEGIN
  ALTER TABLE practice_feedback_report
    ADD CONSTRAINT ck_practice_feedback_selected_action
    CHECK (selected_action IS NULL OR selected_action IN ('REVIEW', 'NEXT_ROUND'));
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_practice_quiz_generation_status_created
  ON practice_quiz(generation_status, created_at ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_practice_quiz_session_task_generation
  ON practice_quiz(session_id, task_id, generation_status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_submission_quiz_judging_submitted
  ON practice_submission(quiz_id, judging_status, submitted_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_feedback_quiz_report_status
  ON practice_feedback_report(quiz_id, report_status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_feedback_session_task_created
  ON practice_feedback_report(session_id, task_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_practice_feedback_selected_action
  ON practice_feedback_report(selected_action, action_selected_at DESC)
  WHERE selected_action IS NOT NULL;
