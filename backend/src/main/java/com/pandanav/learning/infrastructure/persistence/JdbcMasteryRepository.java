package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMasteryRepository implements MasteryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasteryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Mastery save(Mastery mastery) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public List<Mastery> findByUserId(String userId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
