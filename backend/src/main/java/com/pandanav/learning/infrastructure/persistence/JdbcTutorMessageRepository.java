package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.TutorMessageRole;
import com.pandanav.learning.domain.model.TutorMessage;
import com.pandanav.learning.domain.repository.TutorMessageRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class JdbcTutorMessageRepository implements TutorMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTutorMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TutorMessage> findBySessionIdAndTaskIdAndUserId(Long sessionId, Long taskId, Long userId) {
        return jdbcTemplate.query(
            """
                SELECT id, session_id, task_id, user_id, role, content, llm_provider, llm_model, created_at
                FROM tutor_message
                WHERE session_id = ?
                  AND task_id = ?
                  AND user_id = ?
                ORDER BY created_at ASC, id ASC
                """,
            (rs, rowNum) -> mapMessage(rs),
            sessionId,
            taskId,
            userId
        );
    }

    @Override
    public List<TutorMessage> findRecentBySessionIdAndTaskIdAndUserId(Long sessionId, Long taskId, Long userId, int limit) {
        int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(
            """
                SELECT id, session_id, task_id, user_id, role, content, llm_provider, llm_model, created_at
                FROM (
                    SELECT id, session_id, task_id, user_id, role, content, llm_provider, llm_model, created_at
                    FROM tutor_message
                    WHERE session_id = ?
                      AND task_id = ?
                      AND user_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT ?
                ) recent
                ORDER BY created_at ASC, id ASC
                """,
            (rs, rowNum) -> mapMessage(rs),
            sessionId,
            taskId,
            userId,
            safeLimit
        );
    }

    @Override
    public TutorMessage save(TutorMessage message) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO tutor_message (session_id, task_id, user_id, role, content, llm_provider, llm_model)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, message.getSessionId());
            ps.setLong(2, message.getTaskId());
            ps.setLong(3, message.getUserId());
            ps.setString(4, message.getRole().name());
            ps.setString(5, message.getContent());
            ps.setString(6, message.getLlmProvider());
            ps.setString(7, message.getLlmModel());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            message.setId(key.longValue());
        }

        TutorMessage persisted = jdbcTemplate.queryForObject(
            """
                SELECT id, session_id, task_id, user_id, role, content, llm_provider, llm_model, created_at
                FROM tutor_message
                WHERE id = ?
                """,
            (rs, rowNum) -> mapMessage(rs),
            message.getId()
        );
        return persisted == null ? message : persisted;
    }

    private TutorMessage mapMessage(java.sql.ResultSet rs) throws java.sql.SQLException {
        TutorMessage message = new TutorMessage();
        message.setId(rs.getLong("id"));
        message.setSessionId(rs.getLong("session_id"));
        message.setTaskId(rs.getLong("task_id"));
        message.setUserId(rs.getLong("user_id"));
        message.setRole(TutorMessageRole.valueOf(rs.getString("role")));
        message.setContent(rs.getString("content"));
        message.setLlmProvider(rs.getString("llm_provider"));
        message.setLlmModel(rs.getString("llm_model"));
        message.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return message;
    }
}
