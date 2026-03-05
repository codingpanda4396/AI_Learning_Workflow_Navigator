-- Concept node and prerequisites
CREATE TABLE IF NOT EXISTS concept_node (
  id BIGSERIAL PRIMARY KEY,
  chapter_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  outline TEXT,
  order_no INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (chapter_id, name)
);

CREATE INDEX IF NOT EXISTS idx_concept_node_chapter_order ON concept_node(chapter_id, order_no);

CREATE TABLE IF NOT EXISTS concept_prerequisite (
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE CASCADE,
  prereq_node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE RESTRICT,
  PRIMARY KEY (node_id, prereq_node_id),
  CHECK (node_id <> prereq_node_id)
);

CREATE INDEX IF NOT EXISTS idx_prereq_prereq ON concept_prerequisite(prereq_node_id);
