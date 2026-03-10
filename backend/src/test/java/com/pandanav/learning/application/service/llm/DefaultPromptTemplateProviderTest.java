package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.StageGenerationContext;
import com.pandanav.learning.domain.llm.model.TutorPromptContext;
import com.pandanav.learning.domain.llm.model.TutorReplyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPromptTemplateProviderTest {

    private final DefaultPromptTemplateProvider provider = new DefaultPromptTemplateProvider();

    @Test
    void shouldRenderStagePromptWithCompactSchema() {
        var prompt = provider.buildStagePrompt(
            PromptTemplateKey.STRUCTURE_V1,
            new StageGenerationContext(1L, 1L, "ch1", 1L, "TCP", Stage.STRUCTURE, "understand tcp", null, null)
        );

        assertTrue(prompt.userPrompt().contains("阶段：STRUCTURE"));
        assertTrue(prompt.userPrompt().contains("\"title\""));
        assertTrue(prompt.promptKey().equals("STRUCTURE"));
        assertTrue(prompt.promptVersion().equals("v2"));
    }

    @Test
    void shouldBuildTutorPromptWithModeSwitch() {
        String systemPrompt = provider.buildTutorSystemPrompt(new TutorPromptContext(
            "TRAINING",
            "解释链式法则",
            "复合函数求导",
            "完成三道训练题",
            TutorReplyMode.HINT_ONLY,
            TutorReplyMode.DIRECT_ANSWER
        ));

        assertTrue(systemPrompt.contains("hint_mode: HINT_ONLY"));
        assertTrue(systemPrompt.contains("direct_answer_mode: DIRECT_ANSWER"));
        assertTrue(systemPrompt.contains("学习流程导师"));
    }
}
