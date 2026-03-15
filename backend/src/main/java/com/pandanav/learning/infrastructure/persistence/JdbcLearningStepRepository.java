package com.pandanav.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.CompletionRule;
import com.pandanav.learning.domain.model.LearningStep;
import com.pandanav.learning.domain.repository.LearningStepRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcLearningStepRepository implements LearningStepRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcLearningStepRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LearningStep save(LearningStep step) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO learning_step (task_id, stage, step_type, step_order, status, objective, completion_rule)
                    VALUES (?, ?::task_stage, ?, ?, ?, ?, CAST(? AS jsonb))
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, step.getTaskId());
            ps.setString(2, step.getStage().name());
            ps.setString(3, step.getType());
            ps.setInt(4, step.getStepOrder());
            ps.setString(5, step.getStatus().name());
            ps.setString(6, step.getObjective());
            ps.setString(7, writeJson(step.getCompletionRule()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            step.setId(key.longValue());
        }
        return step;
    }

    @Override
    public List<LearningStep> saveAll(List<LearningStep> steps) {
        return steps.stream().map(this::save).toList();
    }

    @Override
    public List<LearningStep> findByTaskIdOrderByStepOrder(Long taskId) {
        return jdbcTemplate.query(
            """
                SELECT id, task_id, stage, step_type, step_order, status, objective, completion_rule, created_at, updated_at
                FROM learning_step
                WHERE task_id = ?
                ORDER BY step_order ASC, id ASC
                """,
            (rs, rowNum) -> mapStep(rs),
            taskId
        );
    }

    @Override
    public Optional<LearningStep> findByIdAndTaskId(Long stepId, Long taskId) {
        try {
            LearningStep step = jdbcTemplate.queryForObject(
                """
                    SELECT id, task_id, stage, step_type, step_order, status, objective, completion_rule, created_at, updated_at
                    FROM learning_step
                    WHERE id = ?
                      AND task_id = ?
                    """,
                (rs, rowNum) -> mapStep(rs),
                stepId,
                taskId
            );
            return Optional.ofNullable(step);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void updateStatus(Long stepId, LearningStepStatus status) {
        jdbcTemplate.update(
            """
                UPDATE learning_step
                SET status = ?,
                    updated_at = now()
                WHERE id = ?
                """,
            status.name(),
            stepId
        );
    }

    private LearningStep mapStep(java.sql.ResultSet rs) throws java.sql.SQLException {
        LearningStep step = new LearningStep();
        step.setId(rs.getLong("id"));
        step.setTaskId(rs.getLong("task_id"));
        step.setStage(Stage.valueOf(rs.getString("stage")));
        step.setType(rs.getString("step_type"));
        step.setStepOrder(rs.getInt("step_order"));
        step.setStatus(LearningStepStatus.valueOf(rs.getString("status")));
        step.setObjective(rs.getString("objective"));
        step.setCompletionRule(readRule(rs.getString("completion_rule")));
        step.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        step.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return step;
    }

    private CompletionRule readRule(String json) {
        try {
            return objectMapper.readValue(json, CompletionRule.class);
        } catch (Exception ex) {
            throw new InternalServerException("Stored completion_rule is invalid.");
        }
    }

    private String writeJson(CompletionRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize completion rule.");
        }
    }
}

