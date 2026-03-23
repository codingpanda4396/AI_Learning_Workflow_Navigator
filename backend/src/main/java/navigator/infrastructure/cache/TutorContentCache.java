package navigator.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * R0003：导师 prompt / explain 本地缓存（Caffeine，10 分钟过期）。
 */
@Component
public class TutorContentCache {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Cache<String, String> explainCache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> promptCache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(10_000)
            .build();

    public static String explainKey(String step, String knowledgeNormalized) {
        return "explain:" + nullToEmpty(step) + ":" + nullToEmpty(knowledgeNormalized);
    }

    public static String promptKey(String step, String knowledgeNormalized) {
        return "prompt:" + nullToEmpty(step) + ":" + nullToEmpty(knowledgeNormalized);
    }

    public Optional<String> getExplain(String key) {
        return Optional.ofNullable(explainCache.getIfPresent(key));
    }

    public void putExplain(String key, String text) {
        if (key != null && text != null && !text.isBlank()) {
            explainCache.put(key, text.trim());
        }
    }

    public Optional<String> getPrompt(String key) {
        return Optional.ofNullable(promptCache.getIfPresent(key));
    }

    public void putPrompt(String key, String text) {
        if (key != null && text != null && !text.isBlank()) {
            promptCache.put(key, text.trim());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
