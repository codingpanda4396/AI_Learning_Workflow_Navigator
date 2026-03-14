package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisNextActionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionCopyDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionDto;
import com.pandanav.learning.api.dto.diagnosis.GenerateDiagnosisResponse;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisAnswerRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisResponse;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.DiagnosisSession;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.domain.repository.DiagnosisAnswerRepository;
import com.pandanav.learning.domain.repository.DiagnosisSessionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.service.CapabilityProfileBuilder;
import com.pandanav.learning.domain.service.DiagnosisTemplateFactory;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiagnosisService {

    private static final DiagnosisNextActionDto NEXT_ACTION = new DiagnosisNextActionDto("PATH_PLAN", "进入个性化学习路径");

    private final SessionRepository sessionRepository;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;
    private final CapabilityProfileRepository capabilityProfileRepository;
    private final DiagnosisTemplateFactory diagnosisTemplateFactory;
    private final DiagnosisQuestionCopyLlmService diagnosisQuestionCopyLlmService;
    private final CapabilityProfileBuilder capabilityProfileBuilder;
    private final CapabilityProfileSummaryGenerator capabilityProfileSummaryGenerator;
    private final CapabilityProfileSummaryLlmService capabilityProfileSummaryLlmService;
    private final ObjectMapper objectMapper;

    public DiagnosisService(
        SessionRepository sessionRepository,
        DiagnosisSessionRepository diagnosisSessionRepository,
        DiagnosisAnswerRepository diagnosisAnswerRepository,
        CapabilityProfileRepository capabilityProfileRepository,
        DiagnosisTemplateFactory diagnosisTemplateFactory,
        DiagnosisQuestionCopyLlmService diagnosisQuestionCopyLlmService,
        CapabilityProfileBuilder capabilityProfileBuilder,
        CapabilityProfileSummaryGenerator capabilityProfileSummaryGenerator,
        CapabilityProfileSummaryLlmService capabilityProfileSummaryLlmService,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.diagnosisSessionRepository = diagnosisSessionRepository;
        this.diagnosisAnswerRepository = diagnosisAnswerRepository;
        this.capabilityProfileRepository = capabilityProfileRepository;
        this.diagnosisTemplateFactory = diagnosisTemplateFactory;
        this.diagnosisQuestionCopyLlmService = diagnosisQuestionCopyLlmService;
        this.capabilityProfileBuilder = capabilityProfileBuilder;
        this.capabilityProfileSummaryGenerator = capabilityProfileSummaryGenerator;
        this.capabilityProfileSummaryLlmService = capabilityProfileSummaryLlmService;
        this.objectMapper = objectMapper;
    }

    public GenerateDiagnosisResponse generateDiagnosis(Long sessionId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));

        List<DiagnosisQuestion> fallbackQuestions = diagnosisTemplateFactory.buildQuestions(session);
        DiagnosisQuestionCopyLlmService.DiagnosisQuestionCopyResult copyResult =
            diagnosisQuestionCopyLlmService.enhanceQuestions(session, fallbackQuestions);
        List<DiagnosisQuestion> questions = copyResult.questions();

        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setLearningSessionId(session.getId());
        diagnosisSession.setUserPk(userId);
        diagnosisSession.setStatus(DiagnosisStatus.GENERATED);
        diagnosisSession.setGeneratedQuestionsJson(toJson(questionsToMap(questions)));
        diagnosisSession.setStartedAt(OffsetDateTime.now());

        DiagnosisSession saved = diagnosisSessionRepository.save(diagnosisSession);
        return new GenerateDiagnosisResponse(
            saved.getId(),
            session.getId(),
            toQuestionDtos(questions),
            copyResult.fallbackApplied(),
            copyResult.fallbackReasons(),
            copyResult.fallbackApplied() ? "RULE_FALLBACK" : "LLM"
        );
    }

    public SubmitDiagnosisResponse submitDiagnosis(SubmitDiagnosisRequest request, Long userId) {
        DiagnosisSession diagnosisSession = diagnosisSessionRepository.findByIdAndUserPk(request.diagnosisId(), userId)
            .orElseThrow(() -> new NotFoundException("Diagnosis session not found."));
        LearningSession learningSession = sessionRepository.findByIdAndUserPk(diagnosisSession.getLearningSessionId(), userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));

        List<DiagnosisQuestion> questions = readQuestions(diagnosisSession.getGeneratedQuestionsJson());
        Map<String, DiagnosisQuestion> questionMap = questions.stream()
            .collect(Collectors.toMap(DiagnosisQuestion::questionId, question -> question, (left, right) -> left, LinkedHashMap::new));

        validateAnswers(questionMap, request.answers());
        List<DiagnosisAnswer> answers = buildAnswers(diagnosisSession.getId(), questionMap, request.answers());
        diagnosisAnswerRepository.saveAll(answers);
        diagnosisSessionRepository.updateStatus(diagnosisSession.getId(), DiagnosisStatus.SUBMITTED, null);

        Map<DiagnosisDimension, List<String>> answersByDimension = answers.stream()
            .collect(Collectors.groupingBy(
                DiagnosisAnswer::getDimension,
                LinkedHashMap::new,
                Collectors.flatMapping(answer -> extractAnswerTexts(answer).stream(), Collectors.toList())
            ));

        CapabilityProfileDraft draft = capabilityProfileBuilder.build(answersByDimension);
        CapabilityProfileSummaryCopy fallbackSummary = capabilityProfileSummaryGenerator.buildFallback(draft);
        CapabilityProfileSummaryLlmService.CapabilityProfileSummaryResult summaryResult =
            capabilityProfileSummaryLlmService.generate(learningSession, draft, answersByDimension);
        CapabilityProfileSummaryCopy summaryCopy = summaryResult.copy() == null ? fallbackSummary : summaryResult.copy();

        CapabilityProfile profile = new CapabilityProfile();
        profile.setLearningSessionId(learningSession.getId());
        profile.setUserPk(userId);
        profile.setSourceDiagnosisId(diagnosisSession.getId());
        profile.setCurrentLevel(draft.currentLevel());
        profile.setStrengths(draft.strengths());
        profile.setWeaknesses(draft.weaknesses());
        profile.setLearningPreference(draft.learningPreference());
        profile.setTimeBudget(draft.timeBudget());
        profile.setGoalOrientation(draft.goalOrientation());
        profile.setSummaryText(summaryCopy.summary());
        profile.setPlanExplanation(summaryCopy.planExplanation());
        profile.setVersion(nextVersion(learningSession.getId()));

        CapabilityProfile savedProfile = capabilityProfileRepository.save(profile);
        diagnosisSessionRepository.updateStatus(diagnosisSession.getId(), DiagnosisStatus.PROFILED, OffsetDateTime.now());

        return new SubmitDiagnosisResponse(
            toProfileDto(savedProfile),
            NEXT_ACTION,
            summaryResult.fallbackApplied(),
            summaryResult.fallbackReasons(),
            summaryResult.fallbackApplied() ? "RULE_FALLBACK" : "LLM"
        );
    }

    private void validateAnswers(Map<String, DiagnosisQuestion> questionMap, List<SubmitDiagnosisAnswerRequest> answerRequests) {
        Map<String, SubmitDiagnosisAnswerRequest> submittedByQuestionId = new LinkedHashMap<>();
        for (SubmitDiagnosisAnswerRequest answer : answerRequests) {
            String questionId = answer.questionId() == null ? "" : answer.questionId().trim();
            DiagnosisQuestion question = questionMap.get(questionId);
            if (question == null) {
                throw new BadRequestException("Unknown diagnosis question: " + questionId);
            }
            if (submittedByQuestionId.put(questionId, answer) != null) {
                throw new BadRequestException("Duplicate diagnosis answer: " + questionId);
            }
            validateAnswerValue(question, normalizeAnswerValue(question, answer));
        }
        for (DiagnosisQuestion question : questionMap.values()) {
            if (question.required() && !submittedByQuestionId.containsKey(question.questionId())) {
                throw new BadRequestException("Missing diagnosis answer: " + question.questionId());
            }
        }
    }

    private void validateAnswerValue(DiagnosisQuestion question, JsonNode value) {
        if (value == null || value.isNull()) {
            throw new BadRequestException("Diagnosis answer value is required.");
        }
        if ("multiple_choice".equalsIgnoreCase(question.type())) {
            if (!value.isArray() || value.isEmpty()) {
                throw new BadRequestException("Question %s requires multiple choices.".formatted(question.questionId()));
            }
            for (JsonNode item : value) {
                assertAllowedOption(question, item.asText(""));
            }
            return;
        }
        if ("text".equalsIgnoreCase(question.type())) {
            if (value.isArray() || value.isObject() || value.asText("").isBlank()) {
                throw new BadRequestException("Question %s requires text input.".formatted(question.questionId()));
            }
            return;
        }
        if (value.isArray() || value.isObject()) {
            throw new BadRequestException("Question %s requires a single value.".formatted(question.questionId()));
        }
        assertAllowedOption(question, value.asText(""));
    }

    private void assertAllowedOption(DiagnosisQuestion question, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || ContractCatalog.diagnosisOption(question.dimension(), normalized).isEmpty()) {
            throw new BadRequestException("Invalid answer option for question: " + question.questionId());
        }
    }

    private List<DiagnosisAnswer> buildAnswers(
        Long diagnosisId,
        Map<String, DiagnosisQuestion> questionMap,
        List<SubmitDiagnosisAnswerRequest> answerRequests
    ) {
        List<DiagnosisAnswer> answers = new ArrayList<>();
        for (SubmitDiagnosisAnswerRequest request : answerRequests) {
            DiagnosisQuestion question = questionMap.get(request.questionId().trim());
            JsonNode normalizedValue = normalizeAnswerValue(question, request);
            DiagnosisAnswer answer = new DiagnosisAnswer();
            answer.setDiagnosisSessionId(diagnosisId);
            answer.setQuestionId(question.questionId());
            answer.setDimension(question.dimension());
            answer.setAnswerType(question.type().toUpperCase(Locale.ROOT));
            answer.setAnswerValueJson(toJson(normalizedValue));
            answer.setRawText(toRawText(question, normalizedValue));
            answers.add(answer);
        }
        return answers;
    }

    private List<String> extractAnswerTexts(DiagnosisAnswer answer) {
        try {
            JsonNode jsonNode = objectMapper.readTree(answer.getAnswerValueJson());
            if (jsonNode.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode item : jsonNode) {
                    values.add(ContractCatalog.diagnosisOptionLabel(answer.getDimension(), item.asText("")));
                }
                return values;
            }
            return List.of(ContractCatalog.diagnosisOptionLabel(answer.getDimension(), jsonNode.asText("")));
        } catch (Exception ex) {
            return answer.getRawText() == null || answer.getRawText().isBlank() ? List.of() : List.of(answer.getRawText());
        }
    }

    private List<DiagnosisQuestion> readQuestions(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            List<DiagnosisQuestion> questions = new ArrayList<>();
            for (JsonNode item : node) {
                List<String> options = new ArrayList<>();
                for (JsonNode option : item.path("options")) {
                    options.add(option.isObject() ? option.path("label").asText("") : option.asText(""));
                }
                DiagnosisQuestionCopy copy = readCopy(item.path("copy"), item);
                questions.add(new DiagnosisQuestion(
                    item.path("questionId").asText(""),
                    DiagnosisDimension.valueOf(item.path("dimension").asText("FOUNDATION")),
                    item.path("type").asText("single_choice"),
                    textOrDefault(item.path("title").asText(""), copy.title()),
                    textOrDefault(item.path("description").asText(""), copy.description()),
                    options,
                    item.path("required").asBoolean(true),
                    copy
                ));
            }
            return questions;
        } catch (Exception ex) {
            throw new InternalServerException("Failed to read generated diagnosis questions.");
        }
    }

    private DiagnosisQuestionCopy readCopy(JsonNode copyNode, JsonNode questionNode) {
        if (copyNode != null && copyNode.isObject()) {
            return new DiagnosisQuestionCopy(
                copyNode.path("sectionLabel").asText(""),
                copyNode.path("title").asText(questionNode.path("title").asText("")),
                copyNode.path("description").asText(questionNode.path("description").asText("")),
                copyNode.path("placeholder").asText(""),
                copyNode.path("submitHint").asText("")
            );
        }
        return new DiagnosisQuestionCopy(
            questionNode.path("dimension").asText(""),
            questionNode.path("title").asText(""),
            questionNode.path("description").asText(""),
            "",
            ""
        );
    }

    private List<Map<String, Object>> questionsToMap(List<DiagnosisQuestion> questions) {
        return questions.stream().map(question -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", question.questionId());
            item.put("dimension", question.dimension().name());
            item.put("type", question.type());
            item.put("title", question.title());
            item.put("description", question.description());
            item.put("options", ContractCatalog.diagnosisOptions(question.dimension(), question.options()));
            item.put("required", question.required());
            item.put("copy", question.copy());
            return item;
        }).toList();
    }

    private CapabilityProfileDto toProfileDto(CapabilityProfile profile) {
        return new CapabilityProfileDto(
            ContractCatalog.capabilityLevel(profile.getCurrentLevel()),
            profile.getStrengths(),
            profile.getWeaknesses(),
            ContractCatalog.learningPreference(profile.getLearningPreference()),
            ContractCatalog.timeBudget(profile.getTimeBudget()),
            ContractCatalog.goalOrientation(profile.getGoalOrientation()),
            profile.getSummaryText(),
            profile.getPlanExplanation()
        );
    }

    private List<DiagnosisQuestionDto> toQuestionDtos(List<DiagnosisQuestion> questions) {
        return questions.stream()
            .map(question -> new DiagnosisQuestionDto(
                question.questionId(),
                ContractCatalog.diagnosisDimension(question.dimension()),
                ContractCatalog.diagnosisQuestionType(question.type()),
                question.title(),
                question.description(),
                ContractCatalog.diagnosisOptions(question.dimension(), question.options()),
                question.required(),
                new DiagnosisQuestionCopyDto(
                    question.copy().sectionLabel(),
                    question.copy().title(),
                    question.copy().description(),
                    question.copy().placeholder(),
                    question.copy().submitHint()
                )
            ))
            .toList();
    }

    private int nextVersion(Long sessionId) {
        return capabilityProfileRepository.findLatestBySessionId(sessionId)
            .map(existing -> existing.getVersion() == null ? 1 : existing.getVersion() + 1)
            .orElse(1);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize diagnosis payload.");
        }
    }

    private JsonNode normalizeAnswerValue(DiagnosisQuestion question, SubmitDiagnosisAnswerRequest request) {
        if (request.value() != null && !request.value().isNull()) {
            return request.value();
        }
        if ("multiple_choice".equalsIgnoreCase(question.type())) {
            List<String> answerCodes = request.answerCodes() == null ? List.of() : request.answerCodes().stream()
                .map(code -> ContractCatalog.diagnosisOptionCode(question.dimension(), code))
                .toList();
            return objectMapper.valueToTree(answerCodes);
        }
        if ("text".equalsIgnoreCase(question.type())) {
            return objectMapper.valueToTree(request.answerText());
        }
        String firstCode = request.answerCodes() == null || request.answerCodes().isEmpty() ? null : request.answerCodes().get(0);
        return objectMapper.valueToTree(ContractCatalog.diagnosisOptionCode(question.dimension(), firstCode));
    }

    private String toRawText(DiagnosisQuestion question, JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : value) {
                items.add(ContractCatalog.diagnosisOptionLabel(question.dimension(), item.asText("")));
            }
            return String.join(" | ", items);
        }
        return "text".equalsIgnoreCase(question.type())
            ? value.asText("")
            : ContractCatalog.diagnosisOptionLabel(question.dimension(), value.asText(""));
    }

    private String textOrDefault(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
    }
}
