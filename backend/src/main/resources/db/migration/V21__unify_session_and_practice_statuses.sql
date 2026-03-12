UPDATE learning_session
SET status = CASE UPPER(status)
    WHEN 'ACTIVE' THEN 'LEARNING'
    WHEN 'GENERATING' THEN 'PRACTICING'
    WHEN 'QUIZ_READY' THEN 'PRACTICING'
    WHEN 'ANSWERED' THEN 'PRACTICING'
    WHEN 'FEEDBACK_READY' THEN 'REPORT_READY'
    WHEN 'REVIEWING' THEN 'LEARNING'
    WHEN 'NEXT_ROUND' THEN 'PRACTICING'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL;

ALTER TABLE learning_session
  DROP CONSTRAINT IF EXISTS ck_learning_session_status;

ALTER TABLE learning_session
  ADD CONSTRAINT ck_learning_session_status
  CHECK (status IN ('ANALYZING', 'PLANNING', 'LEARNING', 'PRACTICING', 'REPORT_READY', 'COMPLETED', 'FAILED'));

UPDATE practice_item
SET status = CASE UPPER(status)
    WHEN 'GENERATED' THEN 'READY'
    WHEN 'ACTIVE' THEN 'READY'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL;

ALTER TABLE practice_item
  DROP CONSTRAINT IF EXISTS ck_practice_item_status;

ALTER TABLE practice_item
  ADD CONSTRAINT ck_practice_item_status
  CHECK (status IN ('READY', 'ANSWERED', 'ARCHIVED'));

UPDATE practice_quiz
SET status = CASE UPPER(status)
    WHEN 'QUIZ_READY' THEN 'READY'
    WHEN 'ANSWERED' THEN 'ANSWERING'
    WHEN 'FEEDBACK_READY' THEN 'REPORT_READY'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL;

ALTER TABLE practice_quiz
  DROP CONSTRAINT IF EXISTS ck_practice_quiz_status;

ALTER TABLE practice_quiz
  ADD CONSTRAINT ck_practice_quiz_status
  CHECK (status IN ('GENERATING', 'READY', 'ANSWERING', 'REVIEWING', 'REPORT_READY', 'NEXT_ROUND', 'FAILED'));

UPDATE task_attempt
SET status = 'FAILED'::run_status
WHERE status = 'CANCELLED'::run_status;

UPDATE practice_quiz
SET generation_status = 'FAILED'::run_status
WHERE generation_status = 'CANCELLED'::run_status;

UPDATE practice_submission
SET judging_status = 'FAILED'::run_status
WHERE judging_status = 'CANCELLED'::run_status;

UPDATE practice_feedback_report
SET report_status = 'FAILED'::run_status
WHERE report_status = 'CANCELLED'::run_status;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'run_status_v2') THEN
    DROP TYPE run_status_v2;
  END IF;
END $$;

CREATE TYPE run_status_v2 AS ENUM ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED');

ALTER TABLE task_attempt
  ALTER COLUMN status DROP DEFAULT,
  ALTER COLUMN status TYPE run_status_v2 USING status::text::run_status_v2,
  ALTER COLUMN status SET DEFAULT 'PENDING'::run_status_v2;

ALTER TABLE practice_quiz
  ALTER COLUMN generation_status DROP DEFAULT,
  ALTER COLUMN generation_status TYPE run_status_v2 USING generation_status::text::run_status_v2,
  ALTER COLUMN generation_status SET DEFAULT 'PENDING'::run_status_v2;

ALTER TABLE practice_submission
  ALTER COLUMN judging_status DROP DEFAULT,
  ALTER COLUMN judging_status TYPE run_status_v2 USING judging_status::text::run_status_v2,
  ALTER COLUMN judging_status SET DEFAULT 'SUCCEEDED'::run_status_v2;

ALTER TABLE practice_feedback_report
  ALTER COLUMN report_status DROP DEFAULT,
  ALTER COLUMN report_status TYPE run_status_v2 USING report_status::text::run_status_v2,
  ALTER COLUMN report_status SET DEFAULT 'SUCCEEDED'::run_status_v2;

DROP TYPE run_status;
ALTER TYPE run_status_v2 RENAME TO run_status;
