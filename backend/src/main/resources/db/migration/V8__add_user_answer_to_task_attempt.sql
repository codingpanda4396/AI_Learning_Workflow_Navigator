ALTER TABLE task_attempt
ADD COLUMN IF NOT EXISTS user_answer TEXT;
