package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.ErrorTag;

import java.util.Set;

public interface NextActionPolicy {

    default NextAction decide(int score, Set<ErrorTag> errorTags) {
        return decide(score, errorTags, NextActionContext.empty()).action();
    }

    NextActionDecision decide(int score, Set<ErrorTag> errorTags, NextActionContext context);
}


