package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import org.springframework.stereotype.Component;

@Component
public class LearnerProfileSnapshotBuilder {

    public LearnerProfileSnapshot build(
        Long diagnosisSessionId,
        Long learningSessionId,
        Long userId,
        Integer profileVersion,
        LearnerFeatureAggregator.AggregationResult aggregationResult
    ) {
        LearnerProfileSnapshot snapshot = new LearnerProfileSnapshot();
        snapshot.setDiagnosisSessionId(diagnosisSessionId);
        snapshot.setLearningSessionId(learningSessionId);
        snapshot.setUserId(userId);
        snapshot.setProfileVersion(profileVersion == null || profileVersion <= 0 ? 1 : profileVersion);
        snapshot.setFeatureSummary(aggregationResult.featureSummary());
        snapshot.setStrategyHints(aggregationResult.strategyHints());
        snapshot.setConstraints(aggregationResult.constraints());
        snapshot.setExplanations(aggregationResult.explanations());
        return snapshot;
    }
}
