package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Stage;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcLearningSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearningSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningSession save(LearningSession session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO learning_session (user_id, course_id, chapter_id, goal_text, current_node_id, current_stage)
                    VALUES (?, ?, ?, ?, ?, ?::task_stage)
                    """,
                new String[]{"id"}
            );
            ps.setString(1, session.getUserId());
            ps.setString(2, session.getCourseId());
            ps.setString(3, session.getChapterId());
            ps.setString(4, session.getGoalText());
            ps.setLong(5, session.getCurrentNodeId());
            ps.setString(6, session.getCurrentStage().name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            session.setId(key.longValue());
        }
        return session;
    }

    @Override
    public Optional<LearningSession> findById(Long id) {
        try {
            LearningSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, course_id, chapter_id, goal_text, current_node_id, current_stage, created_at, updated_at
                    FROM learning_session
                    WHERE id = ?
                    """,
                (rs, rowNum) -> {
                    LearningSession item = new LearningSession();
                    item.setId(rs.getLong("id"));
                    item.setUserId(rs.getString("user_id"));
                    item.setCourseId(rs.getString("course_id"));
                    item.setChapterId(rs.getString("chapter_id"));
                    item.setGoalText(rs.getString("goal_text"));
                    item.setCurrentNodeId(rs.getLong("current_node_id"));
                    String stage = rs.getString("current_stage");
                    item.setCurrentStage(stage == null ? null : Stage.valueOf(stage));
                    item.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    return item;
                },
                id
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
