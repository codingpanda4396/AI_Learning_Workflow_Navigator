package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningPlanResultValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningPlanResultValidator validator = new LearningPlanResultValidator();

    @Test
    void shouldReportSpecificSchemaErrors() throws Exception {
        String json = """
            {
              "headline": "valid headline text",
              "reasons": [{"type":"START_POINT","title":"good"}],
              "focuses": ["focus-1", 2],
              "task_preview": [{"stage":"INVALID","title":"t1","goal":"g1","learner_action":"learn more","ai_support":"coach more","estimated_minutes":"8"}]
            }
            """;

        LearningPlanSchemaValidationException ex = assertThrows(
            LearningPlanSchemaValidationException.class,
            () -> validator.parse(objectMapper.readTree(json))
        );

        assertTrue(ex.errors().contains("$.reasons[0].description is required"));
        assertTrue(ex.errors().contains("$.focuses[1] must be string"));
        assertTrue(ex.errors().contains("$.task_preview[0].stage invalid enum value: INVALID"));
        assertTrue(ex.errors().contains("$.task_preview[0].estimated_minutes must be integer"));
    }
}
