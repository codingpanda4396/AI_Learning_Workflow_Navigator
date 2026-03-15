package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
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
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO evidence (task_id, step_id, step_index, evidence_type, content_json)
                    VALUES (?, ?, ?, ?, CAST(? AS jsonb))
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, evidence.getTaskId());
            if (evidence.getStepId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, evidence.getStepId());
            }
            if (evidence.getStepIndex() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, evidence.getStepIndex());
            }
            ps.setString(4, evidence.getEvidenceType());
            ps.setString(5, evidence.getContentJson());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            evidence.setId(key.longValue());
        }
        return evidence;
    }

    @Override
    public Optional<Evidence> findById(Long id) {
        try {
            Evidence evidence = jdbcTemplate.queryForObject(
                """
                    SELECT id, task_id, step_id, step_index, evidence_type, content_json, created_at
                    FROM evidence
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapEvidence(rs),
                id
            );
            return Optional.ofNullable(evidence);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Evidence> findByTaskId(Long taskId) {
        return jdbcTemplate.query(
            """
                SELECT id, task_id, step_id, step_index, evidence_type, content_json, created_at
                FROM evidence
                WHERE task_id = ?
                ORDER BY created_at DESC, id DESC
                """,
            (rs, rowNum) -> mapEvidence(rs),
            taskId
        );
    }

    @Override
    public List<Evidence> findByTaskIdAndStepId(Long taskId, Long stepId) {
        return jdbcTemplate.query(
            """
                SELECT id, task_id, step_id, step_index, evidence_type, content_json, created_at
                FROM evidence
                WHERE task_id = ?
                  AND step_id = ?
                ORDER BY created_at DESC, id DESC
                """,
            (rs, rowNum) -> mapEvidence(rs),
            taskId,
            stepId
        );
    }

    private Evidence mapEvidence(java.sql.ResultSet rs) throws java.sql.SQLException {
        Evidence evidence = new Evidence();
        evidence.setId(rs.getLong("id"));
        evidence.setTaskId(rs.getLong("task_id"));
        Long stepId = rs.getObject("step_id", Long.class);
        evidence.setStepId(stepId);
        Integer stepIndex = rs.getObject("step_index", Integer.class);
        evidence.setStepIndex(stepIndex);
        evidence.setEvidenceType(rs.getString("evidence_type"));
        evidence.setContentJson(rs.getString("content_json"));
        evidence.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return evidence;
    }
}


