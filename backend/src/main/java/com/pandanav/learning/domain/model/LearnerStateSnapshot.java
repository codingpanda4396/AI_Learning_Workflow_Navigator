package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PreferredLearningMode;

public record LearnerStateSnapshot(
    GoalOrientation goalOrientation,
    PreferredLearningMode preferredLearningMode,
    PacePreference pacePreference,
    CurrentBlockType currentBlockType,
    EvidenceLevel evidenceLevel,
    MotivationRisk motivationRisk,
    String confidenceReasonSummary,
    String primaryBlockDescription,
    String secondaryBlockDescription
) {
}
