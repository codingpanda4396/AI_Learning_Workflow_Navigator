package navigator.application.task.guidance;

import navigator.application.learning.LearningEvidenceBuilder;
import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.application.rule.guidance.rules.BuildFrameRule;
import navigator.application.rule.guidance.rules.ClarifyGoalRule;
import navigator.application.rule.guidance.rules.MetaReflectRule;
import navigator.application.rule.guidance.rules.ProbeGapsRule;
import navigator.application.rule.guidance.rules.TransitionHintRule;
import navigator.application.rule.guidance.rules.TryExpressRule;
import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.enums.TaskExecutionState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuidancePhaseTransitionEvaluatorTest {

    private final GuidancePhaseTransitionEvaluator evaluator =
            new GuidancePhaseTransitionEvaluator(new LearningEvidenceBuilder(), new GuidanceRuleEngine(defaultRules()));

    @Test
    void shouldChooseClarifyGoalForFirstGenericTurn() {
        TaskExecutionRuntime runtime = runtimeWithTurns(0);

        var result = evaluator.evaluate(runtime, LearningActionType.GENERIC, "链表到底是啥");

        assertThat(result.getResult()).isEqualTo(LearningGuidancePhase.CLARIFY_GOAL);
        assertThat(result.getRuleId()).isEqualTo("CLARIFY_GOAL_RULE");
    }

    @Test
    void shouldChooseProbeGapsWhenUserShowsConfusion() {
        TaskExecutionRuntime runtime = runtimeWithTurns(2);

        var result = evaluator.evaluate(runtime, LearningActionType.CONFUSION_SIGNAL, "我还是不懂这个步骤");

        assertThat(result.getResult()).isEqualTo(LearningGuidancePhase.PROBE_GAPS);
        assertThat(result.getRuleId()).isEqualTo("PROBE_GAPS_RULE");
    }

    @Test
    void shouldChooseMetaReflectForGoodSelfExplanationEvenWithLargeTurnCount() {
        TaskExecutionRuntime runtime = runtimeWithTurns(8);
        runtime.setExploreTurnCount(99);

        var result = evaluator.evaluate(runtime, LearningActionType.SELF_EXPLANATION, "我理解这个概念的定义，因为它说明了节点之间的关系");

        assertThat(result.getResult()).isEqualTo(LearningGuidancePhase.META_REFLECT);
        assertThat(result.getRuleId()).isEqualTo("META_REFLECT_RULE");
    }

    @Test
    void taskGuidanceEngineShouldSyncPhaseFromEvidenceNotTurnCount() {
        TaskGuidanceEngine engine = new TaskGuidanceEngine(
                new navigator.domain.policy.tutor.TutorInteractionPolicy(),
                evaluator
        );
        TaskExecutionRuntime runtime = runtimeWithTurns(1);
        runtime.setState(TaskExecutionState.EXPLORE);
        runtime.setExploreTurnCount(50);

        var result = engine.syncGuidancePhase(runtime, LearningActionType.SELF_EXPLANATION,
                "我理解这个概念的关键在于先定义边界，再说明关系");

        assertThat(runtime.getGuidancePhase()).isEqualTo(LearningGuidancePhase.META_REFLECT);
        assertThat(result.getReason()).contains("articulated key concepts");
    }

    private static TaskExecutionRuntime runtimeWithTurns(int totalTurns) {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime();
        runtime.setState(TaskExecutionState.EXPLORE);
        TaskExecutionEvidenceAccumulator.ensureSnapshot(runtime).setTotalTurns(totalTurns);
        return runtime;
    }

    private static List<GuidancePhaseRule> defaultRules() {
        return List.of(
                new ClarifyGoalRule(),
                new BuildFrameRule(),
                new TryExpressRule(),
                new ProbeGapsRule(),
                new MetaReflectRule(),
                new TransitionHintRule()
        );
    }
}
