package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.LearnerSignalTier;

public record LearnerSignalSnapshot(
    LearnerSignalTier conceptUnderstanding,
    LearnerSignalTier relationshipUnderstanding,
    LearnerSignalTier codeMapping,
    LearnerSignalTier decompositionAbility,
    LearnerSignalTier expressionClarity,
    LearnerSignalTier recentStability,
    LearnerSignalTier timePressure,
    LearnerSignalTier pacePreference,
    LearnerSignalTier confidence
) {
}
