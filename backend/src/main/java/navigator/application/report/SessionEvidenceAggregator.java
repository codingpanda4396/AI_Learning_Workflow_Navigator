package navigator.application.report;

import navigator.domain.model.ExecutionEvidenceSummary;
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
        if (completions != null) {
            for (TaskCompletionEntity completion : completions) {
                java.util.List<String> tags = parseStringArrayJson(completion.getDetectedGapTagsJson());
                if (tags != null) {
                    gapTags.addAll(tags);
                }
            }
        }

        // 行为信号：目前先从最近一次交互抽取一个简单描述
        java.util.List<String> behaviorSignals = new java.util.ArrayList<>();
        if (latestInteraction != null && latestInteraction.getInteractionType() != null) {
            behaviorSignals.add("最近交互类型: " + latestInteraction.getInteractionType());
        }

        java.util.List<String> evidenceHighlights = new java.util.ArrayList<>();
        evidenceHighlights.add("已完成 " + completedTasks + "/" + totalTasks + " 个任务");

        return ExecutionEvidenceSummary.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .aggregatedIssueTags(new java.util.ArrayList<>(gapTags))
                .keyBehaviorSignals(behaviorSignals)
                .evidenceHighlights(evidenceHighlights)
                .build();
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

