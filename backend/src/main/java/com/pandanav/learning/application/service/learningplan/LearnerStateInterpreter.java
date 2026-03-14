package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;

public interface LearnerStateInterpreter {
    LearnerStateSnapshot interpret(LearningPlanPlanningContext context);
}
