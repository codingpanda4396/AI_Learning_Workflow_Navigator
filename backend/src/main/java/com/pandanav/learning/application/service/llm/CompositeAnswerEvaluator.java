package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeAnswerEvaluator implements AnswerEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CompositeAnswerEvaluator.class);

    private final LlmAnswerEvaluator llmAnswerEvaluator;
    private final RuleBasedAnswerEvaluator ruleBasedAnswerEvaluator;
    private final LlmProperties llmProperties;

    public CompositeAnswerEvaluator(
        LlmAnswerEvaluator llmAnswerEvaluator,
        RuleBasedAnswerEvaluator ruleBasedAnswerEvaluator,
        LlmProperties llmProperties
    ) {
        this.llmAnswerEvaluator = llmAnswerEvaluator;
        this.ruleBasedAnswerEvaluator = ruleBasedAnswerEvaluator;
        this.llmProperties = llmProperties;
    }

    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        if (!llmProperties.isReady() || !llmProperties.isEnabled()) {
            return ruleBasedAnswerEvaluator.evaluate(context);
        }
        try {
            return llmAnswerEvaluator.evaluate(context);
        } catch (Exception ex) {
            if (!llmProperties.isFallbackToRule()) {
                throw ex;
            }
            log.warn("LLM evaluator failed, fallback to rule evaluator: {}", ex.getMessage());
            return ruleBasedAnswerEvaluator.evaluate(context);
        }
    }
}
