package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.policy.NextActionPolicy;

import java.util.Set;

public class ScoreBasedNextActionPolicy implements NextActionPolicy {

    @Override
    public NextAction decide(int score, Set<ErrorTag> errorTags) {
        if (errorTags != null && !errorTags.isEmpty()) {
            if (score < 60 && (errorTags.contains(ErrorTag.CONCEPT_CONFUSION) || errorTags.contains(ErrorTag.MEMORY_GAP))) {
                return NextAction.INSERT_REMEDIAL_UNDERSTANDING;
            }
            if (score < 80 && (errorTags.contains(ErrorTag.MISSING_STEPS) || errorTags.contains(ErrorTag.SHALLOW_REASONING))) {
                return NextAction.INSERT_TRAINING_VARIANTS;
            }
            if (score < 90 && (errorTags.contains(ErrorTag.BOUNDARY_CASE) || errorTags.contains(ErrorTag.TERMINOLOGY))) {
                return NextAction.INSERT_TRAINING_REINFORCEMENT;
            }
        }
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


