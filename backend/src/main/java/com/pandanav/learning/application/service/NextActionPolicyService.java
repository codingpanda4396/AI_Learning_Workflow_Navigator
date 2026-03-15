package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.NextActionContext;
import com.pandanav.learning.domain.policy.NextActionDecision;
import com.pandanav.learning.domain.policy.NextActionPolicy;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NextActionPolicyService {

    private final NextActionPolicy nextActionPolicy;

    public NextActionPolicyService(NextActionPolicy nextActionPolicy) {
        this.nextActionPolicy = nextActionPolicy;
    }

    public NextAction decide(Integer score, List<ErrorTag> errorTags) {
        return decideWithReason(score, errorTags, null, 0, 0).action();
    }

    public NextActionDecision decideWithReason(
        Integer score,
        List<ErrorTag> errorTags,
        Stage stage,
        int consecutiveStepFailures,
        int stageDeviationScore
    ) {
        if (score == null) {
            return new NextActionDecision(NextAction.NOOP, "Score is missing; keep current learning path.");
        }
        Set<ErrorTag> tags = errorTags == null ? Set.of() : new HashSet<>(errorTags);
        return nextActionPolicy.decide(
            score,
            tags,
            new NextActionContext(stage, Math.max(consecutiveStepFailures, 0), Math.max(stageDeviationScore, 0))
        );
    }
}

