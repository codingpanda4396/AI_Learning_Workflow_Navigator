-- Add sequences for tables that previously used fixed IDs, to prevent duplicate key errors.

-- learning_session
CREATE SEQUENCE IF NOT EXISTS learning_session_id_seq;
ALTER TABLE learning_session ALTER COLUMN id SET DEFAULT nextval('learning_session_id_seq');
SELECT setval('learning_session_id_seq', COALESCE((SELECT MAX(id) FROM learning_session), 1));

-- diagnosis_session
CREATE SEQUENCE IF NOT EXISTS diagnosis_session_id_seq;
ALTER TABLE diagnosis_session ALTER COLUMN id SET DEFAULT nextval('diagnosis_session_id_seq');
SELECT setval('diagnosis_session_id_seq', COALESCE((SELECT MAX(id) FROM diagnosis_session), 1));

-- learning_plan
CREATE SEQUENCE IF NOT EXISTS learning_plan_id_seq;
ALTER TABLE learning_plan ALTER COLUMN id SET DEFAULT nextval('learning_plan_id_seq');
SELECT setval('learning_plan_id_seq', COALESCE((SELECT MAX(id) FROM learning_plan), 1));
