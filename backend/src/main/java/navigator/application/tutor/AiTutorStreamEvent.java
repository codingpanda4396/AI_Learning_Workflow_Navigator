package navigator.application.tutor;

import java.util.Map;

public record AiTutorStreamEvent(String event, Map<String, String> data) {

    public static AiTutorStreamEvent meta(String source) {
        return new AiTutorStreamEvent("meta", Map.of("source", source != null ? source : ""));
    }

    public static AiTutorStreamEvent delta(String text) {
        return new AiTutorStreamEvent("delta", Map.of("text", text != null ? text : ""));
    }

    public static AiTutorStreamEvent done() {
        return new AiTutorStreamEvent("done", Map.of());
    }

    public static AiTutorStreamEvent error(String message) {
        return new AiTutorStreamEvent("error", Map.of("message", message != null ? message : ""));
    }
}
