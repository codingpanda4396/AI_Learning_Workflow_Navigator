package navigator.application.scaffold;

import navigator.api.dto.scaffold.ReflectionInsight;
import navigator.api.dto.scaffold.ReflectionRecord;
import navigator.domain.model.LearningScaffoldEngineState;
import navigator.domain.model.ScaffoldActionRuntimeEntry;
import navigator.domain.model.ScaffoldAttemptSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 合并用户四卡输入与 TRAINING 过程证据，生成沉淀与下一步策略。
 */
@Component
public class ReflectionAssembler {

    public ReflectionRecordAndInsight assemble(LearningScaffoldEngineState eng) {
        String e1 = text(eng, DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL);
        String e2 = text(eng, DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE);
        String e3 = text(eng, DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE);
        String e4 = text(eng, DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME);

        ReflectionInsight insight = buildInsight(eng);
        String future = buildFutureStrategy(insight, e3);

        ReflectionRecord record = ReflectionRecord.builder()
                .errorPattern(e1)
                .rootCause(e2)
                .decisionRule(e3)
                .capabilityName(e4)
                .futureStrategy(future)
                .build();
        return new ReflectionRecordAndInsight(record, insight);
    }

    private static String text(LearningScaffoldEngineState eng, String actionId) {
        ScaffoldActionRuntimeEntry e = eng.getActionRuntimeByActionId() != null
                ? eng.getActionRuntimeByActionId().get(actionId)
                : null;
        if (e == null || e.getUserInput() == null) {
            return "";
        }
        return e.getUserInput().trim();
    }

    private static ReflectionInsight buildInsight(LearningScaffoldEngineState eng) {
        List<String> trainingIds = DfsBfsTrainingScaffoldDefinition.orderedActionIds();
        Map<String, Integer> retries = new HashMap<>();
        int totalAttempts = 0;
        Map<String, Integer> typeCount = new HashMap<>();

        for (String tid : trainingIds) {
            ScaffoldActionRuntimeEntry entry = eng.getActionRuntimeByActionId() != null
                    ? eng.getActionRuntimeByActionId().get(tid)
                    : null;
            if (entry == null) {
                continue;
            }
            retries.put(tid, entry.getRetryCount());
            totalAttempts += Math.max(0, entry.getAttemptNo());
            List<ScaffoldAttemptSnapshot> snaps = entry.getAttemptSnapshots();
            if (snaps == null) {
                continue;
            }
            for (ScaffoldAttemptSnapshot s : snaps) {
                if (s.getErrorTypes() == null) {
                    continue;
                }
                for (String et : s.getErrorTypes()) {
                    if (et == null || et.isBlank()) {
                        continue;
                    }
                    typeCount.merge(et, 1, Integer::sum);
                }
            }
        }

        List<String> repeated = typeCount.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        String hardest = trainingIds.stream()
                .max(Comparator
                        .comparing((String id) -> retries.getOrDefault(id, 0))
                        .thenComparing(Comparator.reverseOrder()))
                .orElse(DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST);

        List<String> improved = new ArrayList<>();
        for (String tid : trainingIds) {
            ScaffoldActionRuntimeEntry entry = eng.getActionRuntimeByActionId() != null
                    ? eng.getActionRuntimeByActionId().get(tid)
                    : null;
            if (entry == null || entry.getAttemptSnapshots() == null || entry.getAttemptSnapshots().size() < 2) {
                continue;
            }
            List<ScaffoldAttemptSnapshot> snaps = entry.getAttemptSnapshots();
            boolean hadFail = snaps.stream().anyMatch(s -> s.getValidationSummary() != null
                    && !s.getValidationSummary().equalsIgnoreCase("PASS"));
            boolean lastPass = snaps.get(snaps.size() - 1).getValidationSummary() != null
                    && snaps.get(snaps.size() - 1).getValidationSummary().toUpperCase(Locale.ROOT).contains("PASS");
            if (hadFail && lastPass) {
                improved.add("卡 " + shortActionLabel(tid) + "：多轮后最终通过");
            }
        }

        return ReflectionInsight.builder()
                .repeatedErrorTypes(repeated)
                .mostDifficultActionId(hardest)
                .totalAttempts(totalAttempts)
                .improvedAspects(improved)
                .build();
    }

    private static String shortActionLabel(String actionId) {
        if (DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST.equals(actionId)) {
            return "BFS 最短路径";
        }
        if (DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE.equals(actionId)) {
            return "顺序与后果";
        }
        return actionId;
    }

    private static String buildFutureStrategy(ReflectionInsight insight, String userRule) {
        Set<String> parts = new LinkedHashSet<>();
        if (!insight.getRepeatedErrorTypes().isEmpty()) {
            parts.add("针对训练中反复出现的「" + String.join("、", insight.getRepeatedErrorTypes()) + "」再做一次短写练习。");
        }
        if (insight.getTotalAttempts() >= 6) {
            parts.add("表达已多轮打磨，下一步用一道新题口头复述规则，检验迁移。");
        }
        if (userRule != null && userRule.length() > 20) {
            parts.add("把你在规律卡里写的那条判断句，用在下一题的第一反应里。");
        }
        if (parts.isEmpty()) {
            return "保持：用场景题验证你的判断规则，并口头讲清一条因果链。";
        }
        return String.join(" ", parts);
    }

    public record ReflectionRecordAndInsight(ReflectionRecord record, ReflectionInsight insight) {
    }
}
