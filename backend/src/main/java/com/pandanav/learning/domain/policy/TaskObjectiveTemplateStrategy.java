package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.Stage;

public interface TaskObjectiveTemplateStrategy {

    String buildObjective(Stage stage, String conceptName);
}


