package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.policy.NextActionPolicy;

import java.util.Set;

public class ScoreBasedNextActionPolicy implements NextActionPolicy {

    @Override
    public NextAction decide(int score, Set<ErrorTag> errorTags) {
        if (score < 60) {
            return NextAction.INSERT_REMEDIAL_UNDERSTANDING;
        }
        if (score < 80) {
            return NextAction.INSERT_TRAINING_VARIANTS;
        }
        if (score < 90) {
            return NextAction.INSERT_TRAINING_REINFORCEMENT;
        }
        if (score <= 100) {
            return NextAction.ADVANCE_TO_NEXT_NODE;
        }
        return NextAction.NOOP;
    }
}


