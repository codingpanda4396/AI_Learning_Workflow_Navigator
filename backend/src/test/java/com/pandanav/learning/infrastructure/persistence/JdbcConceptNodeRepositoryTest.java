package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.ConceptNode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class JdbcConceptNodeRepositoryTest {

    @Test
    void shouldReadIdFromGeneratedKeysMapWhenDriverReturnsMultipleColumns() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(Map.of(
                "id", 22L,
                "chapter_id", "导数",
                "name", "Foundation of 导数"
            ));
            return 1;
        }).when(jdbcTemplate).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class), any(KeyHolder.class));

        JdbcConceptNodeRepository repository = new JdbcConceptNodeRepository(jdbcTemplate);
        ConceptNode node = new ConceptNode();
        node.setChapterId("导数");
        node.setName("Foundation of 导数");
        node.setOutline("outline");
        node.setOrderNo(10);

        ConceptNode saved = repository.save(node);

        assertEquals(22L, saved.getId());
    }
}
