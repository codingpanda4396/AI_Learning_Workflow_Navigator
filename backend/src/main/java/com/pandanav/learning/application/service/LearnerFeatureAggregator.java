package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.LearnerFeatureSignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LearnerFeatureAggregator {

    public AggregationResult aggregate(List<LearnerFeatureSignal> signals) {
        Map<String, AggregateBucket> bucketByKey = new LinkedHashMap<>();
        for (LearnerFeatureSignal signal : signals) {
            String bucketKey = signal.getFeatureKey() + "||" + signal.getFeatureValue();
            AggregateBucket bucket = bucketByKey.computeIfAbsent(
                bucketKey,
                ignored -> new AggregateBucket(signal.getFeatureKey(), signal.getFeatureValue())
            );
            bucket.scoreTotal += signal.getScoreDelta();
            bucket.confidenceTotal += signal.getConfidence();
            bucket.count += 1;
            bucket.questionIds.add(signal.getQuestionId());
        }

        List<Map<String, Object>> featureItems = new ArrayList<>();
        for (AggregateBucket bucket : bucketByKey.values()) {
            double confidence = bucket.count == 0 ? 0.0 : bucket.confidenceTotal / bucket.count;
            featureItems.add(Map.of(
                "featureKey", bucket.featureKey,
                "featureValue", bucket.featureValue,
                "score", round(bucket.scoreTotal),
                "confidence", round(confidence),
                "evidenceCount", bucket.count,
                "sourceQuestionIds", bucket.questionIds.stream().distinct().toList()
            ));
        }

        Map<String, Object> featureSummary = new LinkedHashMap<>();
        featureSummary.put("features", featureItems);
        featureSummary.put("featureCount", featureItems.size());

        Map<String, Object> strategyHints = new LinkedHashMap<>();
        putHint(strategyHints, "learningPreference", dominantValue(bucketByKey, "learning_preference"));
        putHint(strategyHints, "supportPriority", dominantValue(bucketByKey, "support_priority"));
        putHint(strategyHints, "goalOrientation", dominantValue(bucketByKey, "goal_orientation"));

        Map<String, Object> constraints = new LinkedHashMap<>();
        putHint(constraints, "timeBudget", dominantValue(bucketByKey, "time_budget"));
        putHint(constraints, "learningIntensity", dominantValue(bucketByKey, "learning_intensity"));
        putHint(constraints, "assessmentPressure", dominantValue(bucketByKey, "assessment_pressure"));

        List<Map<String, Object>> topSignals = signals.stream()
            .sorted(Comparator.comparingDouble(LearnerFeatureSignal::getConfidence).reversed())
            .limit(5)
            .map(signal -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", signal.getQuestionId());
                item.put("featureKey", signal.getFeatureKey());
                item.put("featureValue", signal.getFeatureValue());
                item.put("scoreDelta", round(signal.getScoreDelta()));
                item.put("confidence", round(signal.getConfidence()));
                return item;
            })
            .toList();

        Map<String, Object> explanations = new LinkedHashMap<>();
        explanations.put("topSignals", topSignals);
        explanations.put("signalCount", signals.size());

        return new AggregationResult(featureSummary, strategyHints, constraints, explanations);
    }

    private void putHint(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String dominantValue(Map<String, AggregateBucket> bucketByKey, String featureKey) {
        return bucketByKey.values().stream()
            .filter(bucket -> featureKey.equals(bucket.featureKey))
            .max(Comparator.comparingDouble((AggregateBucket bucket) -> bucket.scoreTotal)
                .thenComparingDouble(bucket -> bucket.confidenceTotal))
            .map(bucket -> bucket.featureValue)
            .orElse("");
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public record AggregationResult(
        Map<String, Object> featureSummary,
        Map<String, Object> strategyHints,
        Map<String, Object> constraints,
        Map<String, Object> explanations
    ) {
    }

    private static class AggregateBucket {
        private final String featureKey;
        private final String featureValue;
        private final List<String> questionIds = new ArrayList<>();
        private double scoreTotal;
        private double confidenceTotal;
        private int count;

        private AggregateBucket(String featureKey, String featureValue) {
            this.featureKey = featureKey;
            this.featureValue = featureValue;
        }
    }
}
