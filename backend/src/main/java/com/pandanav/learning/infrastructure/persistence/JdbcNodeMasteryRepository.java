package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcNodeMasteryRepository implements NodeMasteryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNodeMasteryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NodeMastery upsert(NodeMastery mastery) {
        jdbcTemplate.update(
            """
                INSERT INTO node_mastery (
                    user_id, session_id, node_id, mastery_score, training_accuracy,
                    recent_error_tags_json, latest_evaluation_score, attempt_count, updated_at
                )
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, now())
                ON CONFLICT (user_id, node_id)
                DO UPDATE SET session_id = EXCLUDED.session_id,
                              mastery_score = EXCLUDED.mastery_score,
                              training_accuracy = EXCLUDED.training_accuracy,
                              recent_error_tags_json = EXCLUDED.recent_error_tags_json,
                              latest_evaluation_score = EXCLUDED.latest_evaluation_score,
                              attempt_count = EXCLUDED.attempt_count,
                              updated_at = now()
                """,
            mastery.getUserId(),
            mastery.getSessionId(),
            mastery.getNodeId(),
            mastery.getMasteryScore(),
            mastery.getTrainingAccuracy(),
            mastery.getRecentErrorTagsJson() == null ? "[]" : mastery.getRecentErrorTagsJson(),
            mastery.getLatestEvaluationScore(),
            mastery.getAttemptCount() == null ? 0 : mastery.getAttemptCount()
        );
        return findByUserIdAndNodeId(mastery.getUserId(), mastery.getNodeId()).orElse(mastery);
    }

    @Override
    public Optional<NodeMastery> findByUserIdAndNodeId(Long userId, Long nodeId) {
        try {
            NodeMastery item = jdbcTemplate.queryForObject(
                """
                    SELECT nm.id, nm.user_id, nm.session_id, nm.node_id, cn.name AS node_name,
                           nm.mastery_score, nm.training_accuracy, nm.recent_error_tags_json,
                           nm.latest_evaluation_score, nm.attempt_count, nm.updated_at
                    FROM node_mastery nm
                    JOIN concept_node cn ON cn.id = nm.node_id
                    WHERE nm.user_id = ?
                      AND nm.node_id = ?
                    """,
                (rs, rowNum) -> map(rs),
                userId,
                nodeId
            );
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<NodeMastery> findByUserIdAndChapterId(Long userId, String chapterId) {
        return jdbcTemplate.query(
            """
                SELECT nm.id, nm.user_id, nm.session_id, nm.node_id, cn.name AS node_name,
                       nm.mastery_score, nm.training_accuracy, nm.recent_error_tags_json,
                       nm.latest_evaluation_score, nm.attempt_count, nm.updated_at
                FROM node_mastery nm
                JOIN concept_node cn ON cn.id = nm.node_id
                WHERE nm.user_id = ?
                  AND cn.chapter_id = ?
                ORDER BY nm.mastery_score ASC, nm.updated_at DESC
                """,
            (rs, rowNum) -> map(rs),
            userId,
            chapterId
        );
    }

    private NodeMastery map(java.sql.ResultSet rs) throws java.sql.SQLException {
        NodeMastery item = new NodeMastery();
        item.setId(rs.getLong("id"));
        item.setUserId(rs.getLong("user_id"));
        item.setSessionId(rs.getObject("session_id", Long.class));
        item.setNodeId(rs.getLong("node_id"));
        item.setNodeName(rs.getString("node_name"));
        item.setMasteryScore(rs.getBigDecimal("mastery_score"));
        item.setTrainingAccuracy(rs.getBigDecimal("training_accuracy"));
        item.setRecentErrorTagsJson(rs.getString("recent_error_tags_json"));
        item.setLatestEvaluationScore(rs.getObject("latest_evaluation_score", Integer.class));
        item.setAttemptCount(rs.getObject("attempt_count", Integer.class));
        item.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return item;
    }
}
