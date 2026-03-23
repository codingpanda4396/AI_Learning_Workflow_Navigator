package navigator.application.tutor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.api.dto.AiTutorChatRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.tutor.AiTutorChatResult;
import navigator.application.tutor.AiTutorChatStreamHandler;
import navigator.application.tutor.AiTutorFeedbackPayload;
import navigator.application.tutor.AiTutorFeedbackResult;
import navigator.application.tutor.AiTutorService;
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

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private static final Logger log = LoggerFactory.getLogger(AiTutorServiceImpl.class);

    public static final String SOURCE_CACHE = "CACHE";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_LLM = "LLM";

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
                : (!knowledgeKey.isBlank() ? knowledgeKey : "当前知识点");
        String system = TutorPromptTemplates.embeddedChatSystemPrompt(stepNum, phase, display, norm);
        if (llmClient.isLiveProviderReady()) {
            try {
                String raw = llmClient.chat(system, userMsg);
                if (raw != null && !raw.isBlank()) {
                    return new AiTutorChatResult(SOURCE_LLM, raw.trim());
                }
            } catch (Exception e) {
                log.warn("AiTutor chat LLM failed — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        String fallback = tutorFallbackRegistry.embeddedChatFallback(norm, userMsg);
        return new AiTutorChatResult(SOURCE_FALLBACK, fallback);
    }

    @Override
    public void streamChat(AiTutorChatRequest request, AiTutorChatStreamHandler handler) {
        AiTutorChatRequest.Context ctx = request.getContext();
        String userMsg = request.getMessage() != null ? request.getMessage().trim() : "";
        if (userMsg.isEmpty()) {
            handler.onMeta(SOURCE_FALLBACK);
            handler.onDelta(tutorFallbackRegistry.embeddedChatFallback(TutorKnowledgeNormalizer.UNKNOWN, ""));
            return;
        }
        int stepNum = ctx.getStep() != null ? ctx.getStep() : 1;
        String phase = ctx.getPhase() != null ? ctx.getPhase().trim() : "";
        String knowledgeKey = ctx.getKnowledge() != null ? ctx.getKnowledge().trim() : "";
        String knowledgeLabel = ctx.getKnowledgeLabel() != null ? ctx.getKnowledgeLabel().trim() : "";
        String forNorm = !knowledgeKey.isBlank() ? knowledgeKey : knowledgeLabel;
        String norm = TutorKnowledgeNormalizer.normalize(forNorm);
        String display = !knowledgeLabel.isBlank() ? knowledgeLabel
                : (!knowledgeKey.isBlank() ? knowledgeKey : "当前知识点");
        String system = TutorPromptTemplates.embeddedChatSystemPrompt(stepNum, phase, display, norm);
        if (!llmClient.isLiveProviderReady()) {
            handler.onMeta(SOURCE_FALLBACK);
            handler.onDelta(tutorFallbackRegistry.embeddedChatFallback(norm, userMsg));
            return;
        }
        AtomicBoolean metaSent = new AtomicBoolean(false);
        AtomicBoolean sawNonEmpty = new AtomicBoolean(false);
        try {
            llmClient.chatStream(system, userMsg, chunk -> {
                if (chunk == null || chunk.isEmpty()) {
                    return;
                }
                sawNonEmpty.set(true);
                if (!metaSent.getAndSet(true)) {
                    handler.onMeta(SOURCE_LLM);
                }
                handler.onDelta(chunk);
            });
            if (!sawNonEmpty.get()) {
                handler.onMeta(SOURCE_FALLBACK);
                handler.onDelta(tutorFallbackRegistry.embeddedChatFallback(norm, userMsg));
            }
        } catch (Exception e) {
            log.warn("AiTutor chat stream LLM failed — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            if (!metaSent.get()) {
                handler.onMeta(SOURCE_FALLBACK);
                handler.onDelta(tutorFallbackRegistry.embeddedChatFallback(norm, userMsg));
            } else {
                handler.onDelta("\n\n（连接中断，以下为本地提示）\n"
                        + tutorFallbackRegistry.embeddedChatFallback(norm, userMsg));
            }
        }
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
        // 先于缓存：生成线程已 put 缓存但 whenComplete 尚未移除 future 时，可返回 LLM 以增强「AI 生成感」
        var justDone = explainPrefetchCoordinator.pollCompletedLlmExplain(key);
        if (justDone.isPresent()) {
            return new AiTutorTextResult(SOURCE_LLM, justDone.get());
        }
        var hit = tutorContentCache.getExplain(key);
        if (hit.isPresent()) {
            return new AiTutorTextResult(SOURCE_CACHE, hit.get());
        }
        // 与前端 prefetch 互补：同步路径在 miss 时强制再调度一次，避免用户快于预取时永远不触发生成
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
                log.warn("AiTutor feedback LLM failed — {}: {}", e.getClass().getSimpleName(), e.getMessage());
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

    /**
     * 供单测调用：将 LLM 原始 JSON 转为 {@link TaskFeedbackResponse}；不可信时返回兜底（含「无法解析」提示）。
     */
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

    /**
     * 从任意前缀噪声中提取首个 JSON 对象子串，再交给 Jackson（反馈解析关键路径）。
     */
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
