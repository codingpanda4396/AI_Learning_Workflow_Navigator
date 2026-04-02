package navigator.application.scaffold;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.application.llm.LlmProperties;
import navigator.application.llm.MockLlmGateway;
import navigator.application.llm.OpenAiCompatibleLlmGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StageScaffoldWorkbenchComposerTest {

    @Test
    void fastMode_doesNotCallOpenAiGateway() {
        OpenAiCompatibleLlmGateway open = mock(OpenAiCompatibleLlmGateway.class);
        MockLlmGateway mockGw = new MockLlmGateway();
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);

        StageScaffoldWorkbenchComposer composer = new StageScaffoldWorkbenchComposer(
                mockGw, open, props, new ObjectMapper());

        LearningActionCard card = LearningActionCard.builder()
                .actionId("card_a")
                .title("题")
                .goal("目标")
                .instructions("说明")
                .build();
        StageScaffold stage = StageScaffold.builder()
                .stageKey("STRUCTURE")
                .currentActionId("card_a")
                .actionCards(List.of(card))
                .phaseGoal("先搭结构")
                .build();

        composer.composeWorkbench(LearningScaffoldPackRegistry.DFS_BFS, stage, WorkbenchMode.FAST);

        verify(open, never()).generateScaffoldReply(any(), any());
    }
}
