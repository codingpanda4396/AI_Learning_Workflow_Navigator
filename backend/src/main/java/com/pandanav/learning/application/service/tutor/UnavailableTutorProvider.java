package com.pandanav.learning.application.service.tutor;

import com.pandanav.learning.infrastructure.exception.AiGenerationException;

public class UnavailableTutorProvider implements TutorProvider {

    @Override
    public TutorProviderReply generateReply(TutorProviderRequest request) {
        throw new AiGenerationException("TUTOR_REPLY", "LLM_NOT_READY");
    }
}
