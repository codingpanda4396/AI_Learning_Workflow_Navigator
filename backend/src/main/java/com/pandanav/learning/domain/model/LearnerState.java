package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.ConceptCodeGap;
import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.FrustrationRisk;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.enums.PreferredLearningMode;

import java.util.List;

public record LearnerState(
    GoalOrientation goalOrientation,
    PreferredLearningMode preferredLearningMode,
    PacePreference pacePreference,
    CurrentBlockType currentBlockType,
    EvidenceLevel evidenceLevel,
    MotivationRisk motivationRisk,
    FoundationStatus foundationStatus,
    PracticeReadiness practiceReadiness,
    ConceptCodeGap conceptCodeGap,
    FrustrationRisk frustrationRisk,
    String confidenceReasonSummary,
    String primaryBlockDescription,
    String secondaryBlockDescription,
    List<String> evidenceSummaries
) {
}
