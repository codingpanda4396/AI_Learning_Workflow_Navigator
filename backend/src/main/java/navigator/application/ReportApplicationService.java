package navigator.application;

import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.NextActionConfirmData;
import navigator.api.dto.ReportData;
import navigator.application.guard.SessionStateGuard;
import navigator.application.report.SessionEvidenceAggregator;
import navigator.application.task.LearningMethodProfileAggregator;
import navigator.domain.enums.NextActionType;
import navigator.domain.enums.ResultStatus;
import navigator.domain.model.ExecutionEvidenceSummary;
import navigator.domain.model.LearningMethodProfile;
import navigator.domain.model.LearningMethodReview;
import navigator.domain.model.LearningReport;
import navigator.domain.model.NextActionDecision;
import navigator.domain.model.RecommendedNextStep;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.model.TaskHighlight;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import navigator.infrastructure.persistence.repository.TaskMethodProfileRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportApplicationService {

    private final SessionStateGuard sessionStateGuard;
    private final SessionEvidenceAggregator evidenceAggregator;
    private final InMemoryStore store;
    private final TaskMethodProfileRepository taskMethodProfileRepository;
    private final JsonSerde jsonSerde;

    public ReportApplicationService(SessionStateGuard sessionStateGuard,
                                    SessionEvidenceAggregator evidenceAggregator,
                                    InMemoryStore store,
                                    TaskMethodProfileRepository taskMethodProfileRepository,
                                    JsonSerde jsonSerde) {
        this.sessionStateGuard = sessionStateGuard;
        this.evidenceAggregator = evidenceAggregator;
        this.store = store;
        this.taskMethodProfileRepository = taskMethodProfileRepository;
        this.jsonSerde = jsonSerde;
    }

    public ReportData getReport(String sessionId) {
        sessionStateGuard.requireSessionCompletedForReport(sessionId);
        Long sessionDbId = extractNumericId(sessionId);
        SessionEvidenceAggregator.AggregatedSessionEvidence aggregated = evidenceAggregator.aggregate(sessionDbId);
        ExecutionEvidenceSummary execution = aggregated.execution();

        List<LearningMethodProfile> methodTasks = loadMethodProfiles(sessionId);
        LearningMethodProfile methodRollup = LearningMethodProfileAggregator.aggregateSession(sessionId, methodTasks);
        LearningReport report = buildLearningReport(sessionId, aggregated, execution, methodRollup);
        return ReportData.builder()
                .learningReport(report)
                .nextActionDecision(report.getNextAction())
                .build();
    }

    public NextActionConfirmData confirmNextAction(String sessionId, NextActionType actionType) {
        sessionStateGuard.requireSessionCompletedForReport(sessionId);
        Long sessionDbId = extractNumericId(sessionId);
        SessionEvidenceAggregator.AggregatedSessionEvidence aggregated = evidenceAggregator.aggregate(sessionDbId);
        LearningMethodProfile methodRollup = LearningMethodProfileAggregator.aggregateSession(sessionId, loadMethodProfiles(sessionId));
        NextActionDecision decision = buildNextActionDecision(aggregated.execution(), methodRollup);

        boolean hasGap = aggregated.execution().getAggregatedIssueTags() != null
                && !aggregated.execution().getAggregatedIssueTags().isEmpty();
        boolean requiresReplan = hasGap && (actionType == NextActionType.REMEDIATE_PREREQUISITE
                || actionType == NextActionType.CHANGE_STRATEGY);

        String nextHint;
        if (actionType == NextActionType.CONTINUE) {
            nextHint = "本轮可以收口，下一步直接进入新的相关知识点。";
        } else if (actionType == NextActionType.REINFORCE) {
            nextHint = "先围绕这轮暴露出的薄弱点补一轮轻量巩固，再继续推进。";
        } else if (actionType == NextActionType.REMEDIATE_PREREQUISITE) {
            nextHint = "建议先补前置概念，再回到当前主题，避免继续带着缺口往下走。";
        } else if (actionType == NextActionType.REDUCE_GRANULARITY) {
            nextHint = "把下一轮任务拆得更小，每次只验证一个判断点，会更容易稳定推进。";
        } else {
            nextHint = "下一轮先换学习方式，再继续推进，会比硬撑当前节奏更有效。";
        }

        if (decision.getActionType() == actionType && decision.getReason() != null && !decision.getReason().isBlank()) {
            nextHint = decision.getReason() + " " + nextHint;
        }

        return NextActionConfirmData.builder()
                .sessionId(sessionId)
                .acceptedAction(actionType)
                .requiresReplan(requiresReplan)
                .nextHint(nextHint)
                .build();
    }

    private LearningReport buildLearningReport(String sessionId,
                                               SessionEvidenceAggregator.AggregatedSessionEvidence aggregated,
                                               ExecutionEvidenceSummary execution,
                                               LearningMethodProfile methodRollup) {
        int totalTasks = safeInt(execution.getTotalTasks());
        int completedTasks = safeInt(execution.getCompletedTasks());

        ResultStatus status = resolveResultStatus(execution, methodRollup);
        String goalReview = resolveGoalReview(sessionId);
        List<CompletionFact> facts = buildCompletionFacts(aggregated.tasks(), aggregated.completions());
        List<String> whatYouLearned = buildWhatYouLearned(facts);
        List<String> whatStillNeedsWork = buildWhatStillNeedsWork(facts, execution, methodRollup);
        List<String> evidenceDigest = buildEvidenceDigest(execution, completedTasks, totalTasks);
        LearningMethodReview learningMethodReview = buildLearningMethodReview(methodRollup, execution);
        NextActionDecision nextAction = buildNextActionDecision(execution, methodRollup);
        RecommendedNextStep recommendedNextStep = buildRecommendedNextStep(nextAction);
        List<TaskHighlight> taskHighlights = buildTaskHighlights(aggregated.tasks(), facts);
        String finalSummary = buildFinalSummary(status, completedTasks, totalTasks, whatYouLearned, whatStillNeedsWork);

        return LearningReport.builder()
                .sessionId(sessionId)
                .resultStatus(status)
                .goalReview(goalReview)
                .finalSummary(finalSummary)
                .whatYouLearned(whatYouLearned)
                .whatStillNeedsWork(whatStillNeedsWork)
                .evidenceDigest(evidenceDigest)
                .learningMethodReview(learningMethodReview)
                .recommendedNextStep(recommendedNextStep)
                .taskHighlights(taskHighlights)
                .completedProgress(evidenceDigest)
                .unresolvedIssues(whatStillNeedsWork)
                .evidenceSummary(evidenceDigest)
                .summaryText(finalSummary)
                .nextAction(nextAction)
                .learningMethodProfile(methodRollup)
                .build();
    }

    private ResultStatus resolveResultStatus(ExecutionEvidenceSummary execution,
                                             LearningMethodProfile methodRollup) {
        int totalTasks = safeInt(execution.getTotalTasks());
        int completedTasks = safeInt(execution.getCompletedTasks());
        boolean hasGap = execution.getAggregatedIssueTags() != null && !execution.getAggregatedIssueTags().isEmpty();
        boolean closureWeak = execution.getClosureQualityScore() != null && execution.getClosureQualityScore() < 60;
        boolean highDependency = methodRollup != null
                && methodRollup.getDirectAnswerDependencyScore() != null
                && methodRollup.getDirectAnswerDependencyScore() > 0.55;

        if (totalTasks == 0 || completedTasks == 0) {
            return ResultStatus.NOT_ACHIEVED;
        }
        if (completedTasks >= totalTasks && !hasGap && !closureWeak && !highDependency) {
            return ResultStatus.ACHIEVED;
        }
        if (completedTasks > 0) {
            return ResultStatus.PARTIALLY_ACHIEVED;
        }
        return ResultStatus.NOT_ACHIEVED;
    }

    private String resolveGoalReview(String sessionId) {
        StructuredLearningGoal goal = null;
        var state = store.getSessions().get(sessionId);
        if (state != null && state.getPlanId() != null) {
            var preview = store.getPlanPreviews().get(state.getPlanId());
            if (preview != null && preview.getGoalId() != null) {
                goal = store.getGoals().get(preview.getGoalId());
            }
        }
        if (goal == null) {
            return "本轮报告围绕当前学习计划的执行结果生成。";
        }
        String base = firstNonBlank(goal.getRawGoalText(), goal.getNormalizedGoalText(), goal.getSubject());
        if (base == null) {
            return "本轮报告围绕当前学习计划的执行结果生成。";
        }
        return "本轮目标：" + truncate(base, 80);
    }

    private List<CompletionFact> buildCompletionFacts(List<SessionTaskEntity> tasks, List<TaskCompletionEntity> completions) {
        Map<Long, SessionTaskEntity> taskMap = new LinkedHashMap<>();
        for (SessionTaskEntity task : tasks) {
            if (task.getId() != null) {
                taskMap.put(task.getId(), task);
            }
        }
        List<CompletionFact> facts = new ArrayList<>();
        for (TaskCompletionEntity completion : completions) {
            CompleteTaskRequest input = parseCompletionInput(completion.getCompletionInputJson());
            facts.add(new CompletionFact(taskMap.get(completion.getTaskId()), completion, input));
        }
        facts.sort(Comparator.comparingInt(f -> f.task() != null && f.task().getOrderIndex() != null ? f.task().getOrderIndex() : Integer.MAX_VALUE));
        return facts;
    }

    private List<String> buildWhatYouLearned(List<CompletionFact> facts) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        for (CompletionFact fact : facts) {
            CompleteTaskRequest input = fact.input();
            if (input == null) {
                continue;
            }
            addAllNonBlank(points, input.getLearnedFrameworkPoints());
            addNonBlank(points, firstSentence(input.getSummaryText()));
            addNonBlank(points, firstSentence(input.getLearnerReflection()));
            if (points.size() >= 4) {
                break;
            }
        }
        if (points.isEmpty()) {
            points.add("本轮已经完成一轮结构化学习，可以进入结果回顾与下一步判断。");
        }
        return points.stream().limit(4).toList();
    }

    private List<String> buildWhatStillNeedsWork(List<CompletionFact> facts,
                                                 ExecutionEvidenceSummary execution,
                                                 LearningMethodProfile methodRollup) {
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        for (CompletionFact fact : facts) {
            CompleteTaskRequest input = fact.input();
            if (input == null) {
                continue;
            }
            addAllNonBlank(issues, input.getUnresolvedQuestions());
        }
        addAllNonBlank(issues, execution.getAggregatedIssueTags());
        if (execution.getClosureQualityScore() != null && execution.getClosureQualityScore() < 60) {
            issues.add("本轮收束还不够完整，建议把总结、框架点和下一练习再说清楚。");
        }
        if (methodRollup != null && methodRollup.getDirectAnswerDependencyScore() != null
                && methodRollup.getDirectAnswerDependencyScore() > 0.55) {
            issues.add("对直接答案的依赖偏高，下一轮要更多先说自己的判断。");
        }
        if (issues.isEmpty()) {
            issues.add("本轮没有明显硬伤，下一步重点是把当前理解转成稳定应用。");
        }
        return issues.stream().limit(3).toList();
    }

    private List<String> buildEvidenceDigest(ExecutionEvidenceSummary execution,
                                             int completedTasks,
                                             int totalTasks) {
        List<String> list = new ArrayList<>();
        list.add("完成 " + completedTasks + "/" + totalTasks + " 个任务");
        if (execution.getTotalDurationMinutes() != null && execution.getTotalDurationMinutes() > 0) {
            list.add("累计投入 " + execution.getTotalDurationMinutes() + " 分钟");
        }
        if (execution.getTotalInteractionCount() != null && execution.getTotalInteractionCount() > 0) {
            list.add("共发生 " + execution.getTotalInteractionCount() + " 次关键互动");
        }
        if (execution.getTotalUserQuestionTurns() != null && execution.getTotalUserQuestionTurns() > 0) {
            list.add("主动提问 " + execution.getTotalUserQuestionTurns() + " 次");
        }
        if (execution.getClosureQualityScore() != null) {
            list.add("收束完整度 " + execution.getClosureQualityScore() + "/100");
        }
        return list;
    }

    private LearningMethodReview buildLearningMethodReview(LearningMethodProfile methodRollup,
                                                           ExecutionEvidenceSummary execution) {
        if (methodRollup == null || "UNKNOWN".equals(methodRollup.getQuestioningQuality())) {
            return LearningMethodReview.builder()
                    .headline("本轮已有执行记录")
                    .summary("后续完成更多带脚手架的任务后，系统会给出更细的学习方式反馈。")
                    .strengths(List.of("执行链路已经闭合"))
                    .risks(List.of())
                    .nextFocus(List.of("下一轮继续保留自我解释和收束动作"))
                    .build();
        }

        String headline;
        if ("GOOD".equals(methodRollup.getQuestioningQuality()) && Boolean.TRUE.equals(methodRollup.getCheckPassed())) {
            headline = "学习方式比较稳";
        } else if ("LOW".equals(methodRollup.getQuestioningQuality())) {
            headline = "学习推进偏被动";
        } else {
            headline = "学习方式基本可用";
        }

        String summary = switch (headline) {
            case "学习方式比较稳" -> "你这轮既有主动追问，也完成了关键检查，说明理解不是只停留在看懂。";
            case "学习推进偏被动" -> "这轮能完成任务，但更像在跟着系统走，下一轮要主动暴露自己的判断。";
            default -> "这轮已经形成基本学习节奏，下一步要继续降低提示依赖。";
        };

        List<String> strengths = new ArrayList<>();
        addAllNonBlank(strengths, methodRollup.getPositiveSignals());
        if (Boolean.TRUE.equals(methodRollup.getSelfExplanationPerformed())) {
            strengths.add("完成了自我解释");
        }
        if (Boolean.TRUE.equals(methodRollup.getCheckPassed())) {
            strengths.add("通过了关键检查");
        }

        List<String> risks = new ArrayList<>();
        addAllNonBlank(risks, methodRollup.getAntiPatternObserved());
        if (execution.getTotalVagueReplies() != null && execution.getTotalVagueReplies() > 1) {
            risks.add("表达还有偏模糊的时候");
        }
        if (methodRollup.getDirectAnswerDependencyScore() != null && methodRollup.getDirectAnswerDependencyScore() > 0.55) {
            risks.add("直接索要答案的倾向偏强");
        }

        List<String> nextFocus = new ArrayList<>();
        addAllNonBlank(nextFocus, methodRollup.getNextMethodAdvice());
        if (nextFocus.isEmpty()) {
            nextFocus.add("下一轮继续先说自己的理解，再用系统反馈校正。");
        }

        return LearningMethodReview.builder()
                .headline(headline)
                .summary(summary)
                .strengths(strengths.stream().distinct().limit(3).toList())
                .risks(risks.stream().distinct().limit(3).toList())
                .nextFocus(nextFocus.stream().distinct().limit(3).toList())
                .build();
    }

    private NextActionDecision buildNextActionDecision(ExecutionEvidenceSummary execution,
                                                       LearningMethodProfile methodRollup) {
        int totalTasks = safeInt(execution.getTotalTasks());
        int completedTasks = safeInt(execution.getCompletedTasks());
        boolean hasGap = execution.getAggregatedIssueTags() != null
                && !execution.getAggregatedIssueTags().isEmpty();
        boolean methodWeak = methodRollup != null && "LOW".equals(methodRollup.getQuestioningQuality());
        if (!methodWeak && completedTasks > 0 && !execution.isSummarySubmitted()) {
            methodWeak = true;
        }
        boolean highDirectAnswerDep = methodRollup != null && methodRollup.getDirectAnswerDependencyScore() != null
                && methodRollup.getDirectAnswerDependencyScore() > 0.55;

        NextActionType actionType;
        boolean requiresReplan;
        String reason;
        String nextEntryPoint;

        if (!hasGap && totalTasks > 0 && completedTasks >= totalTasks && highDirectAnswerDep) {
            actionType = NextActionType.CHANGE_STRATEGY;
            requiresReplan = false;
            reason = "任务已经完成，但下一轮更该先优化学习方式，降低对直接答案的依赖。";
            nextEntryPoint = "先做一轮更强调自我表达和主动提问的任务。";
        } else if (!hasGap && totalTasks > 0 && completedTasks >= totalTasks && !methodWeak) {
            actionType = NextActionType.CONTINUE;
            requiresReplan = false;
            reason = "本轮结果比较完整，可以继续推进到下一个相关知识点。";
            nextEntryPoint = "进入下一知识点或更高一级练习。";
        } else if (hasGap && completedTasks > 0) {
            actionType = NextActionType.REINFORCE;
            requiresReplan = false;
            reason = "这轮已经打下基础，但还有薄弱点，最适合先做一轮巩固。";
            nextEntryPoint = "围绕当前薄弱点安排一轮针对性补练。";
        } else if (methodWeak && completedTasks >= totalTasks) {
            actionType = NextActionType.CHANGE_STRATEGY;
            requiresReplan = false;
            reason = "任务做完了，但方法上还偏被动，下一轮建议换成更强结构化的学习方式。";
            nextEntryPoint = "下一轮优先使用对比、举例、自解释这类动作推进。";
        } else {
            actionType = NextActionType.REMEDIATE_PREREQUISITE;
            requiresReplan = true;
            reason = "当前完成度和稳定度都不够，先回补前置概念会更有效。";
            nextEntryPoint = "先补前置，再重新进入当前主题。";
        }

        List<String> adjustmentSignals = execution.getAggregatedIssueTags() != null
                ? execution.getAggregatedIssueTags()
                : List.of();

        return NextActionDecision.builder()
                .actionType(actionType)
                .reason(reason)
                .nextEntryPoint(nextEntryPoint)
                .adjustmentSignals(adjustmentSignals)
                .requiresReplan(requiresReplan)
                .build();
    }

    private RecommendedNextStep buildRecommendedNextStep(NextActionDecision nextAction) {
        String title = switch (nextAction.getActionType()) {
            case CONTINUE -> "继续推进";
            case REINFORCE -> "先做巩固";
            case REMEDIATE_PREREQUISITE -> "先补前置";
            case REDUCE_GRANULARITY -> "把下一轮拆小";
            case CHANGE_STRATEGY -> "换一种学法";
        };
        String actionLabel = switch (nextAction.getActionType()) {
            case CONTINUE -> "继续下一步";
            case REINFORCE -> "进入巩固";
            case REMEDIATE_PREREQUISITE -> "回补前置";
            case REDUCE_GRANULARITY -> "改成小步推进";
            case CHANGE_STRATEGY -> "调整学习方式";
        };
        return RecommendedNextStep.builder()
                .actionType(nextAction.getActionType())
                .title(title)
                .reason(nextAction.getReason())
                .actionLabel(actionLabel)
                .nextEntryPoint(nextAction.getNextEntryPoint())
                .signals(nextAction.getAdjustmentSignals())
                .requiresReplan(nextAction.isRequiresReplan())
                .build();
    }

    private List<TaskHighlight> buildTaskHighlights(List<SessionTaskEntity> tasks, List<CompletionFact> facts) {
        Map<Long, CompletionFact> factMap = new LinkedHashMap<>();
        for (CompletionFact fact : facts) {
            if (fact.task() != null && fact.task().getId() != null) {
                factMap.put(fact.task().getId(), fact);
            }
        }
        List<TaskHighlight> highlights = new ArrayList<>();
        for (SessionTaskEntity task : tasks.stream()
                .sorted(Comparator.comparingInt(t -> t.getOrderIndex() != null ? t.getOrderIndex() : Integer.MAX_VALUE))
                .toList()) {
            CompletionFact fact = task.getId() != null ? factMap.get(task.getId()) : null;
            CompleteTaskRequest input = fact != null ? fact.input() : null;
            highlights.add(TaskHighlight.builder()
                    .taskId(task.getTaskCode())
                    .title(firstNonBlank(task.getTitle(), task.getObjective(), task.getTaskCode()))
                    .completionStatus(fact != null ? fact.completion().getCompletionStatus() : "NOT_STARTED")
                    .learned(input != null ? firstNonBlank(firstItem(input.getLearnedFrameworkPoints()), firstSentence(input.getSummaryText()), "完成了这一任务的关键推进") : "等待进入")
                    .issue(input != null ? firstNonBlank(firstItem(input.getUnresolvedQuestions()), firstItem(input.getDetectedIssueTags()), firstSentence(input.getLearnerReflection())) : "")
                    .build());
        }
        return highlights;
    }

    private String buildFinalSummary(ResultStatus status,
                                     int completedTasks,
                                     int totalTasks,
                                     List<String> whatYouLearned,
                                     List<String> whatStillNeedsWork) {
        String learned = whatYouLearned.isEmpty() ? "已经完成本轮学习" : truncate(whatYouLearned.get(0), 28);
        String issue = whatStillNeedsWork.isEmpty() ? "" : truncate(whatStillNeedsWork.get(0), 24);
        if (status == ResultStatus.ACHIEVED) {
            return "这轮你已经完成 " + completedTasks + "/" + totalTasks + " 个任务，" + learned + "，可以继续推进下一步。";
        }
        if (status == ResultStatus.PARTIALLY_ACHIEVED) {
            return "这轮你已经完成 " + completedTasks + "/" + totalTasks + " 个任务，已建立起基础理解，但“" + issue + "”还需要继续补稳。";
        }
        return "这轮还没有形成稳定结果，建议先回到关键薄弱点，把当前链路补完整。";
    }

    private CompleteTaskRequest parseCompletionInput(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return jsonSerde.fromJson(json, CompleteTaskRequest.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<LearningMethodProfile> loadMethodProfiles(String sessionId) {
        try {
            var rows = taskMethodProfileRepository.findBySessionKey(sessionId);
            if (rows != null && !rows.isEmpty()) {
                List<LearningMethodProfile> list = new ArrayList<>();
                for (var row : rows) {
                    LearningMethodProfile p = jsonSerde.fromJson(row.getProfileJson(), LearningMethodProfile.class);
                    if (p != null) {
                        list.add(p);
                    }
                }
                return list;
            }
        } catch (Exception ignored) {
        }
        return store.getSessionMethodProfiles().getOrDefault(sessionId, List.of());
    }

    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static void addNonBlank(LinkedHashSet<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private static void addAllNonBlank(LinkedHashSet<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addNonBlank(target, value);
        }
    }

    private static void addAllNonBlank(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }

    private static String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replace('\n', ' ');
        int idx = normalized.indexOf('。');
        if (idx > 0) {
            return normalized.substring(0, idx + 1);
        }
        return truncate(normalized, 42);
    }

    private static String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "…";
    }

    private static String firstItem(List<String> items) {
        if (items == null) {
            return null;
        }
        return items.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).findFirst().orElse(null);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record CompletionFact(SessionTaskEntity task, TaskCompletionEntity completion, CompleteTaskRequest input) {
    }
}
