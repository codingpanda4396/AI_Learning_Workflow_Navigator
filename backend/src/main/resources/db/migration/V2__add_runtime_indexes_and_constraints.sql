-- Indexes and constraints for Sprint 2 runtime tables.

-- learning_session fast lookup by goal and status
CREATE INDEX IF NOT EXISTS idx_learning_session_goal
    ON learning_session (goal_id);

CREATE INDEX IF NOT EXISTS idx_learning_session_status
    ON learning_session (status);

-- diagnosis_answer: prevent duplicate answers per question in a session
CREATE UNIQUE INDEX IF NOT EXISTS uq_diagnosis_answer_session_question
    ON diagnosis_answer (diagnosis_session_id, question_id);

CREATE INDEX IF NOT EXISTS idx_diagnosis_answer_session
    ON diagnosis_answer (diagnosis_session_id);

-- learner_profile_snapshot: one main snapshot per diagnosis/session pair (soft uniqueness by convention)
CREATE INDEX IF NOT EXISTS idx_learner_profile_session
    ON learner_profile_snapshot (session_id);

-- learning_plan: one current plan per session
CREATE UNIQUE INDEX IF NOT EXISTS uq_learning_plan_session
    ON learning_plan (session_id);

CREATE INDEX IF NOT EXISTS idx_learning_plan_status
    ON learning_plan (status);

-- session_task: task ordering within a session
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_task_order
    ON session_task (session_id, order_index);

CREATE INDEX IF NOT EXISTS idx_session_task_session
    ON session_task (session_id);

CREATE INDEX IF NOT EXISTS idx_session_task_status
    ON session_task (status);

-- task_interaction: queries by session/task
CREATE INDEX IF NOT EXISTS idx_task_interaction_session
    ON task_interaction (session_id);

CREATE INDEX IF NOT EXISTS idx_task_interaction_task
    ON task_interaction (task_id);

-- task_completion: at most one completion per task
CREATE UNIQUE INDEX IF NOT EXISTS uq_task_completion_task
    ON task_completion (task_id);

CREATE INDEX IF NOT EXISTS idx_task_completion_session
    ON task_completion (session_id);

