package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.model.DiagnosisSession;
import com.pandanav.learning.domain.repository.DiagnosisSessionRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class JdbcDiagnosisSessionRepository implements DiagnosisSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDiagnosisSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DiagnosisSession save(DiagnosisSession session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO diagnosis_session
                      (learning_session_id, user_id, status, generated_questions_json, started_at)
                    VALUES (?, ?, ?, ?::jsonb, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, session.getLearningSessionId());
            ps.setObject(2, session.getUserPk());
            ps.setString(3, session.getStatus().name());
            ps.setString(4, session.getGeneratedQuestionsJson());
            ps.setObject(5, session.getStartedAt());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            session.setId(key.longValue());
        }
        return session;
    }

    @Override
    public Optional<DiagnosisSession> findById(Long id) {
        return findSingle("WHERE id = ?", id);
    }

    @Override
    public Optional<DiagnosisSession> findByIdAndUserPk(Long id, Long userPk) {
        return findSingle("WHERE id = ? AND user_id = ?", id, userPk);
    }

    @Override
    public void updateStatus(Long id, DiagnosisStatus status, OffsetDateTime completedAt) {
        jdbcTemplate.update(
            """
                UPDATE diagnosis_session
                SET status = ?,
                    completed_at = COALESCE(?, completed_at),
                    updated_at = now()
                WHERE id = ?
                """,
            status.name(),
            completedAt,
            id
        );
    }

    private Optional<DiagnosisSession> findSingle(String whereClause, Object... args) {
        try {
            DiagnosisSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, learning_session_id, user_id, status, generated_questions_json,
                           started_at, completed_at, created_at, updated_at
                    FROM diagnosis_session
                    %s
                    """.formatted(whereClause),
                (rs, rowNum) -> {
                    DiagnosisSession item = new DiagnosisSession();
                    item.setId(rs.getLong("id"));
                    item.setLearningSessionId(rs.getLong("learning_session_id"));
                    item.setUserPk(rs.getObject("user_id", Long.class));
                    item.setStatus(DiagnosisStatus.valueOf(rs.getString("status")));
                    item.setGeneratedQuestionsJson(rs.getString("generated_questions_json"));
                    item.setStartedAt(rs.getObject("started_at", OffsetDateTime.class));
                    item.setCompletedAt(rs.getObject("completed_at", OffsetDateTime.class));
                    item.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return item;
                },
                args
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
