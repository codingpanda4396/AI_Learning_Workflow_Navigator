package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;

public class DefaultTaskObjectiveTemplateStrategy implements TaskObjectiveTemplateStrategy {

    @Override
    public String buildObjective(Stage stage, String conceptName) {
        return switch (stage) {
            case STRUCTURE -> "Build a structure map for concept: " + conceptName;
            case UNDERSTANDING -> "Explain mechanism and misconceptions for concept: " + conceptName;
            case TRAINING -> "Complete adaptive training for concept: " + conceptName;
            case REFLECTION -> "Reflect on errors and next improvements for concept: " + conceptName;
        };
    }
}


