package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcConceptNodeRepository implements ConceptNodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConceptNodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ConceptNode> findById(Long id) {
        try {
            ConceptNode node = jdbcTemplate.queryForObject(
                """
                    SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                    FROM concept_node
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapNode(rs),
                id
            );
            return Optional.ofNullable(node);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ConceptNode> findFirstByChapterIdOrderByOrderNoAsc(String chapterId) {
        List<ConceptNode> nodes = jdbcTemplate.query(
            """
                SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                FROM concept_node
                WHERE chapter_id = ?
                ORDER BY order_no ASC, id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> mapNode(rs),
            chapterId
        );
        return nodes.stream().findFirst();
    }

    @Override
    public List<ConceptNode> findByChapterIdOrderByOrderNoAsc(String chapterId) {
        return jdbcTemplate.query(
            """
                SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                FROM concept_node
                WHERE chapter_id = ?
                ORDER BY order_no ASC, id ASC
                """,
            (rs, rowNum) -> mapNode(rs),
            chapterId
        );
    }

    @Override
    public Map<Long, List<Long>> findPrerequisiteNodeIdsByChapterId(String chapterId) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
                SELECT cp.node_id, cp.prereq_node_id
                FROM concept_prerequisite cp
                JOIN concept_node cn ON cn.id = cp.node_id
                WHERE cn.chapter_id = ?
                ORDER BY cp.node_id ASC, cp.prereq_node_id ASC
                """,
            rs -> {
                Long nodeId = rs.getLong("node_id");
                Long prereqNodeId = rs.getLong("prereq_node_id");
                result.computeIfAbsent(nodeId, key -> new ArrayList<>()).add(prereqNodeId);
            },
            chapterId
        );
        return result;
    }

    @Override
    public ConceptNode save(ConceptNode node) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                    INSERT INTO concept_node (chapter_id, name, outline, order_no)
                    VALUES (?, ?, ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, node.getChapterId());
            statement.setString(2, node.getName());
            statement.setString(3, node.getOutline());
            statement.setInt(4, node.getOrderNo() == null ? 0 : node.getOrderNo());
            return statement;
        }, keyHolder);

        Number id = keyHolder.getKey();
        if (id != null) {
            node.setId(id.longValue());
        }
        return node;
    }

    private ConceptNode mapNode(java.sql.ResultSet rs) throws java.sql.SQLException {
        ConceptNode node = new ConceptNode();
        node.setId(rs.getLong("id"));
        node.setChapterId(rs.getString("chapter_id"));
        node.setName(rs.getString("name"));
        node.setOutline(rs.getString("outline"));
        node.setOrderNo(rs.getInt("order_no"));
        node.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        node.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return node;
    }
}
