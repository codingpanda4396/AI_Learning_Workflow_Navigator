package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

@Repository
public class JdbcLearningEventRepository implements LearningEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearningEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningEvent save(LearningEvent event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO learning_event (session_id, user_id, event_type, event_data)
                    VALUES (?, ?, ?, CAST(? AS jsonb))
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, event.getSessionId());
            ps.setLong(2, event.getUserId());
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getEventData() == null ? "{}" : event.getEventData());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            event.setId(key.longValue());
        }
        return event;
    }
}
