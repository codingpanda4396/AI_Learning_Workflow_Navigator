package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.llm.OpenAiCompatibleLlmClientAdapter;
import navigator.application.llm.LlmProperties;
import navigator.application.tutor.TutorExplainPrefetchCoordinator;
import navigator.application.tutor.fallback.TutorFallbackRegistry;
import navigator.infrastructure.cache.TutorContentCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AiTutorServiceImplParseTest {

    @Test
    void stripMarkdownFence_removesFence() {
        String in = "```json\n{\"a\":1}\n```";
        assertEquals("{\"a\":1}", AiTutorServiceImpl.stripMarkdownFence(in));
    }

    @Test
    void parseFeedbackJson_validObject() {
        AiTutorServiceImpl impl = newImpl();
        String raw = "{\"correct\":true,\"diagnosis\":\"好\",\"suggestion\":\"继续\",\"nextHint\":\"想一下\"}";
        TaskFeedbackResponse r = impl.parseFeedbackJson(raw);
        assertTrue(r.isCorrect());
        assertEquals("好", r.getComment());
        assertEquals("继续", r.getSuggestion());
        assertEquals("想一下", r.getNextHint());
    }

    @Test
    void parseFeedbackJson_withFenceAndNoise() {
        AiTutorServiceImpl impl = newImpl();
        String raw = "这是结果：\n```json\n{\"correct\":false,\"diagnosis\":\"d\",\"suggestion\":\"s\",\"nextHint\":\"n\"}\n```\n";
        TaskFeedbackResponse r = impl.parseFeedbackJson(raw);
        assertFalse(r.isCorrect());
        assertEquals("d", r.getComment());
    }

    @Test
    void parseFeedbackJson_leadingTextBeforeBrace() {
        AiTutorServiceImpl impl = newImpl();
        String raw = "下面是 JSON：\n{\"correct\":true,\"diagnosis\":\"好\",\"suggestion\":\"继续\",\"nextHint\":\"想\"}\n谢谢";
        TaskFeedbackResponse r = impl.parseFeedbackJson(raw);
        assertTrue(r.isCorrect());
        assertEquals("好", r.getComment());
    }

    @Test
    void parseFeedbackJson_invalidFallback() {
        AiTutorServiceImpl impl = newImpl();
        TaskFeedbackResponse r = impl.parseFeedbackJson("not json");
        assertFalse(r.isCorrect());
        assertTrue(r.getComment().contains("无法解析"));
    }

    private static AiTutorServiceImpl newImpl() {
        LlmProperties p = new LlmProperties();
        p.setEnabled(false);
        ObjectMapper om = new ObjectMapper();
        OpenAiCompatibleLlmClientAdapter client = new OpenAiCompatibleLlmClientAdapter(
                p,
                new RestTemplateBuilder(),
                WebClient.builder(),
                om);
        return new AiTutorServiceImpl(
                client,
                new TutorContentCache(),
                mock(TutorExplainPrefetchCoordinator.class),
                new TutorFallbackRegistry(),
                om);
    }
}
