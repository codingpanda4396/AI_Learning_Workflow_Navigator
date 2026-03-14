package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import com.pandanav.learning.domain.model.PreviewEnhancement;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.LearningPlanSchemaValidationException;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
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
    private final LearnerStateInterpreter learnerStateInterpreter;
    private final LlmEnhancedPersonalizedNarrativeGenerator llmEnhancedNarrativeGenerator;

    public LearningPlanOrchestrator(
        RuleBasedPlanBuilder ruleBasedPlanBuilder,
        LearningPlanPromptBuilder learningPlanPromptBuilder,
        LearningPlanResultValidator learningPlanResultValidator,
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmProperties llmProperties,
        LlmCallLogger llmCallLogger,
        LearnerStateInterpreter learnerStateInterpreter,
        LlmEnhancedPersonalizedNarrativeGenerator llmEnhancedNarrativeGenerator
    ) {
        this.ruleBasedPlanBuilder = ruleBasedPlanBuilder;
        this.learningPlanPromptBuilder = learningPlanPromptBuilder;
        this.learningPlanResultValidator = learningPlanResultValidator;
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmProperties = llmProperties;
        this.llmCallLogger = llmCallLogger;
        this.learnerStateInterpreter = learnerStateInterpreter;
        this.llmEnhancedNarrativeGenerator = llmEnhancedNarrativeGenerator;
    }

    public OrchestratedPlan preview(LearningPlanPlanningContext context) {
        log.info(
            "LearningPlan preview stage=RULE_SKELETON_START traceId={} diagnosisId={} sessionId={} userId={}",
            com.pandanav.learning.infrastructure.observability.TraceContext.traceId(),
            context.diagnosisId(),
            context.sourceSessionId(),
            context.userId()
        );
        LearningPlanPreview rulePreview = ruleBasedPlanBuilder.build(context);
        log.info(
            "LearningPlan preview stage=RULE_SKELETON_SUCCESS traceId={} startNode={} pace={}",
            com.pandanav.learning.infrastructure.observability.TraceContext.traceId(),
            rulePreview.summary().recommendedStartNodeId(),
            rulePreview.summary().recommendedPace()
        );
        LearnerStateSnapshot learnerState = context.learnerStateSnapshot() == null
            ? learnerStateInterpreter.interpret(context)
            : context.learnerStateSnapshot();
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
                "LearningPlan LLM unavailable. {} enabled={} ready={}",
                logCtx, llmProperties.isEnabled(), llmProperties.isReady()
            );
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel()),
                LlmFailureType.API_ERROR,
                "LLM_NOT_READY",
                -1
            );
            throw new AiGenerationException("PLAN_PREVIEW", "LLM_NOT_READY");
        }

        Instant start = Instant.now();
        try {
            LlmPrompt prompt = learningPlanPromptBuilder.build(context, rulePreview);
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStart(llmContext);
            log.info(
                "LearningPlan preview stage=LLM_ENHANCEMENT_START {} model={} promptKey={} traceId={} diagnosisId={} sessionId={}",
                logCtx,
                llmProperties.getModel(),
                prompt.promptKey(),
                llmContext.traceId(),
                context.diagnosisId(),
                context.sourceSessionId()
            );
            LlmTextResult llmResult = llmGateway.generate(LlmStage.LEARNING_PLAN, prompt);
            llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmResult.model());
            llmCallLogger.logSuccess(
                llmContext,
                LlmCallMetrics.from(llmResult.usage()),
                llmResult.usage() == null ? null : llmResult.usage().finishReason(),
                llmResult.usage() != null && llmResult.usage().truncated()
            );
            if (isTruncated(llmResult)) {
                String details = "finishReason=%s truncated=%s completionTokens=%s maxOutputTokens=%s".formatted(
                    llmResult.usage() == null ? "unknown" : String.valueOf(llmResult.usage().finishReason()),
                    llmResult.usage() != null && llmResult.usage().truncated(),
                    llmResult.usage() == null ? "unknown" : String.valueOf(llmResult.usage().tokenOutput()),
                    prompt.maxOutputTokens() == null ? "unknown" : String.valueOf(prompt.maxOutputTokens())
                );
                llmCallLogger.logStructuredOutputFailure(llmContext, "OUTPUT_TRUNCATED", details);
                log.warn(
                    "LearningPlan LLM output truncated. {} traceId={} requestId={} model={} details={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    details
                );
                llmCallLogger.logFailure(llmContext, LlmFailureType.VALIDATION_ERROR, details, LlmObservabilityHelper.elapsedMs(start));
                throw new AiGenerationException("PLAN_PREVIEW", "OUTPUT_TRUNCATED");
            }

            JsonNode json = llmJsonParser.parse(llmResult.text(), llmContext);
            LearningPlanLlmResult parsed = learningPlanResultValidator.parse(json);
            List<String> errors = new ArrayList<>();
            errors.addAll(learningPlanResultValidator.validateRawTaskPreview(parsed, rulePreview));
            errors.addAll(learningPlanResultValidator.validate(parsed, rulePreview));
            if (!errors.isEmpty()) {
                log.warn(
                    "LearningPlan preview stage=LLM_SCHEMA_VALIDATE_FAILED {} traceId={} requestId={} model={} errors={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    errors
                );
                llmCallLogger.logStructuredOutputFailure(llmContext, "JSON_SCHEMA_MISMATCH", String.join("; ", errors));
                llmCallLogger.logFailure(llmContext, LlmFailureType.VALIDATION_ERROR, String.join("; ", errors), LlmObservabilityHelper.elapsedMs(start));
                throw new AiGenerationException("PLAN_PREVIEW", "JSON_SCHEMA_MISMATCH");
            }

            LearningPlanPreview merged = merge(rulePreview, parsed, "LLM", false, List.of());
            DecisionPlan decisionPlan = buildDecisionPlan(merged);
            PersonalizedNarrative narrative;
            try {
                narrative = llmEnhancedNarrativeGenerator.generate(context, learnerState, decisionPlan, merged);
            } catch (Exception narrativeEx) {
                log.warn(
                    "LearningPlan preview stage=NARRATIVE_ASSEMBLY_FAILED {} traceId={} requestId={} model={} error={}",
                    logCtx,
                    llmContext.traceId(),
                    llmContext.requestId(),
                    llmContext.model(),
                    narrativeEx.getMessage(),
                    narrativeEx
                );
                throw new AiGenerationException("PLAN_PREVIEW", "NARRATIVE_ASSEMBLY_FAILED");
            }
            PreviewEnhancement previewEnhancement = new PreviewEnhancement(parsed.planGuidance(), parsed.strategyComparison());
            log.info(
                "LearningPlan preview stage=LLM_ENHANCEMENT_SUCCESS {} traceId={} requestId={} model={} tokenInput={} tokenOutput={} totalTokens={} latencyMs={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                llmResult.usage() == null ? null : llmResult.usage().tokenInput(),
                llmResult.usage() == null ? null : llmResult.usage().tokenOutput(),
                llmResult.usage() == null ? null : llmResult.usage().totalTokens(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            log.info("Narrative generated. source={} usedLlm={} fallbackReason={}", NarrativeSource.LLM, true, "");
            return new OrchestratedPlan(
                merged,
                traceId(llmResult),
                PlanSource.LLM,
                false,
                List.of(),
                learnerState,
                decisionPlan,
                narrative,
                previewEnhancement,
                NarrativeSource.LLM,
                true,
                null
            );
        } catch (LearningPlanSchemaValidationException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, "JSON_SCHEMA_MISMATCH", String.join("; ", ex.errors()));
            log.warn(
                "LearningPlan preview stage=LLM_SCHEMA_PARSE_FAILED {} traceId={} requestId={} model={} errors={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.errors()
            );
            llmCallLogger.logFailure(llmContext, LlmFailureType.VALIDATION_ERROR, String.join("; ", ex.errors()), LlmObservabilityHelper.elapsedMs(start));
            throw new AiGenerationException("PLAN_PREVIEW", "JSON_SCHEMA_MISMATCH");
        } catch (LlmJsonParseException ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            llmCallLogger.logStructuredOutputFailure(llmContext, "JSON_PARSE_FAILED", ex.diagnosticSummary());
            log.warn(
                "LearningPlan preview stage=LLM_JSON_PARSE_FAILED {} traceId={} requestId={} model={} reason={} diagnostics={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.fallbackReason(),
                ex.diagnosticSummary()
            );
            llmCallLogger.logFailure(llmContext, LlmFailureType.JSON_PARSE_ERROR, ex.diagnosticSummary(), LlmObservabilityHelper.elapsedMs(start));
            throw new AiGenerationException("PLAN_PREVIEW", "JSON_PARSE_FAILED");
        } catch (AiGenerationException ex) {
            throw ex;
        } catch (Exception ex) {
            LlmCallContext llmContext = LlmObservabilityHelper.context(LlmStage.LEARNING_PLAN, llmProperties.getModel());
            log.warn(
                "LearningPlan preview stage=LLM_UNKNOWN_FAILED {} traceId={} requestId={} model={} error={}",
                logCtx,
                llmContext.traceId(),
                llmContext.requestId(),
                llmContext.model(),
                ex.getMessage(),
                ex
            );
            llmCallLogger.logFailure(llmContext, LlmFailureType.UNKNOWN_ERROR, ex.getMessage(), LlmObservabilityHelper.elapsedMs(start));
            throw new AiGenerationException("PLAN_PREVIEW", "UNKNOWN_ERROR");
        }
    }

    private DecisionPlan buildDecisionPlan(LearningPlanPreview preview) {
        LearningPlanSummary summary = preview.summary();
        return new DecisionPlan(
            summary.recommendedStartNodeId(),
            summary.recommendedPace(),
            summary.alternatives() == null ? List.of() : summary.alternatives(),
            preview.reasons() == null ? summary.whyNow() : preview.reasons().stream()
                .filter(item -> "RISK_CONTROL".equalsIgnoreCase(item.type()))
                .map(item -> item.description())
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse(summary.whyNow()),
            preview.reasons() == null ? List.of() : preview.reasons()
        );
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
        List<String> fallbackReasons,
        LearnerStateSnapshot learnerStateSnapshot,
        DecisionPlan decisionPlan,
        PersonalizedNarrative personalizedNarrative,
        PreviewEnhancement previewEnhancement,
        NarrativeSource narrativeSource,
        boolean narrativeUsedLlm,
        String narrativeFallbackReason
    ) {
    }
}
