package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningPlanOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanOrchestrator.class);

    private final RuleBasedPlanBuilder ruleBasedPlanBuilder;
    private final LearningPlanPromptBuilder learningPlanPromptBuilder;
    private final LearningPlanResultValidator learningPlanResultValidator;
    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;
    private final LlmProperties llmProperties;

    public LearningPlanOrchestrator(
        RuleBasedPlanBuilder ruleBasedPlanBuilder,
        LearningPlanPromptBuilder learningPlanPromptBuilder,
        LearningPlanResultValidator learningPlanResultValidator,
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmProperties llmProperties
    ) {
        this.ruleBasedPlanBuilder = ruleBasedPlanBuilder;
        this.learningPlanPromptBuilder = learningPlanPromptBuilder;
        this.learningPlanResultValidator = learningPlanResultValidator;
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmProperties = llmProperties;
    }

    public OrchestratedPlan preview(LearningPlanPlanningContext context) {
        LearningPlanPreview rulePreview = ruleBasedPlanBuilder.build(context);
        String logCtx = "goalId=%s, chapterId=%s, userId=%s".formatted(
            context.goalId(), context.chapterId(), context.userId());

        if (!llmProperties.isEnabled() || !llmProperties.isReady()) {
            log.warn(
                "LearningPlanOrchestrator: LLM not ready, using rule fallback. {} enabled={}, ready={}",
                logCtx, llmProperties.isEnabled(), llmProperties.isReady()
            );
            return new OrchestratedPlan(rulePreview, null, true, List.of("llm_not_ready"));
        }

        try {
            LlmPrompt prompt = learningPlanPromptBuilder.build(context, rulePreview);
            LlmTextResult llmResult = llmGateway.generate(prompt);
            JsonNode json = llmJsonParser.parse(llmResult.text());
            LearningPlanLlmResult parsed = learningPlanResultValidator.parse(json);
            List<String> errors = learningPlanResultValidator.validate(parsed, rulePreview);
            if (!errors.isEmpty()) {
                log.warn(
                    "LearningPlanOrchestrator: LLM output validation failed, using rule fallback. {} errors={}",
                    logCtx, errors
                );
                return new OrchestratedPlan(rulePreview, traceId(llmResult), true, errors);
            }
            LearningPlanPreview merged = merge(rulePreview, parsed);
            log.debug("LearningPlanOrchestrator: LLM plan applied. {} trace={}", logCtx, traceId(llmResult));
            return new OrchestratedPlan(merged, traceId(llmResult), false, List.of());
        } catch (Exception ex) {
            log.warn(
                "LearningPlanOrchestrator: LLM call failed, using rule fallback. {} reason={}",
                logCtx, ex.getMessage(), ex
            );
            return new OrchestratedPlan(rulePreview, null, true, List.of(ex.getMessage() == null ? "llm_failed" : ex.getMessage()));
        }
    }

    private LearningPlanPreview merge(LearningPlanPreview rulePreview, LearningPlanLlmResult parsed) {
        return new LearningPlanPreview(
            new LearningPlanSummary(
                parsed.headline(),
                rulePreview.summary().recommendedStartNodeId(),
                rulePreview.summary().recommendedStartNodeName(),
                rulePreview.summary().recommendedPace(),
                rulePreview.summary().estimatedMinutes(),
                rulePreview.summary().estimatedNodeCount(),
                rulePreview.summary().estimatedStageCount()
            ),
            parsed.reasons(),
            parsed.focuses(),
            rulePreview.pathPreview(),
            parsed.taskPreview(),
            rulePreview.adjustments()
        );
    }

    private String traceId(LlmTextResult result) {
        if (result == null) {
            return null;
        }
        return (result.provider() == null ? "llm" : result.provider()) + ":" + (result.model() == null ? "unknown" : result.model());
    }

    public record OrchestratedPlan(
        LearningPlanPreview preview,
        String llmTraceId,
        boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
    }
}
