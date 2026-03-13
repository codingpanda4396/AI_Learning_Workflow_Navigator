package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmProfileConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmPropertiesTest {

    @Test
    void shouldNotSendThinkingBudgetWhenConfiguredAsZero() {
        LlmProperties properties = new LlmProperties();
        properties.getProfiles().getLightJsonTask().setThinkingBudget(0);

        LlmProfileConfig profile = properties.resolveProfile(LlmInvocationProfile.LIGHT_JSON_TASK, "STRUCTURE");

        assertFalse(profile.extraParams().containsKey("thinking_budget"));
    }

    @Test
    void shouldSendThinkingBudgetWhenConfiguredAsPositiveNumber() {
        LlmProperties properties = new LlmProperties();
        properties.getProfiles().getHeavyReasoningTask().setThinkingBudget(1024);

        LlmProfileConfig profile = properties.resolveProfile(LlmInvocationProfile.HEAVY_REASONING_TASK, "PATH_PLAN");

        assertTrue(profile.extraParams().containsKey("thinking_budget"));
        assertEquals(1024, profile.extraParams().get("thinking_budget"));
    }
}
