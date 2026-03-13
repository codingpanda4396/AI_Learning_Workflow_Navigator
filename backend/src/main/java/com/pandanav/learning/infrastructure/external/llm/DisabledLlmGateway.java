package com.pandanav.learning.infrastructure.external.llm;

import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.infrastructure.exception.InternalServerException;

public class DisabledLlmGateway implements LlmGateway {

    @Override
    public LlmTextResult generate(LlmStage stage, LlmPrompt prompt) {
        throw new InternalServerException("LLM is not enabled or not fully configured.");
    }
}

