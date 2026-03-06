package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
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
