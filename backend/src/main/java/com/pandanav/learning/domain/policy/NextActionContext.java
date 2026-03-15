package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.Stage;

public record NextActionContext(
    Stage stage,
    int consecutiveStepFailures,
    int stageDeviationScore
) {
    public static NextActionContext empty() {
        return new NextActionContext(null, 0, 0);
    }
}
