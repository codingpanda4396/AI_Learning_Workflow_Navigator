package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmEnhancedPersonalizedNarrativeGenerator {

    private final RuleBasedPersonalizedNarrativeGenerator ruleBasedGenerator;

    public LlmEnhancedPersonalizedNarrativeGenerator(RuleBasedPersonalizedNarrativeGenerator ruleBasedGenerator) {
        this.ruleBasedGenerator = ruleBasedGenerator;
    }

    public PersonalizedNarrative generate(
        LearningPlanPlanningContext context,
        LearnerStateSnapshot learnerStateSnapshot,
        DecisionPlan decisionPlan,
        LearningPlanPreview preview
    ) {
        PersonalizedNarrative base = ruleBasedGenerator.generate(context, learnerStateSnapshot, decisionPlan, preview);
        List<String> whatISaw = new ArrayList<>(base.whatISaw());
        if (preview.summary().subtitle() != null && !preview.summary().subtitle().isBlank()) {
            whatISaw.add("本轮策略强调：" + preview.summary().subtitle());
        }
        String whyFits = preview.summary().whyNow() != null && !preview.summary().whyNow().isBlank()
            ? preview.summary().whyNow()
            : base.whyThisPlanFitsYou();
        String adaptationHint = preview.summary().nextStepLabel() != null && !preview.summary().nextStepLabel().isBlank()
            ? "完成本轮后会优先进入「" + preview.summary().nextStepLabel() + "」，并继续根据你的表现微调路径。"
            : base.adaptationHint();
        return new PersonalizedNarrative(
            base.learnerState(),
            whatISaw.stream().limit(4).toList(),
            whyFits,
            base.mainRiskIfSkip(),
            base.thisRoundBoundary(),
            adaptationHint
        );
    }
}
