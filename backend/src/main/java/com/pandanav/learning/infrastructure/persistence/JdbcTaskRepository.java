package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningTask;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTaskRepository implements TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningTask save(LearningTask task) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public List<LearningTask> saveAll(List<LearningTask> tasks) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Optional<LearningTask> findById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public List<LearningTask> findBySessionId(Long sessionId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
