package navigator.application.report;

import navigator.api.dto.CompleteTaskRequest;
import navigator.domain.model.ExecutionEvidenceSummary;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import navigator.infrastructure.persistence.entity.TaskInteractionEntity;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.repository.TaskCompletionRepository;
import navigator.infrastructure.persistence.repository.TaskInteractionRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SessionEvidenceAggregator {

    private final LearningPlanRepository learningPlanRepository;
    private final SessionTaskRepository sessionTaskRepository;
    private final TaskInteractionRepository taskInteractionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final JsonSerde jsonSerde;

    public SessionEvidenceAggregator(LearningPlanRepository learningPlanRepository,
                                     SessionTaskRepository sessionTaskRepository,
                                     TaskInteractionRepository taskInteractionRepository,
                                     TaskCompletionRepository taskCompletionRepository,
                                     JsonSerde jsonSerde) {
        this.learningPlanRepository = learningPlanRepository;
        this.sessionTaskRepository = sessionTaskRepository;
        this.taskInteractionRepository = taskInteractionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.jsonSerde = jsonSerde;
    }

    public AggregatedSessionEvidence aggregate(Long sessionDbId) {
        if (sessionDbId == null) {
            return new AggregatedSessionEvidence(null, ExecutionEvidenceSummary.builder().build());
        }

        LearningPlanEntity plan = learningPlanRepository.findBySessionId(sessionDbId);
        java.util.List<SessionTaskEntity> tasks = sessionTaskRepository.findBySessionId(sessionDbId);
        java.util.List<TaskCompletionEntity> completions = taskCompletionRepository.findBySessionId(sessionDbId);
        TaskInteractionEntity latestInteraction = taskInteractionRepository.findLatestBySessionId(sessionDbId);

        ExecutionEvidenceSummary executionSummary = buildExecutionSummary(tasks, completions, latestInteraction);

        return new AggregatedSessionEvidence(plan, executionSummary);
    }

    private ExecutionEvidenceSummary buildExecutionSummary(
            java.util.List<SessionTaskEntity> tasks,
            java.util.List<TaskCompletionEntity> completions,
            TaskInteractionEntity latestInteraction) {

        int totalTasks = tasks != null ? tasks.size() : 0;
        int completedTasks = completions != null ? completions.size() : 0;

        // 聚合 gap tags
        Set<String> gapTags = new LinkedHashSet<>();
        // 聚合 completionInputJson 中的证据
        int totalDurationMinutes = 0;
        int totalInteractionCount = 0;
        boolean summarySubmitted = false;
        Set<String> behaviorSignalSet = new LinkedHashSet<>();
        boolean hasLearnerReflection = false;
        int totalUserQuestions = 0;
        int totalVague = 0;
        double depSum = 0.0;
        int depCount = 0;
        int closureOkTasks = 0;
        if (completions != null) {
            for (TaskCompletionEntity completion : completions) {
                java.util.List<String> tags = parseStringArrayJson(completion.getDetectedGapTagsJson());
                if (tags != null) {
                    gapTags.addAll(tags);
                }
                CompleteTaskRequest input = parseCompletionInput(completion.getCompletionInputJson());
                if (input != null) {
                    if (input.getDurationMinutes() != null) {
                        totalDurationMinutes += input.getDurationMinutes();
                    }
                    if (input.getInteractionCount() != null) {
                        totalInteractionCount += input.getInteractionCount();
                    }
                    if (Boolean.TRUE.equals(input.getUserSummarySubmitted())) {
                        summarySubmitted = true;
                    }
                    if (input.getBehaviorSignals() != null) {
                        behaviorSignalSet.addAll(input.getBehaviorSignals());
                    }
                    if (input.getLearnerReflection() != null && !input.getLearnerReflection().isBlank()) {
                        hasLearnerReflection = true;
                    }
                    TaskExecutionEvidenceSnapshot ev = input.getEvidenceSnapshot();
                    if (ev != null) {
                        totalUserQuestions += ev.getUserInitiatedQuestionTurns();
                        totalVague += ev.getVagueUserReplyCount();
                        depSum += ev.getDirectAnswerDependencyScore();
                        depCount++;
                    }
                    if (closurePayloadOk(input)) {
                        closureOkTasks++;
                    }
                }
            }
        }

        // 行为信号：优先用 completion 的 behaviorSignals，不足时用最近交互
        java.util.List<String> behaviorSignals = new java.util.ArrayList<>(behaviorSignalSet);
        if (behaviorSignals.isEmpty() && latestInteraction != null && latestInteraction.getInteractionType() != null) {
            behaviorSignals.add("最近交互类型: " + latestInteraction.getInteractionType());
        }

        // evidenceHighlights：基于真实证据
        java.util.List<String> evidenceHighlights = new java.util.ArrayList<>();
        evidenceHighlights.add("已完成 " + completedTasks + "/" + totalTasks + " 个任务");
        if (totalDurationMinutes > 0) {
            evidenceHighlights.add("总用时 " + totalDurationMinutes + " 分钟");
        }
        if (totalInteractionCount > 0) {
            evidenceHighlights.add("总交互 " + totalInteractionCount + " 次");
        }
        if (summarySubmitted) {
            evidenceHighlights.add("用户已提交自述");
        }
        if (hasLearnerReflection) {
            evidenceHighlights.add("用户有反思记录");
        }

        Integer closureScore = null;
        if (completedTasks > 0) {
            closureScore = (int) Math.round(100.0 * closureOkTasks / completedTasks);
        }

        return ExecutionEvidenceSummary.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .totalDurationMinutes(totalDurationMinutes > 0 ? totalDurationMinutes : null)
                .totalInteractionCount(totalInteractionCount > 0 ? totalInteractionCount : null)
                .summarySubmitted(summarySubmitted)
                .aggregatedIssueTags(new java.util.ArrayList<>(gapTags))
                .keyBehaviorSignals(behaviorSignals)
                .evidenceHighlights(evidenceHighlights)
                .totalUserQuestionTurns(totalUserQuestions > 0 ? totalUserQuestions : null)
                .totalVagueReplies(totalVague > 0 ? totalVague : null)
                .avgDirectAnswerDependency(depCount > 0 ? depSum / depCount : null)
                .closureQualityScore(closureScore)
                .build();
    }

    private static boolean closurePayloadOk(CompleteTaskRequest input) {
        if (input == null) {
            return false;
        }
        String s = input.getSummaryText();
        if (s == null || s.trim().length() < 10) {
            return false;
        }
        java.util.List<String> fw = input.getLearnedFrameworkPoints();
        if (fw == null || fw.stream().filter(x -> x != null && !x.isBlank()).count() < 2) {
            return false;
        }
        String n = input.getNextPracticeIntent();
        return n != null && !n.isBlank();
    }

    private CompleteTaskRequest parseCompletionInput(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return jsonSerde.fromJson(json, CompleteTaskRequest.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private java.util.List<String> parseStringArrayJson(String json) {
        if (json == null || json.isEmpty()) {
            return java.util.List.of();
        }
        String[] arr = jsonSerde.fromJson(json, String[].class);
        if (arr == null) {
            return java.util.List.of();
        }
        return Arrays.stream(arr)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public record AggregatedSessionEvidence(
            LearningPlanEntity plan,
            ExecutionEvidenceSummary execution
    ) {
    }
}

