package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.repository.DiagnosisAnswerRepository;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcDiagnosisAnswerRepository implements DiagnosisAnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDiagnosisAnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAll(List<DiagnosisAnswer> answers) {
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO diagnosis_answer
                  (diagnosis_session_id, question_id, dimension, answer_type, answer_value_json, raw_text)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)
                """,
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DiagnosisAnswer answer = answers.get(i);
                    ps.setLong(1, answer.getDiagnosisSessionId());
                    ps.setString(2, answer.getQuestionId());
                    ps.setString(3, answer.getDimension().name());
                    ps.setString(4, answer.getAnswerType());
                    ps.setString(5, answer.getAnswerValueJson());
                    ps.setString(6, answer.getRawText());
                }

                @Override
                public int getBatchSize() {
                    return answers.size();
                }
            }
        );
    }
}
