package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.mock;

class AiTutorServiceImplStreamingTest {

    @Test
    void streamChat_emitsMetaThenChunksWithoutHandleError() {
        AiTutorServiceImpl impl = newImpl(new StubLlmClient(
                Flux.just("hello", " world"),
                Mono.just("{\"correct\":true,\"diagnosis\":\"ok\",\"suggestion\":\"keep going\"}")
        ));

        List<AiTutorStreamEvent> events = impl.streamChat(chatRequest("Explain binary search"))
                .collectList()
                .block();

        assertEquals(3, events.size());
        assertEquals("meta", events.get(0).event());
        assertEquals("LLM", events.get(0).data().get("source"));
        assertEquals("delta", events.get(1).event());
        assertEquals("hello", events.get(1).data().get("text"));
        assertEquals("delta", events.get(2).event());
        assertEquals(" world", events.get(2).data().get("text"));
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
    void streamChat_fallsBackWhenStreamErrorsBeforeFirstChunk() {
        AiTutorServiceImpl impl = newImpl(new StubLlmClient(
                Flux.error(new IllegalStateException("provider down")),
                Mono.just("{\"correct\":true}")
        ));

        List<AiTutorStreamEvent> events = impl.streamChat(chatRequest("Explain binary search"))
                .collectList()
                .block();

        assertEquals(2, events.size());
        assertEquals("meta", events.get(0).event());
        assertEquals("FALLBACK", events.get(0).data().get("source"));
        assertEquals("delta", events.get(1).event());
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
        request.setMessage(message);
        AiTutorChatRequest.Context context = new AiTutorChatRequest.Context();
        context.setStep(1);
        context.setPhase("understand");
        context.setKnowledge("binary-search");
        context.setKnowledgeLabel("Binary Search");
        request.setContext(context);
        return request;
    }

    private record StubLlmClient(Flux<String> streamResult, Mono<String> feedbackResult) implements LlmClient {

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            return "unused";
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
        public boolean isLiveProviderReady() {
            return true;
        }
    }
}
