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
        return parse(rawText, "unknown", "unknown", "unknown", "unknown");
    }

    public JsonNode parse(String rawText, LlmCallContext context) {
        return parse(
            rawText,
            context == null || context.stage() == null ? "unknown" : context.stage().name(),
            context == null ? "unknown" : context.model(),
            context == null ? "unknown" : context.traceId(),
            context == null ? "unknown" : context.requestId()
        );
    }

    public JsonNode parse(String rawText, String stage, String model, String traceId, String requestId) {
        ParseDiagnostics diagnostics = ParseDiagnostics.from(rawText, stage, model, traceId, requestId);
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

        if (diagnostics.looksTruncated()) {
            RepairResult truncatedRepair = repairTruncated(rawText, repaired.repairedText(), extracted);
            diagnostics = diagnostics.withTruncatedRepair(truncatedRepair);
            if (truncatedRepair.repairedText() != null && !truncatedRepair.repairedText().isBlank()) {
                try {
                    attempts.add("truncated_repair");
                    return objectMapper.readTree(truncatedRepair.repairedText());
                } catch (Exception ex) {
                    lastError = ex;
                }
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

    private RepairResult repairTruncated(String rawText, String repairedText, JsonSlice extracted) {
        String working = repairedText == null || repairedText.isBlank()
            ? (extracted != null ? extracted.content() : rawText)
            : repairedText;
        working = working == null ? "" : working.trim();
        List<String> steps = new ArrayList<>();
        if (working.isBlank()) {
            return new RepairResult(null, List.of("empty_working_text"));
        }
        if (!working.startsWith("{")) {
            return new RepairResult(working, List.of("skip_non_object"));
        }

        String withoutUnclosedString = removeTrailingUnclosedString(working);
        if (!withoutUnclosedString.equals(working)) {
            steps.add("remove_unclosed_string_tail");
            working = withoutUnclosedString;
        }

        String trimmedDangling = trimDanglingTail(working);
        if (!trimmedDangling.equals(working)) {
            steps.add("trim_dangling_tail");
            working = trimmedDangling;
        }

        String closed = closeOpenContainers(working);
        if (tryParse(closed)) {
            steps.add("close_open_containers");
            return new RepairResult(closed, steps);
        }

        int cursor = working.length();
        while (true) {
            int commaIndex = lastCommaOutsideString(working, cursor - 1);
            if (commaIndex < 0) {
                break;
            }
            String candidate = trimDanglingTail(working.substring(0, commaIndex));
            String candidateClosed = closeOpenContainers(candidate);
            if (tryParse(candidateClosed)) {
                steps.add("drop_incomplete_tail_element");
                steps.add("close_open_containers");
                return new RepairResult(candidateClosed, steps);
            }
            cursor = commaIndex;
        }

        return new RepairResult(closeOpenContainers(working), steps);
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

    private String removeTrailingUnclosedString(String text) {
        boolean inString = false;
        boolean escaped = false;
        int lastStringStart = -1;
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
                if (!inString) {
                    lastStringStart = i;
                }
                inString = !inString;
            }
        }
        if (!inString || lastStringStart < 0) {
            return text;
        }
        return text.substring(0, lastStringStart);
    }

    private String trimDanglingTail(String text) {
        String working = text == null ? "" : text.trim();
        while (!working.isBlank()) {
            char last = working.charAt(working.length() - 1);
            if (last == ',' || last == ':' || last == '{' || last == '[') {
                working = working.substring(0, working.length() - 1).trim();
                continue;
            }
            break;
        }
        return working;
    }

    private String closeOpenContainers(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        StringBuilder closers = new StringBuilder();
        List<Character> stack = new ArrayList<>();
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
            if (c == '{' || c == '[') {
                stack.add(c);
            } else if (c == '}' && !stack.isEmpty() && stack.get(stack.size() - 1) == '{') {
                stack.remove(stack.size() - 1);
            } else if (c == ']' && !stack.isEmpty() && stack.get(stack.size() - 1) == '[') {
                stack.remove(stack.size() - 1);
            }
        }
        for (int i = stack.size() - 1; i >= 0; i--) {
            closers.append(stack.get(i) == '{' ? '}' : ']');
        }
        return text + closers;
    }

    private int lastCommaOutsideString(String text, int from) {
        int safeFrom = Math.min(from, text.length() - 1);
        boolean inString = false;
        boolean escaped = false;
        int lastComma = -1;
        for (int i = 0; i <= safeFrom; i++) {
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
            if (!inString && c == ',') {
                lastComma = i;
            }
        }
        return lastComma;
    }

    private boolean tryParse(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
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
        boolean looksTruncated,
        JsonSlice slice,
        RepairResult repair,
        RepairResult truncatedRepair
    ) {

        private static ParseDiagnostics from(String rawText, String stage, String model, String traceId, String requestId) {
            String safeText = rawText == null ? "" : rawText;
            boolean hasFence = safeText.contains("```");
            int firstBrace = safeText.indexOf('{');
            int lastBrace = safeText.lastIndexOf('}');
            boolean hasStart = firstBrace >= 0;
            boolean hasEnd = lastBrace >= 0 && lastBrace > firstBrace;
            boolean hasExtraText = hasStart && hasEnd && (firstBrace > 0 || lastBrace < safeText.length() - 1);
            boolean truncated = hasStart && !hasEnd;
            return new ParseDiagnostics(
                safeValue(stage),
                safeValue(model),
                safeValue(traceId),
                safeValue(requestId),
                safeText.length(),
                sanitize(safeText.length() <= RAW_PREVIEW_LIMIT ? safeText : safeText.substring(0, RAW_PREVIEW_LIMIT)),
                hasFence,
                hasStart,
                hasEnd,
                hasExtraText,
                truncated,
                null,
                new RepairResult(null, List.of()),
                new RepairResult(null, List.of())
            );
        }

        private ParseDiagnostics withSlice(JsonSlice nextSlice) {
            return new ParseDiagnostics(stage, model, traceId, requestId, rawLength, rawPreview, hasCodeFence,
                hasJsonObjectStart, hasJsonObjectEnd || (nextSlice != null && nextSlice.endExclusive() > 0),
                hasLeadingOrTrailingText || (nextSlice != null && (nextSlice.start() > 0 || nextSlice.endExclusive() > 0 && nextSlice.endExclusive() < rawLength)),
                looksTruncated || (nextSlice != null && nextSlice.endExclusive() < 0),
                nextSlice, repair, truncatedRepair);
        }

        private ParseDiagnostics withRepair(RepairResult nextRepair) {
            return new ParseDiagnostics(stage, model, traceId, requestId, rawLength, rawPreview, hasCodeFence,
                hasJsonObjectStart, hasJsonObjectEnd, hasLeadingOrTrailingText, looksTruncated, slice, nextRepair, truncatedRepair);
        }

        private ParseDiagnostics withTruncatedRepair(RepairResult nextRepair) {
            return new ParseDiagnostics(stage, model, traceId, requestId, rawLength, rawPreview, hasCodeFence,
                hasJsonObjectStart, hasJsonObjectEnd, hasLeadingOrTrailingText, looksTruncated, slice, repair, nextRepair);
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
                + " looksTruncated=" + looksTruncated
                + " extractedRange=" + extractedRange()
                + " hasExtraText=" + hasLeadingOrTrailingText
                + " repairSteps=" + repair.steps()
                + " truncatedRepairSteps=" + truncatedRepair.steps()
                + " attempts=" + attempts
                + " errorType=" + safeValue(errorType)
                + " error=" + safeValue(errorMessage);
        }

        private LlmFallbackReason resolveFallbackReason() {
            if (rawLength == 0) {
                return LlmFallbackReason.JSON_EMPTY_RESPONSE;
            }
            if (looksTruncated) {
                return LlmFallbackReason.OUTPUT_TRUNCATED;
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
