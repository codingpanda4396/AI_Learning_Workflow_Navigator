package com.pandanav.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCapabilityProfileRepository implements CapabilityProfileRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCapabilityProfileRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CapabilityProfile save(CapabilityProfile profile) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO capability_profile
                      (learning_session_id, user_id, source_diagnosis_id, current_level,
                       strengths_json, weaknesses_json, preferences_json, constraints_json,
                       summary_text, version)
                    VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, profile.getLearningSessionId());
            ps.setObject(2, profile.getUserPk());
            ps.setLong(3, profile.getSourceDiagnosisId());
            ps.setString(4, profile.getCurrentLevel().name());
            ps.setString(5, writeJson(profile.getStrengths()));
            ps.setString(6, writeJson(profile.getWeaknesses()));
            ps.setString(7, writeJson(Map.of(
                "learningPreference", profile.getLearningPreference(),
                "goalOrientation", profile.getGoalOrientation()
            )));
            ps.setString(8, writeJson(Map.of("timeBudget", profile.getTimeBudget())));
            ps.setString(9, profile.getSummaryText());
            ps.setInt(10, profile.getVersion() == null ? 1 : profile.getVersion());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            profile.setId(key.longValue());
        }
        return profile;
    }

    @Override
    public Optional<CapabilityProfile> findLatestBySessionId(Long learningSessionId) {
        try {
            CapabilityProfile profile = jdbcTemplate.queryForObject(
                """
                    SELECT id, learning_session_id, user_id, source_diagnosis_id, current_level,
                           strengths_json, weaknesses_json, preferences_json, constraints_json,
                           summary_text, version, created_at, updated_at
                    FROM capability_profile
                    WHERE learning_session_id = ?
                    ORDER BY version DESC, created_at DESC, id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> {
                    CapabilityProfile item = new CapabilityProfile();
                    item.setId(rs.getLong("id"));
                    item.setLearningSessionId(rs.getLong("learning_session_id"));
                    item.setUserPk(rs.getObject("user_id", Long.class));
                    item.setSourceDiagnosisId(rs.getLong("source_diagnosis_id"));
                    item.setCurrentLevel(CapabilityLevel.valueOf(rs.getString("current_level")));
                    item.setStrengths(readStringList(rs.getString("strengths_json")));
                    item.setWeaknesses(readStringList(rs.getString("weaknesses_json")));
                    Map<String, String> preferences = readStringMap(rs.getString("preferences_json"));
                    Map<String, String> constraints = readStringMap(rs.getString("constraints_json"));
                    item.setLearningPreference(preferences.getOrDefault("learningPreference", ""));
                    item.setGoalOrientation(preferences.getOrDefault("goalOrientation", ""));
                    item.setTimeBudget(constraints.getOrDefault("timeBudget", ""));
                    item.setSummaryText(rs.getString("summary_text"));
                    item.setVersion(rs.getObject("version", Integer.class));
                    item.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return item;
                },
                learningSessionId
            );
            return Optional.ofNullable(profile);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize capability profile.");
        }
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, String> readStringMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }
}
