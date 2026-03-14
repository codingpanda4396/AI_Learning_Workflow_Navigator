package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;

public interface LearnerSignalInterpreter {
    LearnerSignalSnapshot interpret(LearningPlanPlanningContext context);
}
