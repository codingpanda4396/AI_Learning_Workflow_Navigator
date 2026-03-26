package navigator.application.scaffold;

import navigator.api.dto.scaffold.TrainingFeedback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsBfsTrainingEvaluatorTest {

    private final DfsBfsTrainingEvaluator evaluator = new DfsBfsTrainingEvaluator();

    @Test
    void bfsShortest_passes_when_causal_chain_present() {
        String text = """
                在无权图里，BFS 会按层向外扩展，像一圈圈水波。离起点更近的点会更早进入队列被访问。
                当我们第一次到达目标顶点时，沿路的边数就是最少的，因此常用来求最短路径。
                """.strip();
        TrainingFeedback f = eval(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST, text);
        assertTrue(f.isCanProceed());
        assertTrue(f.getDetectedProblems() == null || f.getDetectedProblems().isEmpty());
    }

    @Test
    void bfsShortest_fails_when_too_short() {
        TrainingFeedback f = eval(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST, "BFS 是广度优先。");
        assertFalse(f.isCanProceed());
    }

    @Test
    void bfsShortest_max_two_problems() {
        TrainingFeedback f = eval(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST,
                "BFS 就是广度优先算法，它很厉害，大家都用它。");
        assertFalse(f.isCanProceed());
        assertTrue(f.getDetectedProblems().size() <= 2);
    }

    @Test
    void orderConsequence_passes_when_order_and_consequence_linked() {
        String text = """
                DFS 会沿着一条路尽量往深处走，走不动再退回；BFS 则先把当前层的点处理完再扩下一层。
                所以两者访问顶点的顺序很不一样，第一次找到的路径也可能不同。
                DFS 更适合需要试探深路径的问题，BFS 更适合按层或求最少步数。
                """.strip();
        TrainingFeedback f = eval(DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE, text);
        assertTrue(f.isCanProceed());
    }

    @Test
    void orderConsequence_fails_when_only_labels() {
        TrainingFeedback f = eval(DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE,
                "深度优先就是一直往下，广度优先就是一层层来，它们不一样。");
        assertFalse(f.isCanProceed());
    }

    private TrainingFeedback eval(String actionId, String text) {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .packId(DfsBfsStructureValidator.PACK_ID)
                .stageKey(DfsBfsTrainingScaffoldDefinition.STAGE_KEY)
                .actionId(actionId)
                .userInput(text)
                .build();
        return evaluator.evaluate(ctx);
    }
}
