package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcPracticeFeedbackReportRepository implements PracticeFeedbackReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPracticeFeedbackReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PracticeFeedbackReport save(PracticeFeedbackReport report) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    """
                        INSERT INTO practice_feedback_report (
                            quiz_id, session_id, task_id, user_id, diagnosis_summary,
                            strengths_json, weaknesses_json, review_focus_json, next_round_advice,
                            recommended_action, selected_action, source, prompt_version,
                            report_status, report_started_at, report_finished_at, action_selected_at,
                            trace_id, token_input, token_output, latency_ms, last_error_code
                        )
                        VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?::run_status, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[]{"id"}
                );
                ps.setLong(1, report.getQuizId());
                ps.setLong(2, report.getSessionId());
                ps.setLong(3, report.getTaskId());
                ps.setLong(4, report.getUserId());
                ps.setString(5, report.getDiagnosisSummary());
                ps.setString(6, report.getStrengthsJson());
                ps.setString(7, report.getWeaknessesJson());
                ps.setString(8, report.getReviewFocusJson());
                ps.setString(9, report.getNextRoundAdvice());
                ps.setString(10, report.getRecommendedAction());
                ps.setString(11, report.getSelectedAction());
                ps.setString(12, report.getSource());
                ps.setString(13, report.getPromptVersion());
                ps.setString(14, (report.getReportStatus() == null ? TaskStatus.SUCCEEDED : report.getReportStatus()).name());
                ps.setObject(15, report.getReportStartedAt());
                ps.setObject(16, report.getReportFinishedAt());
                ps.setObject(17, report.getActionSelectedAt());
                ps.setString(18, report.getTraceId());
                ps.setObject(19, report.getTokenInput());
                ps.setObject(20, report.getTokenOutput());
                ps.setObject(21, report.getLatencyMs());
                ps.setString(22, report.getLastErrorCode());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                report.setId(key.longValue());
            }
            return report;
        } catch (DuplicateKeyException ex) {
            return findByQuizId(report.getQuizId()).orElseThrow();
        }
    }

    @Override
    public Optional<PracticeFeedbackReport> findByQuizId(Long quizId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                    SELECT id, quiz_id, session_id, task_id, user_id, diagnosis_summary,
                           strengths_json, weaknesses_json, review_focus_json, next_round_advice,
                           recommended_action, selected_action, source, prompt_version,
                           report_status, report_started_at, report_finished_at, action_selected_at,
                           trace_id, token_input, token_output, latency_ms, last_error_code, created_at
                    FROM practice_feedback_report
                    WHERE quiz_id = ?
                    """,
                (rs, rowNum) -> mapReport(rs),
                quizId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void markActionSelected(Long reportId, String selectedAction) {
        jdbcTemplate.update(
            """
                UPDATE practice_feedback_report
                SET selected_action = ?,
                    action_selected_at = now()
                WHERE id = ?
                """,
            selectedAction,
            reportId
        );
    }

    private PracticeFeedbackReport mapReport(java.sql.ResultSet rs) throws java.sql.SQLException {
        PracticeFeedbackReport report = new PracticeFeedbackReport();
        report.setId(rs.getLong("id"));
        report.setQuizId(rs.getLong("quiz_id"));
        report.setSessionId(rs.getLong("session_id"));
        report.setTaskId(rs.getLong("task_id"));
        report.setUserId(rs.getLong("user_id"));
        report.setDiagnosisSummary(rs.getString("diagnosis_summary"));
        report.setStrengthsJson(rs.getString("strengths_json"));
        report.setWeaknessesJson(rs.getString("weaknesses_json"));
        report.setReviewFocusJson(rs.getString("review_focus_json"));
        report.setNextRoundAdvice(rs.getString("next_round_advice"));
        report.setRecommendedAction(rs.getString("recommended_action"));
        report.setSelectedAction(rs.getString("selected_action"));
        report.setSource(rs.getString("source"));
        report.setPromptVersion(rs.getString("prompt_version"));
        report.setReportStatus(TaskStatus.fromDb(rs.getString("report_status")));
        report.setReportStartedAt(rs.getObject("report_started_at", java.time.OffsetDateTime.class));
        report.setReportFinishedAt(rs.getObject("report_finished_at", java.time.OffsetDateTime.class));
        report.setActionSelectedAt(rs.getObject("action_selected_at", java.time.OffsetDateTime.class));
        report.setTraceId(rs.getString("trace_id"));
        report.setTokenInput(rs.getObject("token_input", Integer.class));
        report.setTokenOutput(rs.getObject("token_output", Integer.class));
        report.setLatencyMs(rs.getObject("latency_ms", Integer.class));
        report.setLastErrorCode(rs.getString("last_error_code"));
        report.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return report;
    }
}
