package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;
import com.pandanav.learning.domain.llm.model.TutorReplyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPromptTemplateProviderTest {

    private final DefaultPromptTemplateProvider provider = new DefaultPromptTemplateProvider();

    @Test
    void shouldRenderStagePromptWithCompactSchema() {
        var prompt = provider.buildStagePrompt(
            PromptTemplateKey.STRUCTURE_V1,
            new StageGenerationContext(1L, 1L, "ch1", 1L, "TCP", Stage.STRUCTURE, "understand tcp", null, null)
        );

        assertTrue(prompt.userPrompt().contains("Stage: STRUCTURE"));
        assertTrue(prompt.userPrompt().contains("\"title\""));
        assertEquals("STRUCTURE", prompt.promptKey());
        assertEquals("v2", prompt.promptVersion());
    }

    @Test
    void shouldBuildTutorPromptWithModeSwitch() {
        String systemPrompt = provider.buildTutorSystemPrompt(new TutorPromptContext(
            "TRAINING",
            "objective",
            "concept",
            "goal",
            TutorReplyMode.HINT_ONLY,
            TutorReplyMode.DIRECT_ANSWER
        ));

        assertTrue(systemPrompt.contains("hint_mode: HINT_ONLY"));
        assertTrue(systemPrompt.contains("direct_answer_mode: DIRECT_ANSWER"));
        assertTrue(systemPrompt.contains("You are a learning tutor."));
    }
}
