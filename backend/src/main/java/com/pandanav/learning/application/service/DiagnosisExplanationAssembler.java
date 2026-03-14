package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisEvidenceSourceDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisReasoningStepDto;
import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DiagnosisExplanationAssembler {

    private static final String FALLBACK_QUESTION_ID = "unknown-question";
    private static final String FALLBACK_QUESTION_TITLE = "诊断问题";
    private static final String FALLBACK_SELECTED_ANSWER = "基于你的作答信息";
    private static final String FALLBACK_CONCLUSION = "系统基于已收集的回答，形成当前能力判断。";

    private final ObjectMapper objectMapper;

    public DiagnosisExplanationAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DiagnosisExplanation assemble(
        List<DiagnosisQuestion> questions,
        List<DiagnosisAnswer> answers,
        CapabilityProfileDraft draft,
        Map<DiagnosisDimension, List<String>> answerCodesByDimension
    ) {
        List<DiagnosisQuestion> safeQuestions = questions == null ? List.of() : questions;
        List<DiagnosisAnswer> safeAnswers = answers == null ? List.of() : answers;
        Map<DiagnosisDimension, List<String>> safeCodesByDimension =
            answerCodesByDimension == null ? Map.of() : answerCodesByDimension;
        CapabilityProfileDraft safeDraft = draft == null
            ? new CapabilityProfileDraft(CapabilityLevel.BEGINNER, List.of(), List.of(), "CONCEPT_FIRST", "STANDARD", "COURSE", "")
            : draft;

        Map<String, DiagnosisQuestion> questionById = new LinkedHashMap<>();
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension = new LinkedHashMap<>();
        for (DiagnosisQuestion question : safeQuestions) {
            if (question == null) {
                continue;
            }
            questionById.put(question.questionId(), question);
            questionByDimension.putIfAbsent(question.dimension(), question);
        }

        List<DiagnosisReasoningStepDto> reasoningSteps = buildReasoningSteps(
            safeAnswers,
            questionById,
            questionByDimension,
            safeDraft
        );
        if (reasoningSteps.isEmpty()) {
            reasoningSteps = List.of(new DiagnosisReasoningStepDto(
                DiagnosisDimension.FOUNDATION.name(),
                fallbackQuestionId(questionByDimension.get(DiagnosisDimension.FOUNDATION)),
                fallbackQuestionTitle(questionByDimension.get(DiagnosisDimension.FOUNDATION)),
                FALLBACK_SELECTED_ANSWER,
                FALLBACK_CONCLUSION
            ));
        }

        List<DiagnosisEvidenceSourceDto> strengthSources = buildEvidenceSources(
            safeDraft.strengths(),
            questionByDimension,
            safeCodesByDimension,
            true
        );
        List<DiagnosisEvidenceSourceDto> weaknessSources = buildEvidenceSources(
            safeDraft.weaknesses(),
            questionByDimension,
            safeCodesByDimension,
            false
        );

        return new DiagnosisExplanation(reasoningSteps, strengthSources, weaknessSources);
    }

    private List<DiagnosisReasoningStepDto> buildReasoningSteps(
        List<DiagnosisAnswer> answers,
        Map<String, DiagnosisQuestion> questionById,
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension,
        CapabilityProfileDraft draft
    ) {
        List<DiagnosisReasoningStepDto> steps = new ArrayList<>();
        for (DiagnosisAnswer answer : answers) {
            if (answer == null) {
                continue;
            }
            DiagnosisDimension dimension = answer.getDimension() == null ? DiagnosisDimension.FOUNDATION : answer.getDimension();
            DiagnosisQuestion question = questionById.get(answer.getQuestionId());
            if (question == null) {
                question = questionByDimension.get(dimension);
            }
            String questionId = question == null ? fallbackQuestionId(null) : fallbackText(question.questionId(), fallbackQuestionId(null));
            String questionTitle = question == null ? fallbackQuestionTitle(null) : fallbackText(question.title(), FALLBACK_QUESTION_TITLE);
            String selectedAnswer = resolveSelectedAnswerLabel(answer, question);
            String conclusion = inferConclusion(dimension, draft);
            steps.add(new DiagnosisReasoningStepDto(dimension.name(), questionId, questionTitle, selectedAnswer, conclusion));
        }
        return steps;
    }

    private List<DiagnosisEvidenceSourceDto> buildEvidenceSources(
        List<String> labels,
        Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension,
        Map<DiagnosisDimension, List<String>> answerCodesByDimension,
        boolean strength
    ) {
        List<String> safeLabels = labels == null ? List.of() : labels;
        List<DiagnosisEvidenceSourceDto> sources = new ArrayList<>();
        for (String label : safeLabels) {
            DiagnosisDimension dimension = inferSourceDimension(label, answerCodesByDimension, strength);
            DiagnosisQuestion question = questionByDimension.get(dimension);
            sources.add(new DiagnosisEvidenceSourceDto(
                fallbackText(label, strength ? "当前表现具备可持续推进条件" : "当前仍存在需要补强的环节"),
                dimension.name(),
                fallbackQuestionId(question)
            ));
        }

        if (sources.isEmpty()) {
            DiagnosisDimension fallbackDimension = firstAvailableDimension(questionByDimension);
            DiagnosisQuestion fallbackQuestion = questionByDimension.get(fallbackDimension);
            sources.add(new DiagnosisEvidenceSourceDto(
                strength ? "暂未识别到明显优势，将在学习过程中继续观察。" : "暂未识别到明确薄弱项，将在后续训练中继续定位。",
                fallbackDimension.name(),
                fallbackQuestionId(fallbackQuestion)
            ));
        }
        return sources;
    }

    private DiagnosisDimension inferSourceDimension(
        String label,
        Map<DiagnosisDimension, List<String>> answerCodesByDimension,
        boolean strength
    ) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (normalized.contains("时间")) {
            return DiagnosisDimension.TIME_BUDGET;
        }
        if (normalized.contains("基础")) {
            return DiagnosisDimension.FOUNDATION;
        }
        if (normalized.contains("目标")) {
            return DiagnosisDimension.GOAL_STYLE;
        }
        if (normalized.contains("实践") || normalized.contains("经验")) {
            return DiagnosisDimension.EXPERIENCE;
        }
        if (normalized.contains("偏好")) {
            return DiagnosisDimension.LEARNING_PREFERENCE;
        }
        if (strength && hasAnswerCode(answerCodesByDimension, DiagnosisDimension.EXPERIENCE, "PROJECTS", "EXAM_PREP")) {
            return DiagnosisDimension.EXPERIENCE;
        }
        if (!strength && hasAnswerCode(answerCodesByDimension, DiagnosisDimension.TIME_BUDGET, "LIGHT")) {
            return DiagnosisDimension.TIME_BUDGET;
        }
        return DiagnosisDimension.FOUNDATION;
    }

    private boolean hasAnswerCode(
        Map<DiagnosisDimension, List<String>> answerCodesByDimension,
        DiagnosisDimension dimension,
        String... candidates
    ) {
        List<String> values = answerCodesByDimension.getOrDefault(dimension, List.of());
        if (values.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (values.stream().anyMatch(candidate::equalsIgnoreCase)) {
                return true;
            }
        }
        return false;
    }

    private String inferConclusion(DiagnosisDimension dimension, CapabilityProfileDraft draft) {
        return switch (dimension) {
            case FOUNDATION -> {
                if (draft.currentLevel() == CapabilityLevel.ADVANCED) {
                    yield "你的基础较为扎实，可更快推进到综合应用。";
                }
                if (draft.currentLevel() == CapabilityLevel.INTERMEDIATE) {
                    yield "你已有一定基础，但仍需通过阶段训练巩固稳定性。";
                }
                yield "你对相关概念已有接触，但基础稳定性仍需要加强。";
            }
            case EXPERIENCE -> {
                if (containsAny(draft.strengths(), "实践", "目标感")) {
                    yield "你已有一定实践或目标驱动经验，适合采用分阶段推进。";
                }
                if (containsAny(draft.weaknesses(), "经验较少")) {
                    yield "当前经验储备相对不足，建议先从可控的小步练习开始。";
                }
                yield "你的经验信息将用于调整训练节奏与示例密度。";
            }
            case GOAL_STYLE -> "你的目标导向会影响路径侧重点与任务模板。";
            case TIME_BUDGET -> {
                if ("LIGHT".equalsIgnoreCase(draft.timeBudget())) {
                    yield "你的可投入时间较少，规划会优先保障关键路径。";
                }
                yield "你的时间投入可以支持稳定推进当前学习计划。";
            }
            case LEARNING_PREFERENCE -> "你的学习偏好会影响每个阶段的解释方式与练习比重。";
            case DIFFICULTY_PAIN_POINT -> "你的主要卡点会影响任务难度分布与讲解方式。";
            default -> FALLBACK_CONCLUSION;
        };
    }

    private boolean containsAny(List<String> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            for (String key : keys) {
                if (normalized.contains(key.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String resolveSelectedAnswerLabel(DiagnosisAnswer answer, DiagnosisQuestion question) {
        if (answer.getRawText() != null && !answer.getRawText().isBlank()) {
            return answer.getRawText().trim();
        }
        if (question == null || answer.getAnswerValueJson() == null) {
            return FALLBACK_SELECTED_ANSWER;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(answer.getAnswerValueJson());
            if (jsonNode == null || jsonNode.isNull()) {
                return FALLBACK_SELECTED_ANSWER;
            }
            if (jsonNode.isArray()) {
                List<String> labels = new ArrayList<>();
                for (JsonNode item : jsonNode) {
                    labels.add(resolveOptionLabel(question, item.asText("")));
                }
                return labels.isEmpty() ? FALLBACK_SELECTED_ANSWER : String.join(" | ", labels);
            }
            return resolveOptionLabel(question, jsonNode.asText(""));
        } catch (Exception ex) {
            return FALLBACK_SELECTED_ANSWER;
        }
    }

    private String resolveOptionLabel(DiagnosisQuestion question, String code) {
        for (DiagnosisQuestionOption option : question.options()) {
            if (option.code().equalsIgnoreCase(code)) {
                return option.label();
            }
        }
        return fallbackText(code, FALLBACK_SELECTED_ANSWER);
    }

    private String fallbackQuestionId(DiagnosisQuestion question) {
        if (question == null) {
            return FALLBACK_QUESTION_ID;
        }
        return fallbackText(question.questionId(), FALLBACK_QUESTION_ID);
    }

    private String fallbackQuestionTitle(DiagnosisQuestion question) {
        if (question == null) {
            return FALLBACK_QUESTION_TITLE;
        }
        return fallbackText(question.title(), FALLBACK_QUESTION_TITLE);
    }

    private DiagnosisDimension firstAvailableDimension(Map<DiagnosisDimension, DiagnosisQuestion> questionByDimension) {
        if (questionByDimension == null || questionByDimension.isEmpty()) {
            return DiagnosisDimension.FOUNDATION;
        }
        return questionByDimension.keySet().stream().findFirst().orElse(DiagnosisDimension.FOUNDATION);
    }

    private String fallbackText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    public record DiagnosisExplanation(
        List<DiagnosisReasoningStepDto> reasoningSteps,
        List<DiagnosisEvidenceSourceDto> strengthSources,
        List<DiagnosisEvidenceSourceDto> weaknessSources
    ) {
    }
}
