package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsBfsReflectionEvaluatorTest {

    private final DfsBfsReflectionEvaluator evaluator = new DfsBfsReflectionEvaluator();

    @Test
    void errorRecall_rejectsGeneric() {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .actionId(DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL)
                .userInput("我不太会")
                .build();
        ValidationResult v = evaluator.validate(ctx);
        assertFalse(v.isPassed());
    }

    @Test
    void errorRecall_acceptsConcrete() {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .actionId(DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL)
                .userInput("我曾把 BFS 第一次到达终点误当成任意最短路成立，没有说无权图与按层扩展的前提。")
                .build();
        ValidationResult v = evaluator.validate(ctx);
        assertTrue(v.isPassed());
    }

    @Test
    void decisionRule_rejectsDefinitionOnly() {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .actionId(DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE)
                .userInput("DFS 是深度优先，BFS 是广度优先。")
                .build();
        ValidationResult v = evaluator.validate(ctx);
        assertFalse(v.isPassed());
    }

    @Test
    void decisionRule_acceptsConditionalRule() {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .actionId(DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE)
                .userInput("当问题强调层次扩展、无权最短路径时，我优先想到 BFS；当需要深度试探、回溯结构时，我优先想到 DFS。")
                .build();
        ValidationResult v = evaluator.validate(ctx);
        assertTrue(v.isPassed());
    }

    @Test
    void capability_rejectsVagueMastered() {
        StructureValidationContext ctx = StructureValidationContext.builder()
                .actionId(DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME)
                .userInput("学会了 DFS 和 BFS")
                .build();
        ValidationResult v = evaluator.validate(ctx);
        assertFalse(v.isPassed());
    }
}
