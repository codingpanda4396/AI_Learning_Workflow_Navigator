package com.pandanav.learning.domain.llm;

import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;

public interface AnswerEvaluator {

    EvaluationResult evaluate(EvaluationContext context);
}

