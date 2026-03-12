package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.LearningPlanStatus;
import com.pandanav.learning.domain.model.LearningPlan;
import com.pandanav.learning.domain.repository.LearningPlanRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcLearningPlanRepository implements LearningPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearningPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningPlan save(LearningPlan plan) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO learning_plan (
                      user_id, goal_id, diagnosis_id, session_id, status,
                      summary_json, reasons_json, focuses_json, path_preview_json, task_preview_json,
                      adjustments_json, planning_context_json, llm_trace_id
                    )
                    VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb),
                            CAST(? AS jsonb), CAST(? AS jsonb), ?)
                    """,
                new String[]{"id"}
            );
            write(ps, plan);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            plan.setId(key.longValue());
        }
        return plan;
    }

    @Override
    public LearningPlan update(LearningPlan plan) {
        jdbcTemplate.update(
            """
                UPDATE learning_plan
                SET session_id = ?,
                    status = ?,
                    summary_json = CAST(? AS jsonb),
                    reasons_json = CAST(? AS jsonb),
                    focuses_json = CAST(? AS jsonb),
                    path_preview_json = CAST(? AS jsonb),
                    task_preview_json = CAST(? AS jsonb),
                    adjustments_json = CAST(? AS jsonb),
                    planning_context_json = CAST(? AS jsonb),
                    llm_trace_id = ?,
                    updated_at = now()
                WHERE id = ? AND user_id = ?
                """,
            plan.getSessionId(),
            plan.getStatus().name(),
            plan.getSummaryJson(),
            plan.getReasonsJson(),
            plan.getFocusesJson(),
            plan.getPathPreviewJson(),
            plan.getTaskPreviewJson(),
            plan.getAdjustmentsJson(),
            plan.getPlanningContextJson(),
            plan.getLlmTraceId(),
            plan.getId(),
            plan.getUserId()
        );
        return plan;
    }

    @Override
    public Optional<LearningPlan> findByIdAndUserId(Long id, Long userId) {
        try {
            LearningPlan plan = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, goal_id, diagnosis_id, session_id, status,
                           summary_json, reasons_json, focuses_json, path_preview_json, task_preview_json,
                           adjustments_json, planning_context_json, llm_trace_id, created_at, updated_at
                    FROM learning_plan
                    WHERE id = ? AND user_id = ?
                    """,
                (rs, rowNum) -> map(rs),
                id,
                userId
            );
            return Optional.ofNullable(plan);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void write(PreparedStatement ps, LearningPlan plan) throws java.sql.SQLException {
        ps.setLong(1, plan.getUserId());
        ps.setString(2, plan.getGoalId());
        ps.setString(3, plan.getDiagnosisId());
        ps.setObject(4, plan.getSessionId());
        ps.setString(5, plan.getStatus().name());
        ps.setString(6, plan.getSummaryJson());
        ps.setString(7, plan.getReasonsJson());
        ps.setString(8, plan.getFocusesJson());
        ps.setString(9, plan.getPathPreviewJson());
        ps.setString(10, plan.getTaskPreviewJson());
        ps.setString(11, plan.getAdjustmentsJson());
        ps.setString(12, plan.getPlanningContextJson());
        ps.setString(13, plan.getLlmTraceId());
    }

    private LearningPlan map(java.sql.ResultSet rs) throws java.sql.SQLException {
        LearningPlan plan = new LearningPlan();
        plan.setId(rs.getLong("id"));
        plan.setUserId(rs.getLong("user_id"));
        plan.setGoalId(rs.getString("goal_id"));
        plan.setDiagnosisId(rs.getString("diagnosis_id"));
        plan.setSessionId(rs.getObject("session_id", Long.class));
        plan.setStatus(LearningPlanStatus.valueOf(rs.getString("status")));
        plan.setSummaryJson(rs.getString("summary_json"));
        plan.setReasonsJson(rs.getString("reasons_json"));
        plan.setFocusesJson(rs.getString("focuses_json"));
        plan.setPathPreviewJson(rs.getString("path_preview_json"));
        plan.setTaskPreviewJson(rs.getString("task_preview_json"));
        plan.setAdjustmentsJson(rs.getString("adjustments_json"));
        plan.setPlanningContextJson(rs.getString("planning_context_json"));
        plan.setLlmTraceId(rs.getString("llm_trace_id"));
        plan.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        plan.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return plan;
    }
}
