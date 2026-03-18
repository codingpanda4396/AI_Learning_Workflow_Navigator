-- Add sequence for learning_goal.id to support auto-generated unique IDs.
-- Required for PostgreSQL; prevents duplicate key errors when creating multiple goals.

CREATE SEQUENCE IF NOT EXISTS learning_goal_id_seq;

ALTER TABLE learning_goal
    ALTER COLUMN id SET DEFAULT nextval('learning_goal_id_seq');
-- Note: sequence sync is intentionally omitted for cross-DB compatibility in tests.
