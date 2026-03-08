package com.pandanav.learning.domain.llm;

import com.pandanav.learning.domain.llm.model.StageContent;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;

public interface StageContentGenerator {

    StageContent generate(StageGenerationContext context);
}

