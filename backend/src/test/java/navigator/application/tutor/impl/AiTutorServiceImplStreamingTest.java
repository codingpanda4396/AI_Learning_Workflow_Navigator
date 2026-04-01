package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.BusinessException;
import navigator.api.dto.AiTutorChatRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorStreamEvent;
import navigator.application.tutor.TutorExplainPrefetchCoordinator;
import navigator.application.tutor.fallback.TutorFallbackRegistry;
import navigator.infrastructure.cache.TutorContentCache;
import navigator.infrastructure.llm.LlmClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AiTutorServiceImplStreamingTest {

    @Test
    void streamChat_emitsMetaReplyDeltasAndDonePayload() {
        AiTutorServiceImpl impl = newImpl(new StubLlmClient(
                Flux.just("<reply>hello", " world</reply><can_proceed>true</can_proceed><completion_hint>ok</completion_hint><summary>done</summary><final_draft>final text</final_draft>"),
                Mono.just("{\"correct\":true,\"diagnosis\":\"ok\",\"suggestion\":\"keep going\"}")
        ));

        List<AiTutorStreamEvent> events = impl.streamChat(chatRequest("Explain binary search"))
                .collectList()
                .block();

        assertEquals(3, events.size());
        assertEquals("meta", events.get(0).event());
        assertEquals("LLM", events.get(0).data().get("source"));
        assertEquals("delta", events.get(1).event());
        assertEquals("hello world", events.get(1).data().get("text"));
        assertEquals("done", events.get(2).event());
        assertEquals("true", events.get(2).data().get("canProceed"));
        assertEquals("final text", events.get(2).data().get("finalDraft"));
    }

    @Test
    void chat_throwsExplicitErrorWhenProviderDisabled() {
        AiTutorServiceImpl impl = newImpl(new DisabledLlmClient());
        assertThrows(BusinessException.class, () -> impl.chat(chatRequest("Explain binary search")));
    }

    @Test
    void getFeedback_fallsBackWhenLlmTimesOut() {
        AiTutorServiceImpl impl = newImpl(new StubLlmClient(
                Flux.empty(),
                Mono.error(new IllegalStateException("timeout"))
        ));

        AiTutorFeedbackResult result = impl.getFeedback("1", "binary-search", "my answer");

        assertEquals("FALLBACK", result.source());
        TaskFeedbackResponse response = result.response();
        assertNotEquals("LLM", response.getSource());
        assertFalse(response.getComment().isBlank());
    }

    @Test
    void streamChat_emitsErrorWhenStreamFailsBeforeReply() {
        AiTutorServiceImpl impl = newImpl(new StubLlmClient(
                Flux.error(new IllegalStateException("provider down")),
                Mono.just("{\"correct\":true}")
        ));

        List<AiTutorStreamEvent> events = impl.streamChat(chatRequest("Explain binary search"))
                .collectList()
                .block();

        assertEquals(2, events.size());
        assertEquals("meta", events.get(0).event());
        assertEquals("LLM", events.get(0).data().get("source"));
        assertEquals("error", events.get(1).event());
    }

    private static AiTutorServiceImpl newImpl(LlmClient llmClient) {
        return new AiTutorServiceImpl(
                llmClient,
                new TutorContentCache(),
                mock(TutorExplainPrefetchCoordinator.class),
                new TutorFallbackRegistry(),
                new ObjectMapper()
        );
    }

    private static AiTutorChatRequest chatRequest(String message) {
        AiTutorChatRequest request = new AiTutorChatRequest();
        AiTutorChatRequest.Message msg = new AiTutorChatRequest.Message();
        msg.setRole("user");
        msg.setContent(message);
        request.setMessages(List.of(msg));
        AiTutorChatRequest.Context context = new AiTutorChatRequest.Context();
        context.setStep(1);
        context.setPhase("UNDERSTANDING");
        context.setKnowledge("binary-search");
        context.setKnowledgeLabel("Binary Search");
        request.setContext(context);
        return request;
    }

    private record StubLlmClient(Flux<String> streamResult, Mono<String> feedbackResult) implements LlmClient {

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            return "<reply>ok</reply><can_proceed>false</can_proceed><completion_hint>继续</completion_hint><summary>已有进展</summary><final_draft></final_draft>";
        }

        @Override
        public Flux<String> chatStream(String systemPrompt, String userPrompt) {
            return streamResult;
        }

        @Override
        public String chatForFeedback(String systemPrompt, String userPrompt) {
            return feedbackResult.block();
        }

        @Override
        public String chatForScaffold(String systemPrompt, String userPrompt) {
            return "unused";
        }

        @Override
        public boolean isLiveProviderReady() {
            return true;
        }
    }

    private static final class DisabledLlmClient implements LlmClient {
        @Override
        public String chat(String systemPrompt, String userPrompt) {
            throw new IllegalStateException("disabled");
        }

        @Override
        public Flux<String> chatStream(String systemPrompt, String userPrompt) {
            return Flux.error(new IllegalStateException("disabled"));
        }

        @Override
        public String chatForFeedback(String systemPrompt, String userPrompt) {
            throw new IllegalStateException("disabled");
        }

        @Override
        public String chatForScaffold(String systemPrompt, String userPrompt) {
            throw new IllegalStateException("disabled");
        }

        @Override
        public boolean isLiveProviderReady() {
            return false;
        }
    }
}
