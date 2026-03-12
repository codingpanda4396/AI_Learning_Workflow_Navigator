package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.PracticeQuizStatus;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.repository.PracticeQuizRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcPracticeQuizRepository implements PracticeQuizRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPracticeQuizRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PracticeQuiz save(PracticeQuiz quiz) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO practice_quiz (
                        session_id, task_id, user_id, node_id, status, question_count, answered_count,
                        generation_source, prompt_version, failure_reason, generation_status, generation_started_at,
                        trace_id, token_input, token_output, latency_ms, last_error_code
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::run_status, now(), ?, ?, ?, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, quiz.getSessionId());
            ps.setLong(2, quiz.getTaskId());
            ps.setLong(3, quiz.getUserId());
            ps.setLong(4, quiz.getNodeId());
            ps.setString(5, quiz.getStatus().name());
            ps.setObject(6, quiz.getQuestionCount());
            ps.setObject(7, quiz.getAnsweredCount());
            ps.setString(8, quiz.getGenerationSource());
            ps.setString(9, quiz.getPromptVersion());
            ps.setString(10, quiz.getFailureReason());
            ps.setString(11, (quiz.getGenerationStatus() == null ? TaskStatus.PENDING : quiz.getGenerationStatus()).name());
            ps.setString(12, quiz.getTraceId());
            ps.setObject(13, quiz.getTokenInput());
            ps.setObject(14, quiz.getTokenOutput());
            ps.setObject(15, quiz.getLatencyMs());
            ps.setString(16, quiz.getLastErrorCode());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            quiz.setId(key.longValue());
        }
        return quiz;
    }

    @Override
    public Optional<PracticeQuiz> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                    SELECT id, session_id, task_id, user_id, node_id, status, question_count, answered_count,
                           generation_source, prompt_version, failure_reason, generation_status,
                           generation_started_at, generation_finished_at, trace_id, token_input, token_output,
                           latency_ms, last_error_code, created_at, updated_at
                    FROM practice_quiz
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapQuiz(rs),
                id
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PracticeQuiz> findLatestBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                    SELECT pq.id, pq.session_id, pq.task_id, pq.user_id, pq.node_id, pq.status, pq.question_count, pq.answered_count,
                           pq.generation_source, pq.prompt_version, pq.failure_reason, pq.generation_status,
                           pq.generation_started_at, pq.generation_finished_at, pq.trace_id, pq.token_input, pq.token_output,
                           pq.latency_ms, pq.last_error_code, pq.created_at, pq.updated_at
                    FROM practice_quiz pq
                    JOIN learning_session ls ON ls.id = pq.session_id
                    WHERE pq.session_id = ?
                      AND pq.task_id = ?
                      AND ls.user_pk = ?
                    ORDER BY pq.created_at DESC, pq.id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> mapQuiz(rs),
                sessionId,
                taskId,
                userPk
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void updateStatus(Long quizId, PracticeQuizStatus status, String failureReason) {
        jdbcTemplate.update(
            """
                UPDATE practice_quiz
                SET status = ?,
                    failure_reason = ?,
                    updated_at = now()
                WHERE id = ?
                """,
            status.name(),
            failureReason,
            quizId
        );
    }

    @Override
    public void updateGenerationState(Long quizId, TaskStatus status, String failureReason, String lastErrorCode) {
        jdbcTemplate.update(
            """
                UPDATE practice_quiz
                SET generation_status = ?::run_status,
                    generation_started_at = COALESCE(generation_started_at, now()),
                    generation_finished_at = CASE
                        WHEN ? IN ('SUCCEEDED', 'FAILED') THEN now()
                        ELSE generation_finished_at
                    END,
                    failure_reason = ?,
                    last_error_code = ?,
                    updated_at = now()
                WHERE id = ?
                """,
            status.name(),
            status.name(),
            failureReason,
            lastErrorCode,
            quizId
        );
    }

    @Override
    public void markGenerated(Long quizId, Integer questionCount, String generationSource, String promptVersion) {
        jdbcTemplate.update(
            """
                UPDATE practice_quiz
                SET status = 'READY',
                    generation_status = 'SUCCEEDED'::run_status,
                    question_count = ?,
                    generation_source = ?,
                    prompt_version = ?,
                    failure_reason = NULL,
                    generation_finished_at = now(),
                    updated_at = now()
                WHERE id = ?
                """,
            questionCount,
            generationSource,
            promptVersion,
            quizId
        );
    }

    @Override
    public void updateAnsweredCount(Long quizId, Integer answeredCount) {
        jdbcTemplate.update(
            """
                UPDATE practice_quiz
                SET answered_count = ?,
                    updated_at = now()
                WHERE id = ?
                """,
            answeredCount,
            quizId
        );
    }

    private PracticeQuiz mapQuiz(java.sql.ResultSet rs) throws java.sql.SQLException {
        PracticeQuiz quiz = new PracticeQuiz();
        quiz.setId(rs.getLong("id"));
        quiz.setSessionId(rs.getLong("session_id"));
        quiz.setTaskId(rs.getLong("task_id"));
        quiz.setUserId(rs.getLong("user_id"));
        quiz.setNodeId(rs.getLong("node_id"));
        quiz.setStatus(PracticeQuizStatus.fromDb(rs.getString("status")));
        quiz.setQuestionCount(rs.getObject("question_count", Integer.class));
        quiz.setAnsweredCount(rs.getObject("answered_count", Integer.class));
        quiz.setGenerationSource(rs.getString("generation_source"));
        quiz.setPromptVersion(rs.getString("prompt_version"));
        quiz.setFailureReason(rs.getString("failure_reason"));
        quiz.setGenerationStatus(TaskStatus.fromDb(rs.getString("generation_status")));
        quiz.setGenerationStartedAt(rs.getObject("generation_started_at", java.time.OffsetDateTime.class));
        quiz.setGenerationFinishedAt(rs.getObject("generation_finished_at", java.time.OffsetDateTime.class));
        quiz.setTraceId(rs.getString("trace_id"));
        quiz.setTokenInput(rs.getObject("token_input", Integer.class));
        quiz.setTokenOutput(rs.getObject("token_output", Integer.class));
        quiz.setLatencyMs(rs.getObject("latency_ms", Integer.class));
        quiz.setLastErrorCode(rs.getString("last_error_code"));
        quiz.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        quiz.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return quiz;
    }
}
