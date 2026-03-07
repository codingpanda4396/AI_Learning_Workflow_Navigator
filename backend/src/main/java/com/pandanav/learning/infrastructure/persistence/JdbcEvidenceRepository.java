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
                    INSERT INTO evidence (task_id, evidence_type, content_json)
                    VALUES (?, ?, CAST(? AS jsonb))
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, evidence.getTaskId());
            ps.setString(2, evidence.getEvidenceType());
            ps.setString(3, evidence.getContentJson());
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
                    SELECT id, task_id, evidence_type, content_json, created_at
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
                SELECT id, task_id, evidence_type, content_json, created_at
                FROM evidence
                WHERE task_id = ?
                ORDER BY created_at DESC, id DESC
                """,
            (rs, rowNum) -> mapEvidence(rs),
            taskId
        );
    }

    private Evidence mapEvidence(java.sql.ResultSet rs) throws java.sql.SQLException {
        Evidence evidence = new Evidence();
        evidence.setId(rs.getLong("id"));
        evidence.setTaskId(rs.getLong("task_id"));
        evidence.setEvidenceType(rs.getString("evidence_type"));
        evidence.setContentJson(rs.getString("content_json"));
        evidence.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return evidence;
    }
}


