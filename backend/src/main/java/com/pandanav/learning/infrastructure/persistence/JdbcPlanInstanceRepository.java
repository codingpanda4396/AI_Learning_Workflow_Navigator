package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.PlanInstanceStatus;
import com.pandanav.learning.domain.model.PlanInstance;
import com.pandanav.learning.domain.repository.PlanInstanceRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcPlanInstanceRepository implements PlanInstanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPlanInstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PlanInstance save(PlanInstance planInstance) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO plan_instance (session_id, source_plan_id, status)
                    VALUES (?, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, planInstance.getSessionId());
            ps.setObject(2, planInstance.getSourcePlanId());
            ps.setString(3, (planInstance.getStatus() == null ? PlanInstanceStatus.ACTIVE : planInstance.getStatus()).name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            planInstance.setId(key.longValue());
        }
        if (planInstance.getStatus() == null) {
            planInstance.setStatus(PlanInstanceStatus.ACTIVE);
        }
        return planInstance;
    }

    @Override
    public Optional<PlanInstance> findActiveBySessionId(Long sessionId) {
        try {
            PlanInstance planInstance = jdbcTemplate.queryForObject(
                """
                    SELECT id, session_id, source_plan_id, status, created_at, updated_at
                    FROM plan_instance
                    WHERE session_id = ?
                      AND status = 'ACTIVE'
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> map(rs),
                sessionId
            );
            return Optional.ofNullable(planInstance);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private PlanInstance map(java.sql.ResultSet rs) throws java.sql.SQLException {
        PlanInstance item = new PlanInstance();
        item.setId(rs.getLong("id"));
        item.setSessionId(rs.getLong("session_id"));
        item.setSourcePlanId(rs.getObject("source_plan_id", Long.class));
        item.setStatus(PlanInstanceStatus.valueOf(rs.getString("status")));
        item.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        item.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return item;
    }
}
