package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanDecisionValidationResult;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import com.pandanav.learning.domain.model.PreviewEnhancement;
import com.pandanav.learning.domain.service.DefaultDecisionFactory;
import com.pandanav.learning.domain.service.LearningPlanDecisionValidator;
import com.pandanav.learning.domain.service.PlanCandidatePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LearningPlanOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanOrchestrator.class);

    private final LearnerStateAssembler learnerStateAssembler;
    private final PlanCandidatePlanner planCandidatePlanner;
    private final DefaultDecisionFactory defaultDecisionFactory;
    private final LearningPlanDecisionLlmService decisionLlmService;
    private final LearningPlanDecisionValidator decisionValidator;
    private final LearningPlanPreviewViewAssembler previewViewAssembler;
    private final LearnerStateInterpreter learnerStateInterpreter;
    private final LlmEnhancedPersonalizedNarrativeGenerator llmEnhancedNarrativeGenerator;

    public LearningPlanOrchestrator(
        LearnerStateAssembler learnerStateAssembler,
        PlanCandidatePlanner planCandidatePlanner,
        DefaultDecisionFactory defaultDecisionFactory,
        LearningPlanDecisionLlmService decisionLlmService,
        LearningPlanDecisionValidator decisionValidator,
        LearningPlanPreviewViewAssembler previewViewAssembler,
        LearnerStateInterpreter learnerStateInterpreter,
        LlmEnhancedPersonalizedNarrativeGenerator llmEnhancedNarrativeGenerator
    ) {
        this.learnerStateAssembler = learnerStateAssembler;
        this.planCandidatePlanner = planCandidatePlanner;
        this.defaultDecisionFactory = defaultDecisionFactory;
        this.decisionLlmService = decisionLlmService;
        this.decisionValidator = decisionValidator;
        this.previewViewAssembler = previewViewAssembler;
        this.learnerStateInterpreter = learnerStateInterpreter;
        this.llmEnhancedNarrativeGenerator = llmEnhancedNarrativeGenerator;
    }

    public OrchestratedPlan preview(LearningPlanPlanningContext context) {
        LearnerStateSnapshot learnerStateSnapshot = context.learnerStateSnapshot() == null
            ? learnerStateInterpreter.interpret(context)
            : context.learnerStateSnapshot();
        LearnerState learnerState = learnerStateAssembler.assemble(context);
        PlanCandidateSet candidateSet = planCandidatePlanner.plan(context, learnerState);
        LlmPlanDecisionResult defaultDecision = defaultDecisionFactory.create(context, learnerState, candidateSet);
        Optional<LlmPlanDecisionResult> llmDecision = decisionLlmService.decide(learnerState, candidateSet);
        LearningPlanDecisionValidationResult validatedDecision = decisionValidator.validateAndFallback(
            context,
            learnerState,
            candidateSet,
            llmDecision.orElse(null),
            defaultDecision
        );
        LearningPlanPreview preview = previewViewAssembler.assemble(
            context,
            learnerState,
            candidateSet,
            validatedDecision.finalDecision(),
            validatedDecision.fallbackLevel(),
            validatedDecision.fallbackReasons()
        );
        DecisionPlan decisionPlan = buildDecisionPlan(preview);
        PersonalizedNarrative narrative = llmEnhancedNarrativeGenerator.generate(
            context,
            learnerStateSnapshot,
            decisionPlan,
            preview
        );
        PlanSource planSource = validatedDecision.fallbackApplied() ? PlanSource.RULE_FALLBACK : PlanSource.LLM;
        NarrativeSource narrativeSource = validatedDecision.fallbackApplied() ? NarrativeSource.FALLBACK : NarrativeSource.LLM;
        String fallbackReason = validatedDecision.fallbackApplied() ? String.join(",", validatedDecision.fallbackReasons()) : null;
        log.info(
            "LearningPlan preview decided. goalId={} chapterId={} selectedConcept={} selectedStrategy={} fallbackLevel={} fallbackReasons={}",
            context.goalId(),
            context.chapterId(),
            validatedDecision.finalDecision().selectedConceptId(),
            validatedDecision.finalDecision().selectedStrategyCode(),
            validatedDecision.fallbackLevel(),
            validatedDecision.fallbackReasons()
        );
        return new OrchestratedPlan(
            preview,
            null,
            planSource,
            validatedDecision.fallbackApplied(),
            withFallbackLevel(validatedDecision),
            learnerStateSnapshot,
            decisionPlan,
            narrative,
            null,
            narrativeSource,
            !validatedDecision.fallbackApplied(),
            fallbackReason
        );
    }

    private List<String> withFallbackLevel(LearningPlanDecisionValidationResult result) {
        if (!result.fallbackApplied()) {
            return List.of();
        }
        return java.util.stream.Stream.concat(
            java.util.stream.Stream.of("fallback_level:" + result.fallbackLevel().name()),
            (result.fallbackReasons() == null ? List.<String>of() : result.fallbackReasons()).stream()
        ).toList();
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
