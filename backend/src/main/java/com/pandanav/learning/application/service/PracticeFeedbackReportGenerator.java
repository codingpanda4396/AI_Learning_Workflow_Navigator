package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.enums.PracticeFeedbackAction;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeFeedbackReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(PracticeFeedbackReportGenerator.class);
    private static final String PROMPT_VERSION = "practice-feedback-v1";
    private static final String EXPECTED_JSON = """
        {"diagnosis_summary":"","strengths":[],"weaknesses":[],"review_focus":[],"next_round_advice":"","recommended_action":"REVIEW"}
        """;

    private final LlmGateway llmGateway;
    private final LlmProperties llmProperties;
    private final LlmJsonParser llmJsonParser;
    private final PromptOutputValidator promptOutputValidator;
    private final ObjectMapper objectMapper;

    public PracticeFeedbackReportGenerator(
        LlmGateway llmGateway,
        LlmProperties llmProperties,
        LlmJsonParser llmJsonParser,
        PromptOutputValidator promptOutputValidator,
        ObjectMapper objectMapper
    ) {
        this.llmGateway = llmGateway;
        this.llmProperties = llmProperties;
        this.llmJsonParser = llmJsonParser;
        this.promptOutputValidator = promptOutputValidator;
        this.objectMapper = objectMapper;
    }

    public PracticeFeedbackReport generate(PracticeQuiz quiz, List<PracticeItem> items, List<PracticeSubmission> submissions) {
        if (llmProperties.isReady() && llmProperties.isEnabled()) {
            try {
                return generateByLlm(quiz, items, submissions);
            } catch (Exception ex) {
                if (!llmProperties.isFallbackToRule()) {
                    throw ex;
                }
                log.warn("practice feedback fell back to rule mode: quizId={}, reason={}", quiz.getId(), ex.getMessage());
            }
        }
        return generateByRule(quiz, items, submissions);
    }

    private PracticeFeedbackReport generateByLlm(PracticeQuiz quiz, List<PracticeItem> items, List<PracticeSubmission> submissions) {
        LlmPrompt prompt = new LlmPrompt(
            PromptTemplateKey.PRACTICE_GENERATION_V1,
            "PRACTICE_FEEDBACK",
            PROMPT_VERSION,
            LlmInvocationProfile.LIGHT_JSON_TASK,
            "You are a structured learning feedback generator. Return one JSON object only.",
            buildUserPrompt(quiz, items, submissions),
            EXPECTED_JSON,
            "short_json_only",
            null,
            null
        );
        LlmTextResult result = llmGateway.generate(prompt);
        JsonNode parsed = llmJsonParser.parse(result.text());
        List<String> errors = promptOutputValidator.validatePracticeFeedback(parsed);
        if (!errors.isEmpty()) {
            throw new InternalServerException("Invalid practice feedback output: " + String.join("; ", errors));
        }
        return toReport(quiz, parsed, "LLM", PROMPT_VERSION);
    }

    private String buildUserPrompt(PracticeQuiz quiz, List<PracticeItem> items, List<PracticeSubmission> submissions) {
        List<Map<String, Object>> qa = new ArrayList<>();
        for (PracticeItem item : items) {
            PracticeSubmission submission = submissions.stream()
                .filter(candidate -> candidate.getPracticeItemId().equals(item.getId()))
                .findFirst()
                .orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question_type", item.getQuestionType().name());
            row.put("stem", item.getStem());
            row.put("reference_answer", item.getStandardAnswer());
            row.put("user_answer", submission == null ? "" : submission.getUserAnswer());
            row.put("score", submission == null ? null : submission.getScore());
            row.put("is_correct", submission == null ? null : submission.getCorrect());
            row.put("feedback", submission == null ? "" : submission.getFeedback());
            row.put("error_tags", submission == null ? List.of() : readStringArray(submission.getErrorTagsJson()));
            qa.add(row);
        }
        return """
            Build one compact JSON feedback report.
            Constraints:
            - diagnosis_summary: 20-200 chars
            - strengths: 1-3 items
            - weaknesses: 1-3 items
            - review_focus: 1-3 items
            - next_round_advice: 10-120 chars
            - recommended_action: REVIEW or NEXT_ROUND

            quiz_id=%s
            answers=%s
            JSON:
            %s
            """.formatted(quiz.getId(), toJson(qa), EXPECTED_JSON);
    }

    private PracticeFeedbackReport generateByRule(PracticeQuiz quiz, List<PracticeItem> items, List<PracticeSubmission> submissions) {
        int totalScore = submissions.stream().map(PracticeSubmission::getScore).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        int average = submissions.isEmpty() ? 0 : Math.round((float) totalScore / submissions.size());
        List<String> weaknesses = submissions.stream()
            .filter(item -> item.getCorrect() == null || !item.getCorrect())
            .map(PracticeSubmission::getFeedback)
            .filter(text -> text != null && !text.isBlank())
            .limit(3)
            .toList();
        List<String> reviewFocus = submissions.stream()
            .flatMap(item -> readStringArray(item.getErrorTagsJson()).stream())
            .distinct()
            .limit(3)
            .toList();
        List<String> strengths = submissions.stream()
            .filter(item -> Boolean.TRUE.equals(item.getCorrect()))
            .map(item -> "Answered " + item.getPracticeItemId() + " correctly")
            .limit(3)
            .toList();

        PracticeFeedbackReport report = new PracticeFeedbackReport();
        report.setQuizId(quiz.getId());
        report.setSessionId(quiz.getSessionId());
        report.setTaskId(quiz.getTaskId());
        report.setUserId(quiz.getUserId());
        report.setDiagnosisSummary(
            average >= 80
                ? "Current quiz performance is stable. You can move to the next round with light review."
                : "Current quiz shows repeatable gaps. Review the weak points before advancing."
        );
        report.setStrengthsJson(toJson(strengths.isEmpty() ? List.of("Completed the full quiz flow") : strengths));
        report.setWeaknessesJson(toJson(weaknesses.isEmpty() ? List.of("Need one more round to confirm stability") : weaknesses));
        report.setReviewFocusJson(toJson(reviewFocus.isEmpty() ? List.of("CONSOLIDATE_CORE_CONCEPT") : reviewFocus));
        report.setNextRoundAdvice(
            average >= 80
                ? "Start the next round with one harder scenario question."
                : "Redo this node with focus on the tagged weak points first."
        );
        report.setRecommendedAction((average >= 80 ? PracticeFeedbackAction.NEXT_ROUND : PracticeFeedbackAction.REVIEW).name());
        report.setSource("RULE");
        report.setPromptVersion(PROMPT_VERSION);
        return report;
    }

    private PracticeFeedbackReport toReport(PracticeQuiz quiz, JsonNode parsed, String source, String promptVersion) {
        PracticeFeedbackReport report = new PracticeFeedbackReport();
        report.setQuizId(quiz.getId());
        report.setSessionId(quiz.getSessionId());
        report.setTaskId(quiz.getTaskId());
        report.setUserId(quiz.getUserId());
        report.setDiagnosisSummary(parsed.path("diagnosis_summary").asText());
        report.setStrengthsJson(toJson(readArray(parsed.path("strengths"))));
        report.setWeaknessesJson(toJson(readArray(parsed.path("weaknesses"))));
        report.setReviewFocusJson(toJson(readArray(parsed.path("review_focus"))));
        report.setNextRoundAdvice(parsed.path("next_round_advice").asText());
        report.setRecommendedAction(parsed.path("recommended_action").asText(PracticeFeedbackAction.REVIEW.name()));
        report.setSource(source);
        report.setPromptVersion(promptVersion);
        return report;
    }

    private List<String> readArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private List<String> readStringArray(String json) {
        try {
            return readArray(objectMapper.readTree(json == null || json.isBlank() ? "[]" : json));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize practice feedback payload.");
        }
    }
}
