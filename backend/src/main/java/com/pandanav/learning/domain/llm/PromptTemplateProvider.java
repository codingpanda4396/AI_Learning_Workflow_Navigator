package com.pandanav.learning.domain.llm;

import com.pandanav.learning.domain.llm.model.ConceptDecomposeContext;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.GoalDiagnosisContext;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;

public interface PromptTemplateProvider {

    LlmPrompt buildStagePrompt(PromptTemplateKey key, StageGenerationContext context);

    LlmPrompt buildEvaluationPrompt(PromptTemplateKey key, EvaluationContext context);

    LlmPrompt buildGoalDiagnosisPrompt(GoalDiagnosisContext context);

    LlmPrompt buildConceptDecomposePrompt(ConceptDecomposeContext context);

    String buildTutorSystemPrompt(TutorPromptContext context);
}
