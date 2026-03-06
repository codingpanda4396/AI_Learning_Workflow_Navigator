package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcConceptNodeRepository implements ConceptNodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConceptNodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ConceptNode save(ConceptNode node) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<ConceptNode> findById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public List<ConceptNode> findByChapterId(String chapterId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
