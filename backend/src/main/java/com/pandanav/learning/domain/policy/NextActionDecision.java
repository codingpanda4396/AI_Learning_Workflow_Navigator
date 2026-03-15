package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.NextAction;

public record NextActionDecision(
    NextAction action,
    String reason
) {
}
