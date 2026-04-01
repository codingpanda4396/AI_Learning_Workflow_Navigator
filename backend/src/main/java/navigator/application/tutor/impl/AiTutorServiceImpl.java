package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private static final Logger log = LoggerFactory.getLogger(AiTutorServiceImpl.class);

    public static final String SOURCE_CACHE = "CACHE";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_LLM = "LLM";
    private static final String LLM_UNAVAILABLE_MESSAGE = "当前导师暂不可用，请稍后重试。";
    private static final Pattern REPLY_PATTERN = Pattern.compile("<reply>(.*?)</reply>", Pattern.DOTALL);
    private static final Pattern CAN_PROCEED_PATTERN = Pattern.compile("<can_proceed>(.*?)</can_proceed>", Pattern.DOTALL);
    private static final Pattern COMPLETION_HINT_PATTERN = Pattern.compile("<completion_hint>(.*?)</completion_hint>", Pattern.DOTALL);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("<summary>(.*?)</summary>", Pattern.DOTALL);
    private static final Pattern FINAL_DRAFT_PATTERN = Pattern.compile("<final_draft>(.*?)</final_draft>", Pattern.DOTALL);

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
        requireLiveProvider();
        PromptBundle prompt = buildPrompt(request);
        try {
            String raw = llmClient.chat(prompt.systemPrompt(), prompt.userPrompt());
            StructuredTutorReply parsed = parseStructuredReply(raw);
            return new AiTutorChatResult(
                    SOURCE_LLM,
                    parsed.reply(),
                    parsed.canProceed(),
                    parsed.finalDraft(),
                    parsed.completionHint(),
                    parsed.summary()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AiTutor chat LLM failed {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, LLM_UNAVAILABLE_MESSAGE);
        }
    }

    @Override
    public Flux<AiTutorStreamEvent> streamChat(AiTutorChatRequest request) {
        if (!llmClient.isLiveProviderReady()) {
            return Flux.error(new BusinessException(BusinessErrorCode.INTERNAL_ERROR, LLM_UNAVAILABLE_MESSAGE));
        }

        PromptBundle prompt = buildPrompt(request);
        ReplyDeltaExtractor extractor = new ReplyDeltaExtractor();
        StringBuilder raw = new StringBuilder();

        Flux<AiTutorStreamEvent> deltas = llmClient.chatStream(prompt.systemPrompt(), prompt.userPrompt())
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .concatMap(chunk -> {
                    raw.append(chunk);
                    List<String> pieces = extractor.consume(chunk);
                    if (pieces.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pieces).map(AiTutorStreamEvent::delta);
                });

        return Flux.concat(
                        Flux.just(AiTutorStreamEvent.meta(SOURCE_LLM)),
                        deltas,
                        Flux.defer(() -> {
                            StructuredTutorReply parsed = parseStructuredReply(raw.toString());
                            return Flux.just(AiTutorStreamEvent.done(Map.of(
                                    "source", SOURCE_LLM,
                                    "canProceed", Boolean.toString(parsed.canProceed()),
                                    "finalDraft", nullToEmpty(parsed.finalDraft()),
                                    "completionHint", nullToEmpty(parsed.completionHint()),
                                    "summary", nullToEmpty(parsed.summary())
                            )));
                        })
                )
                .onErrorResume(e -> {
                    log.warn("AiTutor chat stream LLM failed {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    String msg = e instanceof BusinessException be && be.getMessage() != null
                            ? be.getMessage()
                            : LLM_UNAVAILABLE_MESSAGE;
                    return Flux.just(AiTutorStreamEvent.error(msg));
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
                log.warn("AiTutor feedback LLM failed {}: {}", e.getClass().getSimpleName(), e.getMessage());
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

    private PromptBundle buildPrompt(AiTutorChatRequest request) {
        AiTutorChatRequest.Context ctx = request.getContext();
        String phase = ctx.getPhase() != null ? ctx.getPhase().trim() : "";
        String knowledgeKey = ctx.getKnowledge() != null ? ctx.getKnowledge().trim() : "";
        String knowledgeLabel = ctx.getKnowledgeLabel() != null ? ctx.getKnowledgeLabel().trim() : "";
        String forNorm = !knowledgeKey.isBlank() ? knowledgeKey : knowledgeLabel;
        String norm = TutorKnowledgeNormalizer.normalize(forNorm);
        String display = !knowledgeLabel.isBlank() ? knowledgeLabel
                : (!knowledgeKey.isBlank() ? knowledgeKey : "当前知识点");
        String system = TutorPromptTemplates.conversationSystemPrompt(phase, display, norm);
        String user = TutorPromptTemplates.conversationUserPrompt(phase, display, request.getMessages());
        return new PromptBundle(system, user);
    }

    private void requireLiveProvider() {
        if (!llmClient.isLiveProviderReady()) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, LLM_UNAVAILABLE_MESSAGE);
        }
    }

    private StructuredTutorReply parseStructuredReply(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("empty tutor reply");
        }
        String reply = extractTag(raw, REPLY_PATTERN);
        if (reply.isBlank()) {
            throw new IllegalStateException("missing reply block");
        }
        boolean canProceed = "true".equalsIgnoreCase(extractTag(raw, CAN_PROCEED_PATTERN));
        String completionHint = extractTag(raw, COMPLETION_HINT_PATTERN);
        String summary = extractTag(raw, SUMMARY_PATTERN);
        String finalDraft = extractTag(raw, FINAL_DRAFT_PATTERN);
        if (!canProceed) {
            finalDraft = "";
        }
        return new StructuredTutorReply(reply, canProceed, emptyToNull(finalDraft), emptyToNull(completionHint), emptyToNull(summary));
    }

    private static String extractTag(String raw, Pattern pattern) {
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) == null ? "" : matcher.group(1).trim();
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
            String comment = blankToText(p.getDiagnosis(), "我已看过你的回答。");
            String suggestion = blankToText(p.getSuggestion(), "试着用一句话抓住核心概念。");
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

    private static TaskFeedbackResponse malformedLlmFeedbackResponse() {
        return new TaskFeedbackResponse(
                false,
                "暂时无法解析导师反馈，请再试一次。",
                "可以换一句话描述你的理解，或补充一个你看到的关键点。",
                null,
                null,
                "试着先说出：这个概念让你联想到什么具体画面？",
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

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private record PromptBundle(String systemPrompt, String userPrompt) {
    }

    private record StructuredTutorReply(
            String reply,
            boolean canProceed,
            String finalDraft,
            String completionHint,
            String summary
    ) {
    }

    private static final class ReplyDeltaExtractor {
        private static final String OPEN = "<reply>";
        private static final String CLOSE = "</reply>";

        private final StringBuilder pending = new StringBuilder();
        private boolean insideReply = false;
        private boolean replyClosed = false;

        List<String> consume(String chunk) {
            pending.append(chunk);
            List<String> pieces = new ArrayList<>();

            while (!replyClosed) {
                if (!insideReply) {
                    int openIndex = pending.indexOf(OPEN);
                    if (openIndex < 0) {
                        trimPrefixForPartialTag(OPEN.length() - 1);
                        break;
                    }
                    pending.delete(0, openIndex + OPEN.length());
                    insideReply = true;
                }

                int closeIndex = pending.indexOf(CLOSE);
                if (closeIndex < 0) {
                    int safeLen = Math.max(0, pending.length() - (CLOSE.length() - 1));
                    if (safeLen == 0) {
                        break;
                    }
                    pieces.add(pending.substring(0, safeLen));
                    pending.delete(0, safeLen);
                    break;
                }

                if (closeIndex > 0) {
                    pieces.add(pending.substring(0, closeIndex));
                }
                pending.delete(0, closeIndex + CLOSE.length());
                insideReply = false;
                replyClosed = true;
            }

            return pieces;
        }

        private void trimPrefixForPartialTag(int keepTail) {
            if (pending.length() <= keepTail) {
                return;
            }
            pending.delete(0, pending.length() - keepTail);
        }
    }
}
