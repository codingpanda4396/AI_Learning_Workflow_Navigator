package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.repository.LearningSessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcLearningSessionRepository implements LearningSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearningSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningSession save(LearningSession session) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<LearningSession> findById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<LearningSession> findByUserIdAndChapterId(String userId, String chapterId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
