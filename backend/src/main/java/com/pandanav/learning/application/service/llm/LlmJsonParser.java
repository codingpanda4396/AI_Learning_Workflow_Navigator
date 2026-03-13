package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmJsonParser {

    private static final Logger log = LoggerFactory.getLogger(LlmJsonParser.class);
    private static final int RAW_PREVIEW_LIMIT = 800;

    private final ObjectMapper objectMapper;

    public LlmJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(String rawText) {
        return parse(rawText, null);
    }

    public JsonNode parse(String rawText, LlmCallContext context) {
        ParseDiagnostics diagnostics = ParseDiagnostics.from(rawText, context);
        if (rawText == null || rawText.isBlank()) {
            String summary = diagnostics.describe(List.of("empty_response"), "EMPTY_RESPONSE", "empty_response");
            log.warn("LLM_JSON_PARSE_FAILURE {}", summary);
            throw new LlmJsonParseException("LLM returned empty content.", LlmFallbackReason.JSON_EMPTY_RESPONSE, summary);
        }

        List<String> attempts = new ArrayList<>();
        Exception lastError = null;

        try {
            attempts.add("strict_raw");
            return objectMapper.readTree(rawText);
        } catch (Exception ex) {
            lastError = ex;
        }

        JsonSlice extracted = extractFirstJsonObject(rawText);
        diagnostics = diagnostics.withSlice(extracted);
        if (extracted != null) {
            try {
                attempts.add("extracted_object");
                return objectMapper.readTree(extracted.content());
            } catch (Exception ex) {
                lastError = ex;
            }
        }

        RepairResult repaired = repair(rawText, extracted);
        diagnostics = diagnostics.withRepair(repaired);
        if (repaired.repairedText() != null && !repaired.repairedText().isBlank()) {
            try {
                attempts.add("repaired_text");
                return objectMapper.readTree(repaired.repairedText());
            } catch (Exception ex) {
                lastError = ex;
            }
        }

        String errorType = lastError == null ? "unknown" : lastError.getClass().getSimpleName();
        String errorMessage = lastError == null ? "n/a" : sanitize(lastError.getMessage());
        String summary = diagnostics.describe(attempts, errorType, errorMessage);
        LlmFallbackReason reason = diagnostics.resolveFallbackReason();
        log.warn("LLM_JSON_PARSE_FAILURE {}", summary);
        throw new LlmJsonParseException("Failed to parse LLM JSON output. " + summary, reason, summary);
    }

    private RepairResult repair(String rawText, JsonSlice extracted) {
        String working = rawText == null ? "" : rawText.trim();
        List<String> steps = new ArrayList<>();

        if (working.startsWith("```json")) {
            working = working.substring(7).trim();
            steps.add("remove_json_fence");
        } else if (working.startsWith("```")) {
            working = working.substring(3).trim();
            steps.add("remove_open_fence");
        }
        if (working.endsWith("```")) {
            working = working.substring(0, working.length() - 3).trim();
            steps.add("remove_close_fence");
        }

        if (extracted != null && !working.equals(extracted.content())) {
            working = extracted.content().trim();
            steps.add("trim_to_first_object");
        }

        int balance = braceBalance(working);
        if (balance > 0 && working.startsWith("{")) {
            working = working + "}".repeat(balance);
            steps.add("append_missing_closing_braces:" + balance);
        }

        String withoutTrailingComma = working.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        if (!withoutTrailingComma.equals(working)) {
            working = withoutTrailingComma;
            steps.add("remove_trailing_commas");
        }

        return new RepairResult(working, steps);
    }

    private int braceBalance(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int balance = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                balance++;
            } else if (c == '}' && balance > 0) {
                balance--;
            }
        }
        return balance;
    }

    private JsonSlice extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int start = -1;
        int level = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                if (start < 0) {
                    start = i;
                }
                level++;
            } else if (c == '}' && level > 0) {
                level--;
                if (level == 0 && start >= 0) {
                    return new JsonSlice(start, i + 1, text.substring(start, i + 1));
                }
            }
        }
        return start >= 0 ? new JsonSlice(start, -1, text.substring(start)) : null;
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }

    private record JsonSlice(int start, int endExclusive, String content) {
    }

    private record RepairResult(String repairedText, List<String> steps) {
    }

    private record ParseDiagnostics(
        String stage,
        String model,
        String traceId,
        String requestId,
        int rawLength,
        String rawPreview,
        boolean hasCodeFence,
        boolean hasJsonObjectStart,
        boolean hasJsonObjectEnd,
        boolean hasLeadingOrTrailingText,
        JsonSlice slice,
        RepairResult repair
    ) {

        private static ParseDiagnostics from(String rawText, LlmCallContext context) {
            String safeText = rawText == null ? "" : rawText;
            boolean hasFence = safeText.contains("```");
            int firstBrace = safeText.indexOf('{');
            int lastBrace = safeText.lastIndexOf('}');
            boolean hasStart = firstBrace >= 0;
            boolean hasEnd = lastBrace >= 0 && lastBrace > firstBrace;
            boolean hasExtraText = hasStart && hasEnd && (firstBrace > 0 || lastBrace < safeText.length() - 1);
            return new ParseDiagnostics(
                context == null ? "unknown" : context.stage().name(),
                context == null ? "unknown" : context.model(),
                context == null ? "unknown" : context.traceId(),
                context == null ? "unknown" : context.requestId(),
                safeText.length(),
                sanitize(safeText.length() <= RAW_PREVIEW_LIMIT ? safeText : safeText.substring(0, RAW_PREVIEW_LIMIT)),
                hasFence,
                hasStart,
                hasEnd,
                hasExtraText,
                null,
                new RepairResult(null, List.of())
            );
        }

        private ParseDiagnostics withSlice(JsonSlice nextSlice) {
            return new ParseDiagnostics(stage, model, traceId, requestId, rawLength, rawPreview, hasCodeFence,
                hasJsonObjectStart, hasJsonObjectEnd || (nextSlice != null && nextSlice.endExclusive() > 0),
                hasLeadingOrTrailingText || (nextSlice != null && (nextSlice.start() > 0 || nextSlice.endExclusive() > 0 && nextSlice.endExclusive() < rawLength)),
                nextSlice, repair);
        }

        private ParseDiagnostics withRepair(RepairResult nextRepair) {
            return new ParseDiagnostics(stage, model, traceId, requestId, rawLength, rawPreview, hasCodeFence,
                hasJsonObjectStart, hasJsonObjectEnd, hasLeadingOrTrailingText, slice, nextRepair);
        }

        private String describe(List<String> attempts, String errorType, String errorMessage) {
            return "stage=" + safeValue(stage)
                + " model=" + safeValue(model)
                + " traceId=" + safeValue(traceId)
                + " requestId=" + safeValue(requestId)
                + " rawLength=" + rawLength
                + " rawPreview=\"" + rawPreview + "\""
                + " hasCodeFence=" + hasCodeFence
                + " jsonObjectStartDetected=" + hasJsonObjectStart
                + " jsonObjectEndDetected=" + hasJsonObjectEnd
                + " extractedRange=" + extractedRange()
                + " hasExtraText=" + hasLeadingOrTrailingText
                + " repairSteps=" + repair.steps()
                + " attempts=" + attempts
                + " errorType=" + safeValue(errorType)
                + " error=" + safeValue(errorMessage);
        }

        private LlmFallbackReason resolveFallbackReason() {
            if (rawLength == 0) {
                return LlmFallbackReason.JSON_EMPTY_RESPONSE;
            }
            if (hasCodeFence || hasLeadingOrTrailingText) {
                return LlmFallbackReason.JSON_EXTRA_TEXT;
            }
            return LlmFallbackReason.JSON_PARSE_ERROR;
        }

        private String extractedRange() {
            if (slice == null) {
                return "none";
            }
            return slice.start() + ":" + slice.endExclusive();
        }

        private static String safeValue(String value) {
            return value == null || value.isBlank() ? "unknown" : value;
        }
    }
}
