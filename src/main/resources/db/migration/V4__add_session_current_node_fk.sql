-- Add foreign key to session.current_node_id after concept_node exists
ALTER TABLE learning_session
  ADD CONSTRAINT fk_session_current_node
  FOREIGN KEY (current_node_id) REFERENCES concept_node(id)
  DEFERRABLE INITIALLY DEFERRED;
