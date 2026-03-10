package com.pandanav.learning.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanModeTest {

    @Test
    void shouldParseValidModes() {
        assertEquals(PlanMode.RULE, PlanMode.fromQuery("rule"));
        assertEquals(PlanMode.LLM, PlanMode.fromQuery("LLM"));
        assertEquals(PlanMode.AUTO, PlanMode.fromQuery("auto"));
        assertEquals(PlanMode.AUTO, PlanMode.fromQuery(null));
    }

    @Test
    void shouldRejectUnsupportedMode() {
        assertThrows(IllegalArgumentException.class, () -> PlanMode.fromQuery("unknown"));
    }
}
