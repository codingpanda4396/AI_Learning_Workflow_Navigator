package com.pandanav.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import com.pandanav.learning.domain.repository.LearnerProfileSnapshotRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcLearnerProfileSnapshotRepository implements LearnerProfileSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcLearnerProfileSnapshotRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LearnerProfileSnapshot saveOrUpdate(LearnerProfileSnapshot snapshot) {
        LearnerProfileSnapshot saved = jdbcTemplate.queryForObject(
            """
                INSERT INTO learner_profile_snapshot
                  (diagnosis_session_id, learning_session_id, user_id, profile_version,
                   feature_summary_json, strategy_hints_json, constraints_json, explanations_json)
                VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb)
                ON CONFLICT (diagnosis_session_id) DO UPDATE
                  SET learning_session_id = EXCLUDED.learning_session_id,
                      user_id = EXCLUDED.user_id,
                      profile_version = EXCLUDED.profile_version,
                      feature_summary_json = EXCLUDED.feature_summary_json,
                      strategy_hints_json = EXCLUDED.strategy_hints_json,
                      constraints_json = EXCLUDED.constraints_json,
                      explanations_json = EXCLUDED.explanations_json,
                      updated_at = now()
                RETURNING id, created_at, updated_at
                """,
            (rs, rowNum) -> {
                snapshot.setId(rs.getLong("id"));
                snapshot.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                snapshot.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                return snapshot;
            },
            snapshot.getDiagnosisSessionId(),
            snapshot.getLearningSessionId(),
            snapshot.getUserId(),
            snapshot.getProfileVersion() == null ? 1 : snapshot.getProfileVersion(),
            writeJson(snapshot.getFeatureSummary()),
            writeJson(snapshot.getStrategyHints()),
            writeJson(snapshot.getConstraints()),
            writeJson(snapshot.getExplanations())
        );
        if (saved == null) {
            throw new InternalServerException("Failed to save learner profile snapshot.");
        }
        return saved;
    }

    @Override
    public Optional<LearnerProfileSnapshot> findByDiagnosisSessionId(Long diagnosisSessionId) {
        try {
            LearnerProfileSnapshot snapshot = jdbcTemplate.queryForObject(
                """
                    SELECT id, diagnosis_session_id, learning_session_id, user_id, profile_version,
                           feature_summary_json, strategy_hints_json, constraints_json, explanations_json,
                           created_at, updated_at
                    FROM learner_profile_snapshot
                    WHERE diagnosis_session_id = ?
                    LIMIT 1
                    """,
                (rs, rowNum) -> {
                    LearnerProfileSnapshot item = new LearnerProfileSnapshot();
                    item.setId(rs.getLong("id"));
                    item.setDiagnosisSessionId(rs.getLong("diagnosis_session_id"));
                    item.setLearningSessionId(rs.getObject("learning_session_id", Long.class));
                    item.setUserId(rs.getObject("user_id", Long.class));
                    item.setProfileVersion(rs.getObject("profile_version", Integer.class));
                    item.setFeatureSummary(readJsonMap(rs.getString("feature_summary_json")));
                    item.setStrategyHints(readJsonMap(rs.getString("strategy_hints_json")));
                    item.setConstraints(readJsonMap(rs.getString("constraints_json")));
                    item.setExplanations(readJsonMap(rs.getString("explanations_json")));
                    item.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return item;
                },
                diagnosisSessionId
            );
            return Optional.ofNullable(snapshot);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize learner profile snapshot.");
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }
}
