package navigator.application.learning;

import navigator.application.task.TaskExecutionRuntime;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.domain.enums.LearningActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearningEvidenceBuilderTest {

    private final LearningEvidenceBuilder builder = new LearningEvidenceBuilder();

    @Test
    void shouldBuildEvidenceFromRuntimeActionAndInput() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime();
        TaskExecutionEvidenceAccumulator.ensureSnapshot(runtime).setTotalTurns(2);

        var evidence = builder.build(runtime, LearningActionType.SELF_EXPLANATION, "我理解这个概念是因为它定义了节点关系");

        assertThat(evidence.isAttemptedExplain()).isTrue();
        assertThat(evidence.isContainsKeyConcept()).isTrue();
        assertThat(evidence.isConfusedSignal()).isFalse();
        assertThat(evidence.getInteractionDepth()).isEqualTo(3);
    }

    @Test
    void shouldDetectDirectAnswerAndConfusionSignals() {
        var evidence = builder.build("直接给我答案吧，我还是不懂");

        assertThat(evidence.isAskingForAnswer()).isTrue();
        assertThat(evidence.isConfusedSignal()).isTrue();
    }
}
