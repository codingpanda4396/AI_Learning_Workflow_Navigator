package navigator.application.scaffold;

import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;
import navigator.domain.model.LearningScaffoldEngineState;
import navigator.domain.model.ScaffoldActionRuntimeEntry;
import navigator.domain.model.ScaffoldAttemptSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionAssemblerTest {

    private final ReflectionAssembler assembler = new ReflectionAssembler();

    @Test
    void assemblesRecordAndInsight() {
        LearningScaffoldEngineState eng = LearningScaffoldEngineState.builder()
                .currentStageKey(DfsBfsReflectionScaffoldDefinition.STAGE_KEY)
                .actionRuntimeByActionId(sampleRuntimeMap())
                .build();

        ReflectionAssembler.ReflectionRecordAndInsight out = assembler.assemble(eng);
        ReflectionRecord r = out.record();
        ReflectionInsight i = out.insight();

        assertTrue(r.getErrorPattern().contains("因果链"));
        assertTrue(r.getRootCause().contains("背术语"));
        assertTrue(r.getDecisionRule().contains("BFS"));
        assertTrue(r.getCapabilityName().contains("能"));
        assertFalse(r.getFutureStrategy().isBlank());

        assertTrue(i.getTotalAttempts() > 0);
        assertEquals(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST, i.getMostDifficultActionId());
    }

    private static Map<String, ScaffoldActionRuntimeEntry> sampleRuntimeMap() {
        Map<String, ScaffoldActionRuntimeEntry> m = new LinkedHashMap<>();

        ScaffoldActionRuntimeEntry t1 = ScaffoldActionRuntimeEntry.builder()
                .actionId(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST)
                .userInput("ok")
                .retryCount(5)
                .attemptNo(3)
                .attemptSnapshots(List.of(
                        snap(1, "FAIL", List.of("CAUSAL_GAP")),
                        snap(2, "FAIL", List.of("CAUSAL_GAP")),
                        snap(3, "PASS", List.of())
                ))
                .build();

        ScaffoldActionRuntimeEntry t2 = ScaffoldActionRuntimeEntry.builder()
                .actionId(DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE)
                .userInput("ok")
                .retryCount(1)
                .attemptNo(1)
                .attemptSnapshots(List.of(snap(1, "PASS", List.of())))
                .build();

        m.put(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST, t1);
        m.put(DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE, t2);

        m.put(DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL,
                entry(DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL,
                        "我常把知道定义当成能解释机制，因果链写不全。"));
        m.put(DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE,
                entry(DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE,
                        "根因是我只会背术语，没有把层进和首次到达串起来。"));
        m.put(DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE,
                entry(DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE,
                        "当问题强调层次扩展、无权最短路径时，我优先想到 BFS；当需要深度试探时，我优先想到 DFS。"));
        m.put(DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME,
                entry(DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME,
                        "我能根据搜索推进方式解释 DFS/BFS 的适用差异，并把 BFS 最短路的因果链讲清楚。"));

        return m;
    }

    private static ScaffoldActionRuntimeEntry entry(String id, String text) {
        return ScaffoldActionRuntimeEntry.builder()
                .actionId(id)
                .userInput(text)
                .retryCount(0)
                .attemptNo(1)
                .attemptSnapshots(List.of())
                .build();
    }

    private static ScaffoldAttemptSnapshot snap(int no, String sum, List<String> err) {
        return ScaffoldAttemptSnapshot.builder()
                .attemptNo(no)
                .userInput("x")
                .submittedAt(0L)
                .validationSummary(sum)
                .tutorSummary("")
                .runtimeStatus("x")
                .errorTypes(err)
                .build();
    }
}
