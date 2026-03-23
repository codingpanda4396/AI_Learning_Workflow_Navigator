package navigator.application.tutor;

import navigator.application.tutor.prompt.TutorPromptTemplates;
import navigator.infrastructure.cache.TutorContentCache;
import navigator.infrastructure.config.AsyncConfig;
import navigator.infrastructure.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Explain 预生成：强制调度 + 进行中的 {@link CompletableFuture}。
 * 缓存在 future 完成后的 {@code whenComplete} 中写入，使同步读路径有机会在「刚生成完」时看到 {@code LLM} 再落为 {@code CACHE}。
 */
@Component
public class TutorExplainPrefetchCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TutorExplainPrefetchCoordinator.class);

    private final LlmClient llmClient;
    private final TutorContentCache tutorContentCache;
    private final Executor aiTutorExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<String>> inflight = new ConcurrentHashMap<>();

    public TutorExplainPrefetchCoordinator(LlmClient llmClient,
                                           TutorContentCache tutorContentCache,
                                           @Qualifier(AsyncConfig.AI_TUTOR_EXECUTOR) Executor aiTutorExecutor) {
        this.llmClient = llmClient;
        this.tutorContentCache = tutorContentCache;
        this.aiTutorExecutor = aiTutorExecutor;
    }

    /**
     * 缓存未命中时<strong>必须</strong>调用：在 LLM 可用时至少触发一次生成（与前端 prefetch 互补）。
     */
    public void ensurePrefetch(String explainKey, String knowledgePointDisplay) {
        if (!llmClient.isLiveProviderReady()) {
            return;
        }
        if (tutorContentCache.getExplain(explainKey).isPresent()) {
            return;
        }
        inflight.computeIfAbsent(explainKey, k -> {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(
                    () -> generateLlmTextOnly(k, knowledgePointDisplay),
                    aiTutorExecutor);
            // 延后写缓存与移除 inflight，给其它线程留出「future 已完成但尚未落缓存」窗口，便于返回 source=LLM
            cf.whenComplete((text, ex) -> aiTutorExecutor.execute(() -> {
                try {
                    if (text != null && !text.isBlank()) {
                        tutorContentCache.putExplain(k, text.trim());
                    }
                } finally {
                    inflight.remove(k, cf);
                }
            }));
            return cf;
        });
    }

    /**
     * Future 已完成且成功时返回正文（此时缓存可能尚未写入，见 {@code whenComplete} 顺序）。
     */
    public Optional<String> pollCompletedLlmExplain(String explainKey) {
        CompletableFuture<String> f = inflight.get(explainKey);
        if (f == null || !f.isDone() || f.isCompletedExceptionally()) {
            return Optional.empty();
        }
        try {
            String s = f.join();
            return (s != null && !s.isBlank()) ? Optional.of(s.trim()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 仅调 LLM / 短路已存在缓存，不在此方法内写 explain 缓存（由 whenComplete 写入）。 */
    private String generateLlmTextOnly(String key, String knowledgePointDisplay) {
        try {
            if (tutorContentCache.getExplain(key).isPresent()) {
                return tutorContentCache.getExplain(key).orElse(null);
            }
            if (!llmClient.isLiveProviderReady()) {
                return null;
            }
            String raw = llmClient.chat(
                    TutorPromptTemplates.explainSystemPrompt(),
                    TutorPromptTemplates.explainUserPrompt(knowledgePointDisplay, ""));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return raw.trim();
        } catch (Exception e) {
            log.warn("explain LLM failed key={} — {}: {}", key,
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
