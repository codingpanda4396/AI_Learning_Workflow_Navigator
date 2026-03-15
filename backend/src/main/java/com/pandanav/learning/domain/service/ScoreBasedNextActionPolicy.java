package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.NextActionContext;
import com.pandanav.learning.domain.policy.NextActionDecision;
import com.pandanav.learning.domain.policy.NextActionPolicy;

import java.util.Set;

public class ScoreBasedNextActionPolicy implements NextActionPolicy {

    @Override
    public NextActionDecision decide(int score, Set<ErrorTag> errorTags, NextActionContext context) {
        NextActionContext safeContext = context == null ? NextActionContext.empty() : context;
        if (safeContext.consecutiveStepFailures() >= 2) {
            if (safeContext.stageDeviationScore() >= 45) {
                return new NextActionDecision(
                    NextAction.REPLAN_SESSION,
                    "Multiple step failures with high stage deviation detected; trigger replan."
                );
            }
            return new NextActionDecision(
                NextAction.INSERT_REMEDIAL_STEP,
                "Recent step failures detected; insert a local remedial step before global replan."
            );
        }
        if (safeContext.stage() == Stage.TRAINING && safeContext.stageDeviationScore() >= 35) {
            return new NextActionDecision(
                NextAction.INSERT_REMEDIAL_STEP,
                "Training-stage deviation is high; add a targeted remedial step."
            );
        }
        if (safeContext.stage() == Stage.REFLECTION && score >= 75 && score < 85) {
            return new NextActionDecision(
                NextAction.SKIP_CURRENT_STEP,
                "Reflection checkpoint is acceptable; skip redundant step to keep progress."
            );
        }
        if (errorTags != null && !errorTags.isEmpty()) {
            if (score < 60 && (errorTags.contains(ErrorTag.CONCEPT_CONFUSION) || errorTags.contains(ErrorTag.MEMORY_GAP))) {
                return new NextActionDecision(
                    NextAction.INSERT_REMEDIAL_UNDERSTANDING,
                    "Low score with concept-level gaps; move to remedial understanding."
                );
            }
            if (score < 80 && (errorTags.contains(ErrorTag.MISSING_STEPS) || errorTags.contains(ErrorTag.SHALLOW_REASONING))) {
                return new NextActionDecision(
                    NextAction.INSERT_TRAINING_VARIANTS,
                    "Reasoning depth is insufficient; add variant training."
                );
            }
            if (score < 90 && (errorTags.contains(ErrorTag.BOUNDARY_CASE) || errorTags.contains(ErrorTag.TERMINOLOGY))) {
                return new NextActionDecision(
                    NextAction.INSERT_TRAINING_REINFORCEMENT,
                    "Boundary or terminology mistakes remain; reinforce with focused training."
                );
            }
        }
        if (score < 60) {
            return new NextActionDecision(
                NextAction.INSERT_REMEDIAL_UNDERSTANDING,
                "Score is below baseline; remedial understanding is required."
            );
        }
        if (score < 80) {
            return new NextActionDecision(
                NextAction.INSERT_TRAINING_VARIANTS,
                "Score indicates unstable performance; schedule variant training."
            );
        }
        if (score < 90) {
            return new NextActionDecision(
                NextAction.INSERT_TRAINING_REINFORCEMENT,
                "Score is close to target but not stable; add reinforcement."
            );
        }
        if (score <= 100) {
            return new NextActionDecision(
                NextAction.ADVANCE_TO_NEXT_NODE,
                "Current node performance is stable; proceed to next node."
            );
        }
        return new NextActionDecision(NextAction.NOOP, "No safe action can be derived from current signals.");
    }
}


