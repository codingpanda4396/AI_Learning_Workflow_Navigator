package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.AiTutorChatRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.tutor.AiTutorChatResult;
import navigator.application.tutor.AiTutorFeedbackPayload;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorService;
import navigator.application.tutor.AiTutorStreamEvent;
import navigator.application.tutor.AiTutorTextResult;
import navigator.application.tutor.TutorExplainPrefetchCoordinator;
import navigator.application.tutor.fallback.TutorFallbackRegistry;
import navigator.application.tutor.fallback.TutorKnowledgeNormalizer;
import navigator.application.tutor.prompt.TutorPromptTemplates;
import navigator.infrastructure.cache.TutorContentCache;
import navigator.infrastructure.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private static final Logger log = LoggerFactory.getLogger(AiTutorServiceImpl.class);

    public static final String SOURCE_CACHE = "CACHE";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_LLM = "LLM";
    private static final String STREAM_INTERRUPTED_PREFIX = "\n\n\uFF08\u8FDE\u63A5\u4E2D\u65AD\uFF0C\u4EE5\u4E0B\u4E3A\u672C\u5730\u63D0\u793A\uFF09\n";

    private final LlmClient llmClient;
    private final TutorContentCache tutorContentCache;
    private final TutorExplainPrefetchCoordinator explainPrefetchCoordinator;
    private final TutorFallbackRegistry tutorFallbackRegistry;
    private final ObjectMapper objectMapper;

    public AiTutorServiceImpl(LlmClient llmClient,
                              TutorContentCache tutorContentCache,
                              TutorExplainPrefetchCoordinator explainPrefetchCoordinator,
                              TutorFallbackRegistry tutorFallbackRegistry,
                              ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.tutorContentCache = tutorContentCache;
        this.explainPrefetchCoordinator = explainPrefetchCoordinator;
        this.tutorFallbackRegistry = tutorFallbackRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiTutorChatResult chat(AiTutorChatRequest request) {
        AiTutorChatRequest.Context ctx = request.getContext();
        String userMsg = request.getMessage() != null ? request.getMessage().trim() : "";
        if (userMsg.isEmpty()) {
            return new AiTutorChatResult(SOURCE_FALLBACK, tutorFallbackRegistry.embeddedChatFallback(
                    TutorKnowledgeNormalizer.UNKNOWN, ""));
        }
        int stepNum = ctx.getStep() != null ? ctx.getStep() : 1;
        String phase = ctx.getPhase() != null ? ctx.getPhase().trim() : "";
        String knowledgeKey = ctx.getKnowledge() != null ? ctx.getKnowledge().trim() : "";
        String knowledgeLabel = ctx.getKnowledgeLabel() != null ? ctx.getKnowledgeLabel().trim() : "";
        String forNorm = !knowledgeKey.isBlank() ? knowledgeKey : knowledgeLabel;
        String norm = TutorKnowledgeNormalizer.normalize(forNorm);
        String display = !knowledgeLabel.isBlank() ? knowledgeLabel
                : (!knowledgeKey.isBlank() ? knowledgeKey : "\u5f53\u524d\u77e5\u8bc6\u70b9");
        String system = TutorPromptTemplates.embeddedChatSystemPrompt(stepNum, phase, display, norm);
        if (llmClient.isLiveProviderReady()) {
            try {
                String raw = llmClient.chat(system, userMsg);
                if (raw != null && !raw.isBlank()) {
                    return new AiTutorChatResult(SOURCE_LLM, raw.trim());
                }
            } catch (Exception e) {
                log.warn("AiTutor chat LLM failed 鈥?{}: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        String fallback = tutorFallbackRegistry.embeddedChatFallback(norm, userMsg);
        return new AiTutorChatResult(SOURCE_FALLBACK, fallback);
    }

    @Override
    public Flux<AiTutorStreamEvent> streamChat(AiTutorChatRequest request) {
        return Flux.defer(() -> {
            AiTutorChatRequest.Context ctx = request.getContext();
            String userMsg = request.getMessage() != null ? request.getMessage().trim() : "";
            if (userMsg.isEmpty()) {
                return fallbackStream(TutorKnowledgeNormalizer.UNKNOWN, "");
            }

            int stepNum = ctx.getStep() != null ? ctx.getStep() : 1;
            String phase = ctx.getPhase() != null ? ctx.getPhase().trim() : "";
            String knowledgeKey = ctx.getKnowledge() != null ? ctx.getKnowledge().trim() : "";
            String knowledgeLabel = ctx.getKnowledgeLabel() != null ? ctx.getKnowledgeLabel().trim() : "";
            String forNorm = !knowledgeKey.isBlank() ? knowledgeKey : knowledgeLabel;
            String norm = TutorKnowledgeNormalizer.normalize(forNorm);
            String display = !knowledgeLabel.isBlank() ? knowledgeLabel
                    : (!knowledgeKey.isBlank() ? knowledgeKey : "\u5f53\u524d\u77e5\u8bc6\u70b9");
            String system = TutorPromptTemplates.embeddedChatSystemPrompt(stepNum, phase, display, norm);

            if (!llmClient.isLiveProviderReady()) {
                return fallbackStream(norm, userMsg);
            }

            AtomicBoolean sawNonEmpty = new AtomicBoolean(false);
            Flux<String> chunks = llmClient.chatStream(system, userMsg)
                    .filter(chunk -> chunk != null && !chunk.isEmpty())
                    .doOnNext(chunk -> sawNonEmpty.set(true));

            Flux<AiTutorStreamEvent> llmEvents = chunks.switchOnFirst((signal, flux) -> {
                if (signal.isOnNext()) {
                    return Flux.concat(
                            Flux.just(AiTutorStreamEvent.meta(SOURCE_LLM)),
                            flux.map(AiTutorStreamEvent::delta)
                    );
                }
                if (signal.isOnComplete()) {
                    return Flux.empty();
                }
                return Flux.error(signal.getThrowable());
            });

            return llmEvents
                    .switchIfEmpty(fallbackStream(norm, userMsg))
                    .onErrorResume(e -> {
                        log.warn("AiTutor chat stream LLM failed 鈥?{}: {}", e.getClass().getSimpleName(), e.getMessage());
                        if (!sawNonEmpty.get()) {
                            return fallbackStream(norm, userMsg);
                        }
                        return Flux.just(AiTutorStreamEvent.delta(
                                STREAM_INTERRUPTED_PREFIX + tutorFallbackRegistry.embeddedChatFallback(norm, userMsg)));
                    });
        });
    }

    @Override
    public AiTutorTextResult getPrompt(String step, String knowledgePoint) {
        String norm = TutorKnowledgeNormalizer.normalize(knowledgePoint);
        String key = TutorContentCache.promptKey(step, norm);
        var hit = tutorContentCache.getPrompt(key);
        if (hit.isPresent()) {
            return new AiTutorTextResult(SOURCE_CACHE, hit.get());
        }
        String text = TutorPromptTemplates.renderTemplatePrompt(knowledgePoint);
        tutorContentCache.putPrompt(key, text);
        return new AiTutorTextResult(SOURCE_FALLBACK, text);
    }

    @Override
    public AiTutorTextResult getExplain(String step, String knowledgePoint) {
        String norm = TutorKnowledgeNormalizer.normalize(knowledgePoint);
        String key = TutorContentCache.explainKey(step, norm);
        var justDone = explainPrefetchCoordinator.pollCompletedLlmExplain(key);
        if (justDone.isPresent()) {
            return new AiTutorTextResult(SOURCE_LLM, justDone.get());
        }
        var hit = tutorContentCache.getExplain(key);
        if (hit.isPresent()) {
            return new AiTutorTextResult(SOURCE_CACHE, hit.get());
        }
        explainPrefetchCoordinator.ensurePrefetch(key, knowledgePoint);
        String fallback = tutorFallbackRegistry.explainFallback(norm, step);
        return new AiTutorTextResult(SOURCE_FALLBACK, fallback);
    }

    @Override
    public AiTutorFeedbackResult getFeedback(String step, String knowledgePoint, String userAnswer) {
        String norm = TutorKnowledgeNormalizer.normalize(knowledgePoint);
        String st = step != null ? step : "";
        if (llmClient.isLiveProviderReady()) {
            try {
                String raw = llmClient.chatForFeedback(
                        TutorPromptTemplates.feedbackSystemPrompt(knowledgePoint),
                        TutorPromptTemplates.feedbackUserPrompt(userAnswer));
                TaskFeedbackResponse parsed = parseFeedbackBody(raw);
                if (parsed != null) {
                    return new AiTutorFeedbackResult(SOURCE_LLM, parsed);
                }
                return new AiTutorFeedbackResult(SOURCE_FALLBACK,
                        tutorFallbackRegistry.feedbackWhenLlmJsonUnreliable(norm, st, userAnswer));
            } catch (Exception e) {
                log.warn("AiTutor feedback LLM failed 鈥?{}: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        TaskFeedbackResponse fb = tutorFallbackRegistry.feedbackFallback(norm, st, userAnswer);
        return new AiTutorFeedbackResult(SOURCE_FALLBACK, fb);
    }

    @Override
    public void prefetch(String step, String knowledgePoint) {
        String norm = TutorKnowledgeNormalizer.normalize(knowledgePoint);
        String key = TutorContentCache.explainKey(step, norm);
        explainPrefetchCoordinator.ensurePrefetch(key, knowledgePoint);
    }

    TaskFeedbackResponse parseFeedbackJson(String raw) {
        TaskFeedbackResponse r = parseFeedbackBody(raw);
        return r != null ? r : malformedLlmFeedbackResponse();
    }

    private TaskFeedbackResponse parseFeedbackBody(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = extractJson(raw);
        try {
            AiTutorFeedbackPayload p = objectMapper.readValue(cleaned, AiTutorFeedbackPayload.class);
            if (p.getDiagnosis() == null && p.getSuggestion() == null && p.getNextHint() == null) {
                return null;
            }
            String comment = blankToText(p.getDiagnosis(), "\u6211\u5df2\u770b\u8fc7\u4f60\u7684\u56de\u7b54\u3002");
            String suggestion = blankToText(p.getSuggestion(), "\u8bd5\u7740\u7528\u4e00\u53e5\u8bdd\u6293\u4f4f\u6838\u5fc3\u6982\u5ff5\u3002");
            String nextHint = p.getNextHint() != null && !p.getNextHint().isBlank() ? p.getNextHint().trim() : null;
            return new TaskFeedbackResponse(
                    p.isCorrect(),
                    comment,
                    suggestion,
                    null,
                    null,
                    nextHint,
                    null);
        } catch (Exception e) {
            log.debug("AiTutor feedback JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    private Flux<AiTutorStreamEvent> fallbackStream(String norm, String userMsg) {
        return Flux.just(
                AiTutorStreamEvent.meta(SOURCE_FALLBACK),
                AiTutorStreamEvent.delta(tutorFallbackRegistry.embeddedChatFallback(norm, userMsg))
        );
    }

    private static TaskFeedbackResponse malformedLlmFeedbackResponse() {
        return new TaskFeedbackResponse(
                false,
                "\u6682\u65f6\u65e0\u6cd5\u89e3\u6790\u5bfc\u5e08\u53cd\u9988\uff0c\u8bf7\u518d\u8bd5\u4e00\u6b21\u3002",
                "\u53ef\u4ee5\u6362\u4e00\u53e5\u8bdd\u63cf\u8ff0\u4f60\u7684\u7406\u89e3\uff0c\u6216\u8865\u5145\u4e00\u4e2a\u4f60\u770b\u5230\u7684\u5173\u952e\u70b9\u3002",
                null,
                null,
                "\u8bd5\u7740\u5148\u8bf4\u51fa\uff1a\u8fd9\u4e2a\u6982\u5ff5\u8ba9\u4f60\u8054\u60f3\u5230\u4ec0\u4e48\u5177\u4f53\u753b\u9762\uff1f",
                null);
    }

    private static String blankToText(String s, String d) {
        return s == null || s.isBlank() ? d : s.trim();
    }

    static String stripMarkdownFence(String s) {
        String t = s;
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            }
            int end = t.indexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    static String extractJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = stripMarkdownFence(raw.trim()).trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1).trim();
        }
        return s;
    }

    static String extractFirstJsonObject(String s) {
        return extractJson(s);
    }
}
