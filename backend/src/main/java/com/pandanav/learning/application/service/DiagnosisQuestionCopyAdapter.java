package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.PlanningContext;

import java.util.List;

/**
 * Adapts question copy after selection: terminology, topic, hint strength, etc.
 * Does not decide which questions to ask; only how they are phrased.
 */
public interface DiagnosisQuestionCopyAdapter {

    List<DiagnosisQuestion> adapt(
        List<DiagnosisQuestionDraft> selectedDrafts,
        PlanningContext planningContext,
        DiagnosisLearnerProfileSnapshot profileSnapshot,
        DiagnosisStrategyDecision strategyDecision
    );
}
