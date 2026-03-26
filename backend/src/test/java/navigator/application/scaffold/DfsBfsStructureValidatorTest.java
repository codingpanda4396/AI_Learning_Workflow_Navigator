package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsBfsStructureValidatorTest {

    private final DfsBfsStructureValidator validator = new DfsBfsStructureValidator();

    @Test
    void rejectsShortInput() {
        ValidationResult r = validator.validate(ctx("dfs_bfs_structure_position", "太短"));
        assertFalse(r.isPassed());
        assertTrue("INSUFFICIENT_CONTENT".equals(r.getErrorType()));
    }

    @Test
    void rejectsBoundaryComplexity() {
        ValidationResult r = validator.validate(ctx("dfs_bfs_structure_position",
                "DFS和BFS是图上的两种遍历方式，时间复杂度一般是O(V+E)，这里我想先讲清它们各自解决什么问题。"));
        assertFalse(r.isPassed());
        assertTrue("BOUNDARY_VIOLATION".equals(r.getErrorType()));
    }

    @Test
    void acceptsStructuralAnswer() {
        ValidationResult r = validator.validate(ctx("dfs_bfs_structure_position",
                "它们主要帮助我在图或树结构里决定「先往深处走」还是「一层层铺开」，用来判断可达关系或分层信息，先停留在用途层面。"));
        assertTrue(r.isPassed());
    }

    private static StructureValidationContext ctx(String actionId, String text) {
        return StructureValidationContext.builder()
                .packId(DfsBfsStructureValidator.PACK_ID)
                .stageKey(DfsBfsStructureValidator.STAGE_KEY)
                .actionId(actionId)
                .userInput(text)
                .build();
    }
}
