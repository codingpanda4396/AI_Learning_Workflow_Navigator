package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningPlanStageMapperTest {

    @Test
    void shouldMapLegacyEvaluationToReflection() {
        assertEquals(Stage.REFLECTION, LearningPlanStageMapper.normalize("EVALUATION"));
        assertEquals(Stage.TRAINING, LearningPlanStageMapper.normalize("training"));
    }
}
