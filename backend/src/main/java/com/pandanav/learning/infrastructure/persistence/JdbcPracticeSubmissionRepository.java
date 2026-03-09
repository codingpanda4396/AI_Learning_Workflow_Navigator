package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPracticeSubmissionRepository implements PracticeSubmissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPracticeSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PracticeSubmission save(PracticeSubmission submission) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO practice_submission (
                        practice_item_id, session_id, task_id, user_id, user_answer,
                        score, is_correct, error_tags_json, feedback, judge_mode,
                        prompt_version, token_input, token_output, latency_ms, trace_id
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, submission.getPracticeItemId());
            ps.setLong(2, submission.getSessionId());
            ps.setLong(3, submission.getTaskId());
            ps.setLong(4, submission.getUserId());
            ps.setString(5, submission.getUserAnswer());
            ps.setObject(6, submission.getScore());
            ps.setObject(7, submission.getCorrect());
            ps.setString(8, submission.getErrorTagsJson() == null ? "[]" : submission.getErrorTagsJson());
            ps.setString(9, submission.getFeedback());
            ps.setString(10, submission.getJudgeMode() == null ? "RULE" : submission.getJudgeMode());
            ps.setString(11, submission.getPromptVersion());
            ps.setObject(12, submission.getTokenInput());
            ps.setObject(13, submission.getTokenOutput());
            ps.setObject(14, submission.getLatencyMs());
            ps.setString(15, submission.getTraceId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            submission.setId(key.longValue());
        }
        return submission;
    }

    @Override
    public List<PracticeSubmission> findByPracticeItemId(Long practiceItemId) {
        return jdbcTemplate.query(
            """
                SELECT id, practice_item_id, session_id, task_id, user_id, user_answer,
                       score, is_correct, error_tags_json, feedback, judge_mode,
                       prompt_version, token_input, token_output, latency_ms, trace_id, submitted_at
                FROM practice_submission
                WHERE practice_item_id = ?
                ORDER BY submitted_at DESC, id DESC
                """,
            (rs, rowNum) -> mapSubmission(rs),
            practiceItemId
        );
    }

    @Override
    public List<PracticeSubmission> findBySessionIdAndTaskId(Long sessionId, Long taskId) {
        return jdbcTemplate.query(
            """
                SELECT id, practice_item_id, session_id, task_id, user_id, user_answer,
                       score, is_correct, error_tags_json, feedback, judge_mode,
                       prompt_version, token_input, token_output, latency_ms, trace_id, submitted_at
                FROM practice_submission
                WHERE session_id = ?
                  AND task_id = ?
                ORDER BY submitted_at DESC, id DESC
                """,
            (rs, rowNum) -> mapSubmission(rs),
            sessionId,
            taskId
        );
    }

    @Override
    public List<PracticeSubmission> findBySessionIdAndTaskIdAndUserPk(Long sessionId, Long taskId, Long userPk) {
        return jdbcTemplate.query(
            """
                SELECT ps.id, ps.practice_item_id, ps.session_id, ps.task_id, ps.user_id, ps.user_answer,
                       ps.score, ps.is_correct, ps.error_tags_json, ps.feedback, ps.judge_mode,
                       ps.prompt_version, ps.token_input, ps.token_output, ps.latency_ms, ps.trace_id, ps.submitted_at
                FROM practice_submission ps
                JOIN learning_session ls ON ls.id = ps.session_id
                WHERE ps.session_id = ?
                  AND ps.task_id = ?
                  AND ls.user_pk = ?
                ORDER BY ps.submitted_at DESC, ps.id DESC
                """,
            (rs, rowNum) -> mapSubmission(rs),
            sessionId,
            taskId,
            userPk
        );
    }

    @Override
    public Optional<PracticeSubmission> findLatestByPracticeItemIdAndUserPk(Long practiceItemId, Long userPk) {
        try {
            PracticeSubmission submission = jdbcTemplate.queryForObject(
                """
                    SELECT ps.id, ps.practice_item_id, ps.session_id, ps.task_id, ps.user_id, ps.user_answer,
                           ps.score, ps.is_correct, ps.error_tags_json, ps.feedback, ps.judge_mode,
                           ps.prompt_version, ps.token_input, ps.token_output, ps.latency_ms, ps.trace_id, ps.submitted_at
                    FROM practice_submission ps
                    JOIN learning_session ls ON ls.id = ps.session_id
                    WHERE ps.practice_item_id = ?
                      AND ls.user_pk = ?
                    ORDER BY ps.submitted_at DESC, ps.id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> mapSubmission(rs),
                practiceItemId,
                userPk
            );
            return Optional.ofNullable(submission);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private PracticeSubmission mapSubmission(java.sql.ResultSet rs) throws java.sql.SQLException {
        PracticeSubmission submission = new PracticeSubmission();
        submission.setId(rs.getLong("id"));
        submission.setPracticeItemId(rs.getLong("practice_item_id"));
        submission.setSessionId(rs.getLong("session_id"));
        submission.setTaskId(rs.getLong("task_id"));
        submission.setUserId(rs.getLong("user_id"));
        submission.setUserAnswer(rs.getString("user_answer"));
        submission.setScore(rs.getObject("score", Integer.class));
        submission.setCorrect(rs.getObject("is_correct", Boolean.class));
        submission.setErrorTagsJson(rs.getString("error_tags_json"));
        submission.setFeedback(rs.getString("feedback"));
        submission.setJudgeMode(rs.getString("judge_mode"));
        submission.setPromptVersion(rs.getString("prompt_version"));
        submission.setTokenInput(rs.getObject("token_input", Integer.class));
        submission.setTokenOutput(rs.getObject("token_output", Integer.class));
        submission.setLatencyMs(rs.getObject("latency_ms", Integer.class));
        submission.setTraceId(rs.getString("trace_id"));
        submission.setSubmittedAt(rs.getObject("submitted_at", java.time.OffsetDateTime.class));
        return submission;
    }
}
