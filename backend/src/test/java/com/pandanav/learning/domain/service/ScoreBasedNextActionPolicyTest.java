package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.NextActionContext;
import com.pandanav.learning.domain.policy.NextActionDecision;
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

    @Test
    void shouldPreferStepRemedialWhenConsecutiveFailuresAppear() {
        NextActionDecision decision = policy.decide(
            78,
            Set.of(ErrorTag.SHALLOW_REASONING),
            new NextActionContext(Stage.TRAINING, 2, 30)
        );
        assertEquals(NextAction.INSERT_REMEDIAL_STEP, decision.action());
    }

    @Test
    void shouldEscalateToReplanWhenFailureAndDeviationAreHigh() {
        NextActionDecision decision = policy.decide(
            42,
            Set.of(ErrorTag.CONCEPT_CONFUSION),
            new NextActionContext(Stage.TRAINING, 3, 55)
        );
        assertEquals(NextAction.REPLAN_SESSION, decision.action());
    }
}

