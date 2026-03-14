package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PersonalizedNarrative;

public interface PersonalizedNarrativeGenerator {
    PersonalizedNarrative generate(
        LearningPlanPlanningContext context,
        LearnerStateSnapshot learnerStateSnapshot,
        DecisionPlan decisionPlan,
        LearningPlanPreview preview
    );
}
