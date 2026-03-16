package navigator.application.diagnosis;

import navigator.domain.model.DiagnosisAnswer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sprint 1: questionId alias 归一化，答案 code 标准化。
 */
public final class DiagnosisAnswerNormalizer {

    private static final Map<String, String> QUESTION_ALIAS = Map.of(
            "q_foundation", "q_foundation",
            "q_current_state", "q_foundation",
            "q_gap", "q_gap",
            "q_main_blocker", "q_gap"
    );

    private DiagnosisAnswerNormalizer() {
    }

    /**
     * 归一化 questionId（alias -> 标准 id），返回按 dimension 分组的答案 code。
     * key: "foundation" | "gap", value: 选中的 code 列表（第一项为主选）。
     */
    public static NormalizedAnswers normalize(List<DiagnosisAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return new NormalizedAnswers(null, null);
        }
        String foundationCode = null;
        List<String> gapCodes = null;
        for (DiagnosisAnswer a : answers) {
            String qId = a.getQuestionId() != null ? a.getQuestionId().trim() : null;
            if (qId == null) continue;
            String canonical = QUESTION_ALIAS.getOrDefault(qId, qId);
            List<String> options = a.getSelectedOptions() != null ? a.getSelectedOptions() : Collections.emptyList();
            if (options.isEmpty()) continue;
            String first = options.get(0);
            if (canonical.equals("q_foundation")) {
                foundationCode = first;
            } else if (canonical.equals("q_gap")) {
                gapCodes = options.stream().limit(2).collect(Collectors.toList());
            }
        }
        return new NormalizedAnswers(foundationCode, gapCodes);
    }

    public static class NormalizedAnswers {
        private final String foundationCode;
        private final List<String> gapCodes;

        public NormalizedAnswers(String foundationCode, List<String> gapCodes) {
            this.foundationCode = foundationCode;
            this.gapCodes = gapCodes != null ? gapCodes : List.of();
        }

        public String getFoundationCode() { return foundationCode; }
        public List<String> getGapCodes() { return gapCodes; }
    }
}
