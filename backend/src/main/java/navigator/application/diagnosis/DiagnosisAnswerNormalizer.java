package navigator.application.diagnosis;

import navigator.domain.model.DiagnosisAnswer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * questionId alias 归一化，答案 code 按 dimension 分组。
 * 支持 6 维诊断题：GOAL_SITUATION, FOUNDATION, GAP, WEAKNESS_SCOPE, PREFERENCE, EXECUTION_RISK。
 */
public final class DiagnosisAnswerNormalizer {

    private static final Map<String, String> QUESTION_ALIAS = Map.ofEntries(
            Map.entry("q_foundation", "q_foundation_state"),
            Map.entry("q_current_state", "q_foundation_state"),
            Map.entry("q_gap", "q_primary_gap"),
            Map.entry("q_main_blocker", "q_primary_gap"),
            Map.entry("q_goal_outcome", "q_goal_outcome"),
            Map.entry("q_foundation_state", "q_foundation_state"),
            Map.entry("q_primary_gap", "q_primary_gap"),
            Map.entry("q_scope_of_problem", "q_scope_of_problem"),
            Map.entry("q_preferred_entry_mode", "q_preferred_entry_mode"),
            Map.entry("q_execution_risk", "q_execution_risk")
    );

    private DiagnosisAnswerNormalizer() {
    }

    /**
     * 归一化 questionId（alias -> 标准 id），返回按 dimension 分组的答案 code。
     */
    public static NormalizedAnswers normalize(List<DiagnosisAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return new NormalizedAnswers(null, null, null, null, null, null);
        }
        String goalOutcomeCode = null;
        String foundationCode = null;
        String gapCode = null;
        String weaknessScopeCode = null;
        String preferenceCode = null;
        String executionRiskCode = null;

        for (DiagnosisAnswer a : answers) {
            String qId = a.getQuestionId() != null ? a.getQuestionId().trim() : null;
            if (qId == null) continue;
            String canonical = QUESTION_ALIAS.getOrDefault(qId, qId);
            List<String> options = a.getSelectedOptions() != null ? a.getSelectedOptions() : Collections.emptyList();
            if (options.isEmpty()) continue;
            String first = options.get(0);

            switch (canonical) {
                case "q_goal_outcome" -> goalOutcomeCode = first;
                case "q_foundation_state" -> foundationCode = first;
                case "q_primary_gap" -> gapCode = first;
                case "q_scope_of_problem" -> weaknessScopeCode = first;
                case "q_preferred_entry_mode" -> preferenceCode = first;
                case "q_execution_risk" -> executionRiskCode = first;
                default -> { /* 未知 questionId 忽略 */ }
            }
        }
        return new NormalizedAnswers(goalOutcomeCode, foundationCode, gapCode,
                weaknessScopeCode, preferenceCode, executionRiskCode);
    }

    public static class NormalizedAnswers {
        private final String goalOutcomeCode;
        private final String foundationCode;
        private final String gapCode;
        private final String weaknessScopeCode;
        private final String preferenceCode;
        private final String executionRiskCode;

        public NormalizedAnswers(String goalOutcomeCode, String foundationCode, String gapCode,
                                String weaknessScopeCode, String preferenceCode, String executionRiskCode) {
            this.goalOutcomeCode = goalOutcomeCode;
            this.foundationCode = foundationCode;
            this.gapCode = gapCode;
            this.weaknessScopeCode = weaknessScopeCode;
            this.preferenceCode = preferenceCode;
            this.executionRiskCode = executionRiskCode;
        }

        public String getGoalOutcomeCode() { return goalOutcomeCode; }
        public String getFoundationCode() { return foundationCode; }
        public String getGapCode() { return gapCode; }
        public String getWeaknessScopeCode() { return weaknessScopeCode; }
        public String getPreferenceCode() { return preferenceCode; }
        public String getExecutionRiskCode() { return executionRiskCode; }

        /** 兼容：gapCodes 以列表形式返回，首项即 primary gap */
        public List<String> getGapCodes() {
            return gapCode != null ? List.of(gapCode) : List.of();
        }
    }
}
