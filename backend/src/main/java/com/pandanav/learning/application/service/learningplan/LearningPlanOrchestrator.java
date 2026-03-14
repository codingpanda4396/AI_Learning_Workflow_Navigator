package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.LearningPlanSchemaValidationException;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
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
        log.info(
            "LearningPlan request received. {} strategy={} timeBudget={} basedOnPreviewId={} userFeedback={}",
            logCtx,
            context.requestedStrategy(),
            context.requestedTimeBudgetMinutes(),
            context.basedOnPreviewId(),
            context.userFeedback()
        );

        if (!llmProperties.isEnabled() || !llmProperties.isReady()) {
            log.warn(
                "LearningPlan LLM unavailable, using fallback. {} enabled={} ready={}",
                logCtx, llmProperties.isEnabled(), llmProperties.isReady()
            );
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel()),
                LlmFallbackReason.UNKNOWN_ERROR,
                -1
            );
            return fallback(rulePreview, null, List.of("LLM_NOT_READY"));
        }

        Instant start = Instant.now();
        try {
            LlmPrompt prompt = learningPlanPromptBuilder.build(context, rulePreview);
            log.info("LearningPlan LLM call starting. {} model={} promptKey={}", logCtx, llmProperties.getModel(), prompt.promptKey());
            LlmTextResult llmResult = llmGateway.generate(LlmStage.LEARNING_PLAN, prompt);
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmResult.model());
            if (isTruncated(llmResult)) {
                String details = "finishReason=%s truncated=%s completionTokens=%s maxOutputTokens=%s".formatted(
                    llmResult.usage() == null ? "unknown" : String.valueOf(llmResult.usage().finishReason()),
                    llmResult.usage() != null && llmResult.usage().truncated(),
                    llmResult.usage() == null ? "unknown" : String.valueOf(llmResult.usage().tokenOutput()),
                    prompt.maxOutputTokens() == null ? "unknown" : String.valueOf(prompt.maxOutputTokens())
                );
                llmCallLogger.logStructuredOutputFailure(llmContext, LlmFallbackReason.OUTPUT_TRUNCATED.name(), details);
                log.warn(
                    "LearningPlan LLM output truncated, fallback hit. {} traceId={} requestId={} model={} details={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    details
                );
                llmCallLogger.logFallback(llmContext, LlmFallbackReason.OUTPUT_TRUNCATED, LlmObservabilityHelper.elapsedMs(start));
                return fallback(rulePreview, traceId(llmResult), List.of(LlmFallbackReason.OUTPUT_TRUNCATED.name()));
            }

            JsonNode json = llmJsonParser.parse(llmResult.text(), llmContext);
            LearningPlanLlmResult parsed = learningPlanResultValidator.parse(json);
            LearningPlanLlmResult normalized = learningPlanResultValidator.normalize(parsed, rulePreview);
            List<String> errors = new ArrayList<>();
            errors.addAll(learningPlanResultValidator.validateRawTaskPreview(parsed, rulePreview));
            errors.addAll(learningPlanResultValidator.validate(normalized, rulePreview));
            if (!errors.isEmpty()) {
                log.warn(
                    "LearningPlan LLM schema validation failed, fallback hit. {} traceId={} requestId={} model={} errors={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    errors
                );
                llmCallLogger.logStructuredOutputFailure(llmContext, LlmFallbackReason.JSON_SCHEMA_MISMATCH.name(), String.join("; ", errors));
                llmCallLogger.logFallback(llmContext, LlmFallbackReason.JSON_SCHEMA_MISMATCH, LlmObservabilityHelper.elapsedMs(start));
                return fallback(rulePreview, traceId(llmResult), List.of(LlmFallbackReason.JSON_SCHEMA_MISMATCH.name()));
            }

            LearningPlanPreview merged = merge(rulePreview, normalized, "LLM", false, List.of());
            log.info(
                "LearningPlan LLM applied successfully. {} traceId={} requestId={} model={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model()
            );
            return new OrchestratedPlan(merged, traceId(llmResult), PlanSource.LLM, false, List.of());
        } catch (LearningPlanSchemaValidationException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, ex.fallbackReason().name(), String.join("; ", ex.errors()));
            log.warn(
                "LearningPlan schema mismatch, fallback hit. {} traceId={} requestId={} model={} errors={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.errors()
            );
            llmCallLogger.logFallback(llmContext, ex.fallbackReason(), LlmObservabilityHelper.elapsedMs(start));
            return fallback(rulePreview, null, List.of(ex.fallbackReason().name()));
        } catch (LlmJsonParseException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, ex.fallbackReason().name(), ex.diagnosticSummary());
            log.warn(
                "LearningPlan JSON parse failed, fallback hit. {} traceId={} requestId={} model={} reason={} diagnostics={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.fallbackReason(),
                ex.diagnosticSummary()
            );
            llmCallLogger.logFallback(llmContext, ex.fallbackReason(), LlmObservabilityHelper.elapsedMs(start));
            return fallback(rulePreview, null, List.of(ex.fallbackReason().name()));
        } catch (Exception ex) {
            LlmFallbackReason reason = llmFailureClassifier.classifyFallback(ex);
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            log.warn(
                "LearningPlan LLM call failed, fallback hit. {} traceId={} requestId={} model={} reason={} error={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                reason,
                ex.getMessage(),
                ex
            );
            llmCallLogger.logFallback(llmContext, reason, LlmObservabilityHelper.elapsedMs(start));
            return fallback(rulePreview, null, List.of(reason.name()));
        }
    }

    private OrchestratedPlan fallback(LearningPlanPreview rulePreview, String llmTraceId, List<String> fallbackReasons) {
        log.warn("LearningPlan fallback applied. fallbackReasons={}", fallbackReasons);
        LearningPlanPreview preview = withDecisionMetadata(rulePreview, "FALLBACK", true, fallbackReasons);
        return new OrchestratedPlan(preview, llmTraceId, PlanSource.RULE_FALLBACK, true, fallbackReasons);
    }

    private LearningPlanPreview merge(
        LearningPlanPreview rulePreview,
        LearningPlanLlmResult parsed,
        String contentSourceType,
        boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
        LearningPlanSummary ruleSummary = rulePreview.summary();
        return new LearningPlanPreview(
            new LearningPlanSummary(
                parsed.headline(),
                ruleSummary.recommendedStartNodeId(),
                ruleSummary.recommendedStartNodeName(),
                ruleSummary.recommendedPace(),
                ruleSummary.estimatedMinutes(),
                ruleSummary.estimatedNodeCount(),
                ruleSummary.estimatedStageCount(),
                parsed.subtitle(),
                parsed.whyNow(),
                parsed.confidence(),
                parsed.currentFocusLabel(),
                parsed.taskTitle(),
                parsed.taskEstimatedMinutes(),
                parsed.taskPriority(),
                parsed.alternatives(),
                parsed.benefits(),
                parsed.nextUnlocks(),
                parsed.nextStepLabel(),
                contentSourceType,
                fallbackApplied,
                fallbackReasons
            ),
            parsed.reasons(),
            parsed.focuses(),
            rulePreview.pathPreview(),
            parsed.taskPreview(),
            rulePreview.adjustments()
        );
    }

    private LearningPlanPreview withDecisionMetadata(
        LearningPlanPreview preview,
        String contentSourceType,
        boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
        LearningPlanSummary summary = preview.summary();
        return new LearningPlanPreview(
            new LearningPlanSummary(
                summary.headline(),
                summary.recommendedStartNodeId(),
                summary.recommendedStartNodeName(),
                summary.recommendedPace(),
                summary.estimatedMinutes(),
                summary.estimatedNodeCount(),
                summary.estimatedStageCount(),
                summary.subtitle(),
                summary.whyNow(),
                summary.confidence(),
                summary.currentFocusLabel(),
                summary.taskTitle(),
                summary.taskEstimatedMinutes(),
                summary.taskPriority(),
                summary.alternatives(),
                summary.benefits(),
                summary.nextUnlocks(),
                summary.nextStepLabel(),
                contentSourceType,
                fallbackApplied,
                fallbackReasons
            ),
            preview.reasons(),
            preview.focuses(),
            preview.pathPreview(),
            preview.taskPreview(),
            preview.adjustments()
        );
    }

    private String traceId(LlmTextResult result) {
        if (result == null) {
            return null;
        }
        return (result.provider() == null ? "llm" : result.provider()) + ":" + (result.model() == null ? "unknown" : result.model());
    }

    private boolean isTruncated(LlmTextResult result) {
        if (result == null || result.usage() == null) {
            return false;
        }
        return result.usage().truncated() || "length".equalsIgnoreCase(result.usage().finishReason());
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
