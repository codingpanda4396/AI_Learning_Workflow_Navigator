package navigator.application.task;

import navigator.domain.model.ExecutableTaskSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionEvaluatorTest {

    private final CompletionEvaluator evaluator = new CompletionEvaluator();

    @Test
    void fallbackSelfExplain_passWhenLongEnough() {
        EvaluationResult r = evaluator.evaluateSelfExplanation(null, null,
                "我理解了链表的结构，每个节点包含数据和指向下一个节点的指针，这是一种线性数据结构。");
        assertThat(r.isPass()).isTrue();
    }

    @Test
    void fallbackSelfExplain_failWhenTooShort() {
        EvaluationResult r = evaluator.evaluateSelfExplanation(null, null, "懂了");
        assertThat(r.isPass()).isFalse();
    }

    @Test
    void fallbackCheckpoint_passWhenLongEnough() {
        EvaluationResult r = evaluator.evaluateCheckpoint(null, null, "链表是节点通过指针连接的数据结构");
        assertThat(r.isPass()).isTrue();
    }

    @Test
    void rubricSelfExplain_passWhenDimensionsSatisfied() {
        ExecutableTaskSpec.EvaluationRubric rubric = ExecutableTaskSpec.EvaluationRubric.builder()
                .dimensions(Map.of("复述", "能用自己的话说出定义", "例子", "能举出至少一个例子"))
                .passThreshold("覆盖2/2")
                .build();
        ExecutableTaskSpec spec = ExecutableTaskSpec.builder().evaluationRubric(rubric).build();
        EvaluationResult r = evaluator.evaluateSelfExplanation(null, spec,
                "链表就是每个节点包含数据以及指向下一个节点的指针，例如一个节点存1，下一个存2，通过指针连接");
        assertThat(r.isPass()).isTrue();
    }

    @Test
    void rubricSelfExplain_failWhenMissingDimension() {
        ExecutableTaskSpec.EvaluationRubric rubric = ExecutableTaskSpec.EvaluationRubric.builder()
                .dimensions(Map.of("复述", "能用自己的话说出定义", "例子", "能举出至少一个例子"))
                .passThreshold("覆盖2/2")
                .build();
        ExecutableTaskSpec spec = ExecutableTaskSpec.builder().evaluationRubric(rubric).build();
        EvaluationResult r = evaluator.evaluateSelfExplanation(null, spec, "链表就是一种数据结构");
        assertThat(r.isPass()).isFalse();
    }
}
