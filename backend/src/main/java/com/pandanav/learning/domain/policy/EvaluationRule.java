package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.ErrorTag;

public interface EvaluationRule {

    void apply(EvaluationContext context, EvaluationDraft draft);

    record EvaluationContext(String conceptName, String objective, String userAnswer) {
    }

    interface EvaluationDraft {

        int score();

        void addScore(int delta);

        void addDiagnosis(String message);

        void addFix(String fix);

        void addErrorTag(ErrorTag errorTag);
    }
}

