package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
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
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier llmFailureClassifier;

    public CompositeAnswerEvaluator(
        LlmAnswerEvaluator llmAnswerEvaluator,
        RuleBasedAnswerEvaluator ruleBasedAnswerEvaluator,
        LlmProperties llmProperties,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        this.llmAnswerEvaluator = llmAnswerEvaluator;
        this.ruleBasedAnswerEvaluator = ruleBasedAnswerEvaluator;
        this.llmProperties = llmProperties;
        this.llmCallLogger = llmCallLogger;
        this.llmFailureClassifier = llmFailureClassifier;
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
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.EVALUATE, llmProperties.getModel()),
                llmFailureClassifier.classifyFallback(ex),
                -1
            );
            log.warn("LLM evaluator failed, fallback to rule evaluator: {}", ex.getMessage());
            return ruleBasedAnswerEvaluator.evaluate(context);
        }
    }
}
