package navigator.application.task;

import navigator.domain.enums.LearningActionType;
import navigator.domain.model.LearningMethodProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 从执行运行时聚合单任务学习方法画像。
 */
public final class LearningMethodProfileAggregator {

    private LearningMethodProfileAggregator() {}

    public static LearningMethodProfile aggregate(String sessionId, String taskId, TaskExecutionRuntime rt) {
        List<LearningActionType> actions = rt.getActionHistory();
        Map<LearningActionType, Long> counts = actions.stream()
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        long askN = counts.getOrDefault(LearningActionType.ASK_FOR_EXPLANATION, 0L)
                + counts.getOrDefault(LearningActionType.ASK_FOR_EXAMPLE, 0L)
                + counts.getOrDefault(LearningActionType.ASK_FOR_COMPARISON, 0L)
                + counts.getOrDefault(LearningActionType.ASK_FOR_SIMPLIFICATION, 0L);
        String questioning = askN >= 3 ? "GOOD" : (askN >= 1 ? "BASIC" : "LOW");
        boolean selfDone = "ACCEPTABLE".equals(rt.getSelfExplanationEvaluation())
                || "GOOD".equals(rt.getSelfExplanationEvaluation())
                || "WEAK".equals(rt.getSelfExplanationEvaluation());
        String selfQ = rt.getSelfExplanationEvaluation() != null ? rt.getSelfExplanationEvaluation() : "NONE";
        Boolean checkOk = rt.getState().name().equals("PASS") ? Boolean.TRUE : Boolean.FALSE;

        List<String> anti = new ArrayList<>();
        if (counts.getOrDefault(LearningActionType.SEEK_DIRECT_ANSWER, 0L) > 0) {
            anti.add("多次试图直接要答案");
        }
        if (counts.getOrDefault(LearningActionType.OFF_TOPIC, 0L) > 0) {
            anti.add("出现跑题倾向");
        }
        List<String> pos = new ArrayList<>();
        if (askN > 0) pos.add("主动提出澄清类问题");
        if (selfDone) pos.add("完成自我解释环节");
        if (Boolean.TRUE.equals(checkOk)) pos.add("微检查通过");

        List<String> dominant = counts.entrySet().stream()
                .filter(e -> e.getValue() > 0 && e.getKey() != LearningActionType.GENERIC)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> e.getKey().name())
                .collect(Collectors.toList());

        List<String> advice = new ArrayList<>();
        if ("LOW".equals(questioning)) {
            advice.add("下一轮可多使用「举例 / 对比 / 更简单」类提问。");
        }
        if (!Boolean.TRUE.equals(checkOk)) {
            advice.add("完成自解释后再进入微检查，避免跳过理解环节。");
        }
        if (advice.isEmpty()) {
            advice.add("保持当前提问节奏，继续用自我复述巩固理解。");
        }

        var profileBuilder = LearningMethodProfile.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .questioningQuality(questioning)
                .selfExplanationPerformed(selfDone)
                .selfExplanationQuality(selfQ)
                .checkPassed(checkOk)
                .antiPatternObserved(anti.isEmpty() ? List.of() : anti)
                .positiveSignals(pos.isEmpty() ? List.of("已参与结构化任务流") : pos)
                .dominantActionTypes(dominant.isEmpty() ? List.of("GENERIC") : dominant)
                .nextMethodAdvice(advice);
        if (rt.getEvidenceSnapshot() != null) {
            var ev = rt.getEvidenceSnapshot();
            profileBuilder.directAnswerDependencyScore(ev.getDirectAnswerDependencyScore());
            int n = ev.getCompletedGuidancePhases() != null ? ev.getCompletedGuidancePhases().size() : 0;
            profileBuilder.guidancePhaseCoverage(n + " phases touched");
            profileBuilder.hintBurden(ev.getAssistantHintTurns() > ev.getUserInitiatedQuestionTurns() + 2 ? "HIGH" : "OK");
        }
        return profileBuilder.build();
    }

    /** 会话级汇总（报告用） */
    public static LearningMethodProfile aggregateSession(String sessionId, List<LearningMethodProfile> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return LearningMethodProfile.builder()
                    .sessionId(sessionId)
                    .taskId("_session")
                    .questioningQuality("UNKNOWN")
                    .selfExplanationPerformed(false)
                    .selfExplanationQuality("NONE")
                    .checkPassed(false)
                    .antiPatternObserved(List.of())
                    .positiveSignals(List.of())
                    .dominantActionTypes(List.of())
                    .nextMethodAdvice(List.of("完成带脚手架的任务流后可获得更细的方法反馈。"))
                    .build();
        }
        int goodQ = 0, basicQ = 0;
        int self = 0, pass = 0;
        List<String> allAnti = new ArrayList<>();
        List<String> allAdvice = new ArrayList<>();
        double depSum = 0;
        int depC = 0;
        boolean anyHintHigh = false;
        for (LearningMethodProfile p : tasks) {
            if ("GOOD".equals(p.getQuestioningQuality())) goodQ++;
            else if ("BASIC".equals(p.getQuestioningQuality())) basicQ++;
            if (Boolean.TRUE.equals(p.getSelfExplanationPerformed())) self++;
            if (Boolean.TRUE.equals(p.getCheckPassed())) pass++;
            if (p.getAntiPatternObserved() != null) allAnti.addAll(p.getAntiPatternObserved());
            if (p.getNextMethodAdvice() != null) allAdvice.addAll(p.getNextMethodAdvice());
            if (p.getDirectAnswerDependencyScore() != null) {
                depSum += p.getDirectAnswerDependencyScore();
                depC++;
            }
            if ("HIGH".equals(p.getHintBurden())) {
                anyHintHigh = true;
            }
        }
        String q = goodQ >= tasks.size() / 2 ? "GOOD" : (basicQ + goodQ > 0 ? "BASIC" : "LOW");
        return LearningMethodProfile.builder()
                .sessionId(sessionId)
                .taskId("_session")
                .questioningQuality(q)
                .selfExplanationPerformed(self > 0)
                .selfExplanationQuality(self >= tasks.size() ? "GOOD" : (self > 0 ? "ACCEPTABLE" : "NONE"))
                .checkPassed(pass == tasks.size())
                .antiPatternObserved(allAnti.stream().distinct().limit(5).toList())
                .positiveSignals(List.of(
                        "完成任务数：" + tasks.size(),
                        "其中完成自解释：" + self,
                        "微检查通过任务：" + pass))
                .dominantActionTypes(List.of())
                .nextMethodAdvice(allAdvice.stream().distinct().limit(3).toList())
                .directAnswerDependencyScore(depC > 0 ? depSum / depC : null)
                .guidancePhaseCoverage("session rollup")
                .hintBurden(anyHintHigh ? "HIGH" : "OK")
                .build();
    }
}
