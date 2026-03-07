package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.FeedbackResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EvaluatorService {

    private static final int MIN_REASONING_LENGTH = 40;

    public EvaluationResult evaluate(String conceptName, String objective, String userAnswer) {
        String answer = normalize(userAnswer);
        String context = normalize(conceptName + " " + objective);

        List<String> keyTerms = buildKeyTerms(context);
        List<String> missingTerms = keyTerms.stream()
            .filter(term -> !contains(answer, term))
            .toList();

        int score = 100;
        Set<String> errorTags = new LinkedHashSet<>();
        List<String> fixes = new ArrayList<>();

        if (!missingTerms.isEmpty()) {
            score -= Math.min(48, missingTerms.size() * 12);
            errorTags.add("MISSING_STEPS");
            fixes.add("Add missing key points: " + String.join(", ", missingTerms) + ".");
        }

        if (answer.length() < MIN_REASONING_LENGTH) {
            score -= 15;
            errorTags.add("SHALLOW_REASONING");
            fixes.add("Expand reasoning with cause-effect chain and concrete steps.");
        }

        if (containsConfusion(answer)) {
            score -= 20;
            errorTags.add("CONCEPT_CONFUSION");
            fixes.add("Correct the misconception and explain why the correct mechanism is required.");
        }

        if (containsTerminologyIssue(answer)) {
            score -= 10;
            errorTags.add("TERMINOLOGY");
            fixes.add("Use precise protocol terminology (SYN/ACK/sequence number semantics).");
        }

        if (keyTerms.stream().noneMatch(term -> contains(answer, term))) {
            score -= 12;
            errorTags.add("MEMORY_GAP");
            fixes.add("Recall and restate the core concept definition before explaining.");
        }

        if (isTcpHandshakeContext(context)
            && !contains(answer, "old syn")
            && !contains(answer, "replay")
            && !contains(answer, "\u65e7 syn")
            && !contains(answer, "\u91cd\u653e")
            && !contains(answer, "half-open")
            && !contains(answer, "\u534a\u5f00")) {
            score -= 8;
            errorTags.add("BOUNDARY_CASE");
            fixes.add("Include boundary case discussion: old SYN replay and half-open connection risk.");
        }

        score = clamp(score, 0, 100);

        String diagnosis;
        if (errorTags.isEmpty()) {
            diagnosis = "Answer is accurate and complete for current objective.";
        } else {
            diagnosis = "Answer has gaps in " + String.join(", ", errorTags) + ".";
        }

        return new EvaluationResult(
            score,
            List.copyOf(errorTags),
            new FeedbackResponse(diagnosis, List.copyOf(fixes))
        );
    }

    private List<String> buildKeyTerms(String context) {
        if (isTcpHandshakeContext(context)) {
            return List.of("\u4e09\u6b21\u63e1\u624b", "syn", "ack", "\u5e8f\u5217\u53f7");
        }
        return List.of("definition", "mechanism", "steps");
    }

    private boolean isTcpHandshakeContext(String context) {
        return contains(context, "\u4e09\u6b21\u63e1\u624b")
            || contains(context, "tcp")
            || contains(context, "handshake");
    }

    private boolean containsConfusion(String answer) {
        return contains(answer, "\u4e24\u6b21\u63e1\u624b\u5c31\u591f")
            || contains(answer, "two-way handshake is enough")
            || contains(answer, "no need ack")
            || contains(answer, "\u4e0d\u9700\u8981 ack");
    }

    private boolean containsTerminologyIssue(String answer) {
        return contains(answer, "\u56db\u6b21\u63e1\u624b\u5efa\u7acb\u8fde\u63a5")
            || contains(answer, "ack means connect success");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean contains(String text, String token) {
        return text.contains(token.toLowerCase(Locale.ROOT));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record EvaluationResult(
        Integer score,
        List<String> errorTags,
        FeedbackResponse feedback
    ) {
    }
}
