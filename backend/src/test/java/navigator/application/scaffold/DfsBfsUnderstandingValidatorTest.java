package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsBfsUnderstandingValidatorTest {

    private final DfsBfsUnderstandingValidator validator = new DfsBfsUnderstandingValidator();

    @Test
    void rejectsShortInput() {
        ValidationResult r = validator.validate(ctx(DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS, "太短了不解释"));
        assertFalse(r.isPassed());
        assertEquals("INSUFFICIENT_CONTENT", r.getErrorType());
    }

    @Test
    void rejectsShallowDfsDefinitionOnly() {
        ValidationResult r = validator.validate(ctx(DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS,
                "DFS 就是深度优先搜索，它是一种遍历方式，和 BFS 不一样；"
                        + "深度优先这四个字听起来很专业，但我这里先只记住名字。"));
        assertFalse(r.isPassed());
        assertEquals("SHALLOW_UNDERSTANDING", r.getErrorType());
    }

    @Test
    void acceptsDfsMechanismNarrative() {
        String text = "我从任意未访问的顶点出发，沿着一条边走到邻接点继续深入；"
                + "如果当前点没有未访问邻居，就往回退到上一个分叉点再试别的边，直到所有可达点被访问。";
        ValidationResult r = validator.validate(ctx(DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS, text));
        assertTrue(r.isPassed());
    }

    @Test
    void rejectsShallowBfs() {
        ValidationResult r = validator.validate(ctx(DfsBfsUnderstandingScaffoldDefinition.ACTION_BFS_LAYERS,
                "BFS 就是广度优先，它和 DFS 一样都是图上的遍历方式，"
                        + "名字不同而已，我先把两个概念区分开来记一记。"));
        assertFalse(r.isPassed());
        assertEquals("SHALLOW_UNDERSTANDING", r.getErrorType());
    }

    @Test
    void acceptsBfsLayersNarrative() {
        String text = "从起点出发，我先处理与起点距离为 1 的那一圈邻居，再处理距离为 2 的下一层，"
                + "像水波一圈圈扩散；因此更近的顶点会更早被访问，这种顺序对无权图的最短路径直觉很有帮助。";
        ValidationResult r = validator.validate(ctx(DfsBfsUnderstandingScaffoldDefinition.ACTION_BFS_LAYERS, text));
        assertTrue(r.isPassed());
    }

    private static StructureValidationContext ctx(String actionId, String text) {
        return StructureValidationContext.builder()
                .packId(DfsBfsUnderstandingValidator.PACK_ID)
                .stageKey(DfsBfsUnderstandingValidator.STAGE_KEY)
                .actionId(actionId)
                .userInput(text)
                .build();
    }
}
