package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;

public class DefaultTaskObjectiveTemplateStrategy implements TaskObjectiveTemplateStrategy {

    @Override
    public String buildObjective(Stage stage, String conceptName) {
        return switch (stage) {
            case STRUCTURE -> "梳理" + conceptName + "的核心结构";
            case UNDERSTANDING -> "理解" + conceptName + "的机制与易错点";
            case TRAINING -> "完成" + conceptName + "的针对性训练";
            case REFLECTION -> "反思" + conceptName + "的错误与改进方向";
        };
    }
}


