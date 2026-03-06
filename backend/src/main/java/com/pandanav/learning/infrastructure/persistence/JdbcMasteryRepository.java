package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcMasteryRepository implements MasteryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasteryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mastery> findByUserIdAndChapterId(String userId, String chapterId) {
        return jdbcTemplate.query(
            """
                SELECT cn.id AS node_id,
                       cn.name AS node_name,
                       m.user_id,
                       COALESCE(m.mastery_value, 0.000) AS mastery_value,
                       m.updated_at
                FROM concept_node cn
                LEFT JOIN mastery m
                  ON m.node_id = cn.id
                 AND m.user_id = ?
                WHERE cn.chapter_id = ?
                ORDER BY cn.order_no ASC, cn.id ASC
                """,
            (rs, rowNum) -> {
                Mastery mastery = new Mastery();
                mastery.setUserId(rs.getString("user_id"));
                mastery.setNodeId(rs.getLong("node_id"));
                mastery.setNodeName(rs.getString("node_name"));
                mastery.setMasteryValue(rs.getBigDecimal("mastery_value"));
                mastery.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                return mastery;
            },
            userId,
            chapterId
        );
    }
}
