package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreBasedNextActionPolicyTest {

    private final ScoreBasedNextActionPolicy policy = new ScoreBasedNextActionPolicy();

    @Test
    void shouldChooseRemedialForLowScoreWithConceptConfusion() {
        NextAction action = policy.decide(55, Set.of(ErrorTag.CONCEPT_CONFUSION));
        assertEquals(NextAction.INSERT_REMEDIAL_UNDERSTANDING, action);
    }

    @Test
    void shouldChooseReinforcementForHighScoreWithBoundaryCaseIssues() {
        NextAction action = policy.decide(85, Set.of(ErrorTag.BOUNDARY_CASE));
        assertEquals(NextAction.INSERT_TRAINING_REINFORCEMENT, action);
    }
}

