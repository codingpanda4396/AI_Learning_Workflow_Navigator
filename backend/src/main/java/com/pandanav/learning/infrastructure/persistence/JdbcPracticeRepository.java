package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.PracticeItemSource;
import com.pandanav.learning.domain.enums.PracticeItemStatus;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.repository.PracticeRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPracticeRepository implements PracticeRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPracticeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PracticeItem save(PracticeItem item) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO practice_item (
                        session_id, task_id, quiz_id, user_id, node_id, stage, question_type,
                        stem, options_json, standard_answer, explanation, difficulty,
                        source, status, prompt_version, token_input, token_output, latency_ms, trace_id
                    )
                    VALUES (?, ?, ?, ?, ?, ?::task_stage, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, item.getSessionId());
            ps.setLong(2, item.getTaskId());
            ps.setObject(3, item.getQuizId());
            ps.setLong(4, item.getUserId());
            ps.setLong(5, item.getNodeId());
            ps.setString(6, item.getStage() == null ? Stage.TRAINING.name() : item.getStage().name());
            ps.setString(7, item.getQuestionType().name());
            ps.setString(8, item.getStem());
            ps.setString(9, item.getOptionsJson());
            ps.setString(10, item.getStandardAnswer());
            ps.setString(11, item.getExplanation());
            ps.setString(12, item.getDifficulty());
            ps.setString(13, item.getSource().name());
            ps.setString(14, item.getStatus().name());
            ps.setString(15, item.getPromptVersion());
            ps.setObject(16, item.getTokenInput());
            ps.setObject(17, item.getTokenOutput());
            ps.setObject(18, item.getLatencyMs());
            ps.setString(19, item.getTraceId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            item.setId(key.longValue());
        }
        return item;
    }

    @Override
    public Optional<PracticeItem> findById(Long id) {
        try {
            PracticeItem item = jdbcTemplate.queryForObject(
                """
                    SELECT id, session_id, task_id, quiz_id, user_id, node_id, stage, question_type,
                           stem, options_json, standard_answer, explanation, difficulty,
                           source, status, prompt_version, token_input, token_output, latency_ms, trace_id, created_at
                    FROM practice_item
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapItem(rs),
                id
            );
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PracticeItem> findByIdAndUserPk(Long id, Long userPk) {
        try {
            PracticeItem item = jdbcTemplate.queryForObject(
                """
                    SELECT pi.id, pi.session_id, pi.task_id, pi.quiz_id, pi.user_id, pi.node_id, pi.stage, pi.question_type,
                           pi.stem, pi.options_json, pi.standard_answer, pi.explanation, pi.difficulty,
                           pi.source, pi.status, pi.prompt_version, pi.token_input, pi.token_output, pi.latency_ms,
                           pi.trace_id, pi.created_at
                    FROM practice_item pi
                    JOIN learning_session ls ON ls.id = pi.session_id
                    WHERE pi.id = ?
                      AND ls.user_pk = ?
                    """,
                (rs, rowNum) -> mapItem(rs),
                id,
                userPk
            );
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<PracticeItem> findBySessionIdAndTaskId(Long sessionId, Long taskId) {
        return jdbcTemplate.query(
            """
                SELECT id, session_id, task_id, quiz_id, user_id, node_id, stage, question_type,
                       stem, options_json, standard_answer, explanation, difficulty,
                       source, status, prompt_version, token_input, token_output, latency_ms, trace_id, created_at
                FROM practice_item
                WHERE session_id = ?
                  AND task_id = ?
                ORDER BY created_at ASC, id ASC
                """,
            (rs, rowNum) -> mapItem(rs),
            sessionId,
            taskId
        );
    }

    @Override
    public List<PracticeItem> findBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk) {
        return jdbcTemplate.query(
            """
                SELECT pi.id, pi.session_id, pi.task_id, pi.quiz_id, pi.user_id, pi.node_id, pi.stage, pi.question_type,
                       pi.stem, pi.options_json, pi.standard_answer, pi.explanation, pi.difficulty,
                       pi.source, pi.status, pi.prompt_version, pi.token_input, pi.token_output, pi.latency_ms,
                       pi.trace_id, pi.created_at
                FROM practice_item pi
                JOIN learning_session ls ON ls.id = pi.session_id
                WHERE pi.session_id = ?
                  AND pi.task_id = ?
                  AND ls.user_pk = ?
                ORDER BY pi.created_at ASC, pi.id ASC
                """,
            (rs, rowNum) -> mapItem(rs),
            sessionId,
            taskId,
            userPk
        );
    }

    @Override
    public List<PracticeItem> findByQuizId(Long quizId) {
        return jdbcTemplate.query(
            """
                SELECT id, session_id, task_id, quiz_id, user_id, node_id, stage, question_type,
                       stem, options_json, standard_answer, explanation, difficulty,
                       source, status, prompt_version, token_input, token_output, latency_ms, trace_id, created_at
                FROM practice_item
                WHERE quiz_id = ?
                ORDER BY created_at ASC, id ASC
                """,
            (rs, rowNum) -> mapItem(rs),
            quizId
        );
    }

    @Override
    public void updateStatus(Long id, PracticeItemStatus status) {
        jdbcTemplate.update(
            """
                UPDATE practice_item
                SET status = ?
                WHERE id = ?
                """,
            status.name(),
            id
        );
    }

    private PracticeItem mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        PracticeItem item = new PracticeItem();
        item.setId(rs.getLong("id"));
        item.setSessionId(rs.getLong("session_id"));
        item.setTaskId(rs.getLong("task_id"));
        item.setQuizId(rs.getObject("quiz_id", Long.class));
        item.setUserId(rs.getLong("user_id"));
        item.setNodeId(rs.getLong("node_id"));
        item.setStage(Stage.valueOf(rs.getString("stage")));
        item.setQuestionType(PracticeQuestionType.fromDb(rs.getString("question_type")));
        item.setStem(rs.getString("stem"));
        item.setOptionsJson(rs.getString("options_json"));
        item.setStandardAnswer(rs.getString("standard_answer"));
        item.setExplanation(rs.getString("explanation"));
        item.setDifficulty(rs.getString("difficulty"));
        item.setSource(PracticeItemSource.fromDb(rs.getString("source")));
        item.setStatus(PracticeItemStatus.fromDb(rs.getString("status")));
        item.setPromptVersion(rs.getString("prompt_version"));
        item.setTokenInput(rs.getObject("token_input", Integer.class));
        item.setTokenOutput(rs.getObject("token_output", Integer.class));
        item.setLatencyMs(rs.getObject("latency_ms", Integer.class));
        item.setTraceId(rs.getString("trace_id"));
        item.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return item;
    }
}
