package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.service.LearningPlanSchemaValidationException;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier llmFailureClassifier;

    public LearningPlanOrchestrator(
        RuleBasedPlanBuilder ruleBasedPlanBuilder,
        LearningPlanPromptBuilder learningPlanPromptBuilder,
        LearningPlanResultValidator learningPlanResultValidator,
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmProperties llmProperties,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        this.ruleBasedPlanBuilder = ruleBasedPlanBuilder;
        this.learningPlanPromptBuilder = learningPlanPromptBuilder;
        this.learningPlanResultValidator = learningPlanResultValidator;
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmProperties = llmProperties;
        this.llmCallLogger = llmCallLogger;
        this.llmFailureClassifier = llmFailureClassifier;
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
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel()),
                LlmFallbackReason.UNKNOWN_ERROR,
                -1
            );
            return new OrchestratedPlan(rulePreview, null, PlanSource.RULE_FALLBACK, true, List.of("LLM_NOT_READY"));
        }

        Instant start = Instant.now();
        try {
            LlmPrompt prompt = learningPlanPromptBuilder.build(context, rulePreview);
            LlmTextResult llmResult = llmGateway.generate(LlmStage.LEARNING_PLAN, prompt);
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmResult.model());
            JsonNode json = llmJsonParser.parse(llmResult.text(), llmContext);
            LearningPlanLlmResult parsed = learningPlanResultValidator.parse(json);
            List<String> errors = learningPlanResultValidator.validate(parsed, rulePreview);
            if (!errors.isEmpty()) {
                log.warn(
                    "LearningPlanOrchestrator: LLM output contract validation failed, using rule fallback. {} traceId={} requestId={} model={} errors={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    errors
                );
                llmCallLogger.logStructuredOutputFailure(llmContext, LlmFallbackReason.JSON_SCHEMA_MISMATCH.name(), String.join("; ", errors));
                llmCallLogger.logFallback(
                    llmContext,
                    LlmFallbackReason.JSON_SCHEMA_MISMATCH,
                    LlmObservabilityHelper.elapsedMs(start)
                );
                return new OrchestratedPlan(rulePreview, traceId(llmResult), PlanSource.RULE_FALLBACK, true, List.of(LlmFallbackReason.JSON_SCHEMA_MISMATCH.name()));
            }
            LearningPlanPreview merged = merge(rulePreview, parsed);
            log.info(
                "LearningPlanOrchestrator: LLM plan applied. {} traceId={} requestId={} model={} planSource={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                PlanSource.LLM
            );
            return new OrchestratedPlan(merged, traceId(llmResult), PlanSource.LLM, false, List.of());
        } catch (LearningPlanSchemaValidationException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, ex.fallbackReason().name(), String.join("; ", ex.errors()));
            log.warn(
                "LearningPlanOrchestrator: Learning plan schema mismatch, using rule fallback. {} traceId={} requestId={} model={} errors={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.errors()
            );
            llmCallLogger.logFallback(llmContext, ex.fallbackReason(), LlmObservabilityHelper.elapsedMs(start));
            return new OrchestratedPlan(rulePreview, null, PlanSource.RULE_FALLBACK, true, List.of(ex.fallbackReason().name()));
        } catch (LlmJsonParseException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, ex.fallbackReason().name(), ex.diagnosticSummary());
            log.warn(
                "LearningPlanOrchestrator: Learning plan JSON parse failed, using rule fallback. {} traceId={} requestId={} model={} reason={} diagnostics={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.fallbackReason(),
                ex.diagnosticSummary()
            );
            llmCallLogger.logFallback(llmContext, ex.fallbackReason(), LlmObservabilityHelper.elapsedMs(start));
            return new OrchestratedPlan(rulePreview, null, PlanSource.RULE_FALLBACK, true, List.of(ex.fallbackReason().name()));
        } catch (Exception ex) {
            LlmFallbackReason reason = llmFailureClassifier.classifyFallback(ex);
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            log.warn(
                "LearningPlanOrchestrator: LLM call failed, using rule fallback. {} traceId={} requestId={} model={} reason={} error={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                reason,
                ex.getMessage(),
                ex
            );
            llmCallLogger.logFallback(
                llmContext,
                reason,
                LlmObservabilityHelper.elapsedMs(start)
            );
            return new OrchestratedPlan(rulePreview, null, PlanSource.RULE_FALLBACK, true, List.of(reason.name()));
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
        PlanSource planSource,
        boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
    }
}
