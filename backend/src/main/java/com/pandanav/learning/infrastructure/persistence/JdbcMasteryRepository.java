package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
            (rs, rowNum) -> mapMastery(rs),
            userId,
            chapterId
        );
    }

    @Override
    public Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId) {
        try {
            Mastery mastery = jdbcTemplate.queryForObject(
                """
                    SELECT m.user_id, m.node_id, cn.name AS node_name, m.mastery_value, m.updated_at
                    FROM mastery m
                    JOIN concept_node cn ON cn.id = m.node_id
                    WHERE m.user_id = ? AND m.node_id = ?
                    """,
                (rs, rowNum) -> mapMastery(rs),
                userId,
                nodeId
            );
            return Optional.ofNullable(mastery);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Mastery upsert(Mastery mastery) {
        jdbcTemplate.update(
            """
                INSERT INTO mastery (user_id, node_id, mastery_value, updated_at)
                VALUES (?, ?, ?, now())
                ON CONFLICT (user_id, node_id)
                DO UPDATE SET mastery_value = EXCLUDED.mastery_value,
                              updated_at = now()
                """,
            mastery.getUserId(),
            mastery.getNodeId(),
            mastery.getMasteryValue()
        );
        return findByUserIdAndNodeId(mastery.getUserId(), mastery.getNodeId())
            .orElse(mastery);
    }

    private Mastery mapMastery(java.sql.ResultSet rs) throws java.sql.SQLException {
        Mastery mastery = new Mastery();
        mastery.setUserId(rs.getString("user_id"));
        mastery.setNodeId(rs.getLong("node_id"));
        mastery.setNodeName(rs.getString("node_name"));
        mastery.setMasteryValue(rs.getBigDecimal("mastery_value"));
        mastery.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return mastery;
    }
}


