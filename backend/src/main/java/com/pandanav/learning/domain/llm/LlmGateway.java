package com.pandanav.learning.domain.llm;

import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;

public interface LlmGateway {

    LlmTextResult generate(LlmPrompt prompt);
}

