package com.pandanav.learning.domain.llm;

import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;

public interface PromptTemplateProvider {

    LlmPrompt buildStagePrompt(PromptTemplateKey key, StageGenerationContext context);

    LlmPrompt buildEvaluationPrompt(PromptTemplateKey key, EvaluationContext context);
}

