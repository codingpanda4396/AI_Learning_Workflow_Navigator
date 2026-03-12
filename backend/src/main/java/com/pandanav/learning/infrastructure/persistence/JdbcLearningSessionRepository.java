package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.SessionStatus;
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
                    INSERT INTO learning_session
                      (user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage, status, last_active_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?::task_stage, ?, now())
                    """,
                new String[]{"id"}
            );
            ps.setString(1, session.getUserId());
            ps.setObject(2, session.getUserPk());
            ps.setString(3, session.getCourseId());
            ps.setString(4, session.getChapterId());
            ps.setString(5, session.getGoalText());
            ps.setLong(6, session.getCurrentNodeId());
            ps.setString(7, session.getCurrentStage().name());
            ps.setString(8, session.getStatus() == null ? SessionStatus.ANALYZING.name() : session.getStatus().name());
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
                    SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                           status, completed_at, last_active_at, created_at, updated_at
                    FROM learning_session
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapSession(rs),
                id
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LearningSession> findLatestByUserId(String userId) {
        try {
            LearningSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                           status, completed_at, last_active_at, created_at, updated_at
                    FROM learning_session
                    WHERE user_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> mapSession(rs),
                userId
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LearningSession> findByIdAndUserPk(Long id, Long userPk) {
        try {
            LearningSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                           status, completed_at, last_active_at, created_at, updated_at
                    FROM learning_session
                    WHERE id = ? AND user_pk = ?
                    """,
                (rs, rowNum) -> mapSession(rs),
                id,
                userPk
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LearningSession> findLatestActiveByUserPk(Long userPk) {
        try {
            LearningSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                           status, completed_at, last_active_at, created_at, updated_at
                    FROM learning_session
                    WHERE user_pk = ?
                      AND status IN (
                        'ACTIVE', 'GENERATING', 'QUIZ_READY', 'ANSWERED', 'FEEDBACK_READY', 'REVIEWING', 'NEXT_ROUND',
                        'ANALYZING', 'PLANNING', 'LEARNING', 'PRACTICING', 'REPORT_READY'
                      )
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> mapSession(rs),
                userPk
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<LearningSession> findHistoryByUserPk(Long userPk, SessionStatus status, int limit, int offset) {
        if (status == null) {
            return jdbcTemplate.query(
                """
                    SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                           status, completed_at, last_active_at, created_at, updated_at
                    FROM learning_session
                    WHERE user_pk = ?
                    ORDER BY last_active_at DESC, id DESC
                    LIMIT ? OFFSET ?
                    """,
                (rs, rowNum) -> mapSession(rs),
                userPk,
                limit,
                offset
            );
        }
        return jdbcTemplate.query(
            """
                SELECT id, user_id, user_pk, course_id, chapter_id, goal_text, current_node_id, current_stage,
                       status, completed_at, last_active_at, created_at, updated_at
                FROM learning_session
                WHERE user_pk = ?
                  AND status = ?
                ORDER BY last_active_at DESC, id DESC
                LIMIT ? OFFSET ?
                """,
            (rs, rowNum) -> mapSession(rs),
            userPk,
            status.name(),
            limit,
            offset
        );
    }

    @Override
    public long countHistoryByUserPk(Long userPk, SessionStatus status) {
        if (status == null) {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM learning_session WHERE user_pk = ?",
                Long.class,
                userPk
            );
            return count == null ? 0L : count;
        }
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM learning_session WHERE user_pk = ? AND status = ?",
            Long.class,
            userPk,
            status.name()
        );
        return count == null ? 0L : count;
    }

    @Override
    public void updateCurrentPosition(Long sessionId, Long currentNodeId, Stage currentStage) {
        jdbcTemplate.update(
            """
                UPDATE learning_session
                SET current_node_id = ?,
                    current_stage = ?::task_stage,
                    updated_at = now(),
                    last_active_at = now()
                WHERE id = ?
                """,
            currentNodeId,
            currentStage.name(),
            sessionId
        );
    }

    @Override
    public void updateStatus(Long sessionId, SessionStatus status) {
        jdbcTemplate.update(
            """
                UPDATE learning_session
                SET status = ?,
                    updated_at = now(),
                    last_active_at = now()
                WHERE id = ?
                """,
            status.name(),
            sessionId
        );
    }

    @Override
    public void touchLastActive(Long sessionId) {
        jdbcTemplate.update(
            """
                UPDATE learning_session
                SET last_active_at = now(),
                    updated_at = now()
                WHERE id = ?
                """,
            sessionId
        );
    }

    private LearningSession mapSession(java.sql.ResultSet rs) throws java.sql.SQLException {
        LearningSession item = new LearningSession();
        item.setId(rs.getLong("id"));
        item.setUserId(rs.getString("user_id"));
        item.setUserPk(rs.getObject("user_pk", Long.class));
        item.setCourseId(rs.getString("course_id"));
        item.setChapterId(rs.getString("chapter_id"));
        item.setGoalText(rs.getString("goal_text"));
        item.setCurrentNodeId(rs.getLong("current_node_id"));
        String stage = rs.getString("current_stage");
        item.setCurrentStage(stage == null ? null : Stage.valueOf(stage));
        item.setStatus(SessionStatus.fromDb(rs.getString("status")));
        item.setCompletedAt(rs.getObject("completed_at", java.time.OffsetDateTime.class));
        item.setLastActiveAt(rs.getObject("last_active_at", java.time.OffsetDateTime.class));
        item.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        item.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return item;
    }
}


