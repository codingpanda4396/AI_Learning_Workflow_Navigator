package com.pandanav.learning.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.model.LearnerFeatureSignal;
import com.pandanav.learning.domain.repository.LearnerFeatureSignalRepository;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcLearnerFeatureSignalRepository implements LearnerFeatureSignalRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcLearnerFeatureSignalRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveAll(List<LearnerFeatureSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO learner_feature_signal
                  (diagnosis_session_id, learning_session_id, user_id, question_id, feature_key, feature_value,
                   score_delta, confidence, evidence, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    LearnerFeatureSignal signal = signals.get(i);
                    ps.setLong(1, signal.getDiagnosisSessionId());
                    ps.setObject(2, signal.getLearningSessionId());
                    ps.setObject(3, signal.getUserId());
                    ps.setString(4, signal.getQuestionId());
                    ps.setString(5, signal.getFeatureKey());
                    ps.setString(6, signal.getFeatureValue());
                    ps.setBigDecimal(7, BigDecimal.valueOf(signal.getScoreDelta()));
                    ps.setBigDecimal(8, BigDecimal.valueOf(signal.getConfidence()));
                    ps.setString(9, writeJson(signal.getEvidence()));
                    ps.setString(10, signal.getSource() == null || signal.getSource().isBlank() ? "RULE" : signal.getSource());
                }

                @Override
                public int getBatchSize() {
                    return signals.size();
                }
            }
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize learner feature signal evidence.");
        }
    }
}
