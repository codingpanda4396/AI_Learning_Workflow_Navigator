-- Learning session
CREATE TABLE IF NOT EXISTS learning_session (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64) NOT NULL,
  chapter_id VARCHAR(64) NOT NULL,
  goal_text TEXT,
  current_node_id BIGINT,
  current_stage task_stage,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, chapter_id)
);

CREATE INDEX IF NOT EXISTS idx_session_user ON learning_session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_course_chapter ON learning_session(course_id, chapter_id);
