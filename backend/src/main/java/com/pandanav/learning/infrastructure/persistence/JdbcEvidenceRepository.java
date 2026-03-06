package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcEvidenceRepository implements EvidenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Evidence save(Evidence evidence) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<Evidence> findById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public List<Evidence> findByTaskId(Long taskId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
