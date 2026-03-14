package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileDto;
import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionResponse;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisActionTargetDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisAnswerSubmissionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisFallbackDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisInsightsDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisMetadataDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisNextActionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionDto;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionResponse;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private final SessionRepository sessionRepository;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;
    private final CapabilityProfileRepository capabilityProfileRepository;
    private final DiagnosisTemplateFactory diagnosisTemplateFactory;
    private final DiagnosisQuestionCopyLlmService diagnosisQuestionCopyLlmService;
    private final CapabilityProfileBuilder capabilityProfileBuilder;
    private final DiagnosisExplanationAssembler diagnosisExplanationAssembler;
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
        DiagnosisExplanationAssembler diagnosisExplanationAssembler,
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
        this.diagnosisExplanationAssembler = diagnosisExplanationAssembler;
        this.capabilityProfileSummaryLlmService = capabilityProfileSummaryLlmService;
        this.objectMapper = objectMapper;
    }

    public CreateDiagnosisSessionResponse createDiagnosisSession(Long sessionId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));

        List<DiagnosisQuestion> sourceQuestions = diagnosisTemplateFactory.buildQuestions(session);
        List<DiagnosisQuestion> questions = diagnosisQuestionCopyLlmService.enhanceQuestions(session, sourceQuestions);

        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setLearningSessionId(session.getId());
        diagnosisSession.setUserPk(userId);
        diagnosisSession.setStatus(DiagnosisStatus.GENERATED);
        diagnosisSession.setGeneratedQuestionsJson(toJson(questionsToMap(questions)));
        diagnosisSession.setStartedAt(OffsetDateTime.now());

        DiagnosisSession saved = diagnosisSessionRepository.save(diagnosisSession);
        return new CreateDiagnosisSessionResponse(
            saved.getId(),
            session.getId(),
            DiagnosisStatus.GENERATED.name(),
            toQuestionDtos(questions),
            buildNextAction(session.getId(), saved.getId()),
            sourceMeta(false, List.of(), "LLM"),
            new DiagnosisMetadataDto(questions.size(), null, null)
        );
    }

    public SubmitDiagnosisSessionResponse submitDiagnosisSession(Long diagnosisId, SubmitDiagnosisSessionRequest request, Long userId) {
        Long resolvedDiagnosisId = diagnosisId != null ? diagnosisId : request.diagnosisId();
        if (resolvedDiagnosisId == null || resolvedDiagnosisId <= 0) {
            throw new BadRequestException("Diagnosis session id is required.");
        }

        DiagnosisSession diagnosisSession = diagnosisSessionRepository.findByIdAndUserPk(resolvedDiagnosisId, userId)
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

        Map<DiagnosisDimension, List<String>> answerCodesByDimension = answers.stream()
            .collect(Collectors.groupingBy(
                DiagnosisAnswer::getDimension,
                LinkedHashMap::new,
                Collectors.flatMapping(answer -> extractAnswerCodes(answer).stream(), Collectors.toList())
            ));

        CapabilityProfileDraft draft = capabilityProfileBuilder.build(answerCodesByDimension);
        CapabilityProfileSummaryCopy summaryCopy =
            capabilityProfileSummaryLlmService.generate(learningSession, draft, answerCodesByDimension);

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
        DiagnosisExplanationAssembler.DiagnosisExplanation explanation = diagnosisExplanationAssembler.assemble(
            questions,
            answers,
            draft,
            answerCodesByDimension
        );

        log.info(
            "DiagnosisService: submit completed. diagnosisId={}, sessionId={}, reasoningStepCount={}, strengthSourceCount={}, weaknessSourceCount={}",
            diagnosisSession.getId(),
            learningSession.getId(),
            explanation.reasoningSteps().size(),
            explanation.strengthSources().size(),
            explanation.weaknessSources().size()
        );

        return new SubmitDiagnosisSessionResponse(
            diagnosisSession.getId(),
            learningSession.getId(),
            DiagnosisStatus.PROFILED.name(),
            toProfileDto(savedProfile),
            new DiagnosisInsightsDto(savedProfile.getSummaryText(), savedProfile.getPlanExplanation()),
            buildNextAction(learningSession.getId(), diagnosisSession.getId()),
            sourceMeta(false, List.of(), "LLM"),
            new DiagnosisMetadataDto(questions.size(), answers.size(), savedProfile.getVersion()),
            explanation.reasoningSteps(),
            explanation.strengthSources(),
            explanation.weaknessSources()
        );
    }

    private DiagnosisNextActionDto buildNextAction(Long sessionId, Long diagnosisId) {
        return new DiagnosisNextActionDto(
            "PATH_PLAN",
            "进入个性化学习路径",
            new DiagnosisActionTargetDto(
                "/plan",
                Map.of("sessionId", sessionId, "diagnosisId", diagnosisId)
            )
        );
    }

    private DiagnosisFallbackDto sourceMeta(boolean applied, List<String> reasons, String contentSource) {
        return new DiagnosisFallbackDto(applied, reasons == null ? List.of() : reasons, contentSource);
    }

    private void validateAnswers(Map<String, DiagnosisQuestion> questionMap, List<DiagnosisAnswerSubmissionDto> answerRequests) {
        Map<String, DiagnosisAnswerSubmissionDto> submittedByQuestionId = new LinkedHashMap<>();
        for (DiagnosisAnswerSubmissionDto answer : answerRequests) {
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
        String typeCode = ContractCatalog.diagnosisQuestionTypeCode(question.type());
        if ("MULTIPLE_CHOICE".equals(typeCode)) {
            if (!value.isArray() || value.isEmpty()) {
                throw new BadRequestException("Question %s requires multiple choices.".formatted(question.questionId()));
            }
            for (JsonNode item : value) {
                assertAllowedOption(question, item.asText(""));
            }
            return;
        }
        if ("TEXT".equals(typeCode)) {
            if (value.isArray() || value.isObject() || value.asText("").isBlank()) {
                throw new BadRequestException("Question %s requires text input.".formatted(question.questionId()));
            }
            return;
        }
        if (value.isArray() || value.isObject()) {
            throw new BadRequestException("Question %s requires a single option code.".formatted(question.questionId()));
        }
        assertAllowedOption(question, value.asText(""));
    }

    private void assertAllowedOption(DiagnosisQuestion question, String value) {
        String normalized = value == null ? "" : value.trim();
        boolean matched = question.options().stream().anyMatch(option -> option.code().equalsIgnoreCase(normalized));
        if (!matched && ContractCatalog.diagnosisOption(question.dimension(), normalized).isEmpty()) {
            throw new BadRequestException("Invalid answer option for question: " + question.questionId());
        }
    }

    private List<DiagnosisAnswer> buildAnswers(
        Long diagnosisId,
        Map<String, DiagnosisQuestion> questionMap,
        List<DiagnosisAnswerSubmissionDto> answerRequests
    ) {
        List<DiagnosisAnswer> answers = new ArrayList<>();
        for (DiagnosisAnswerSubmissionDto request : answerRequests) {
            DiagnosisQuestion question = questionMap.get(request.questionId().trim());
            JsonNode normalizedValue = normalizeAnswerValue(question, request);
            DiagnosisAnswer answer = new DiagnosisAnswer();
            answer.setDiagnosisSessionId(diagnosisId);
            answer.setQuestionId(question.questionId());
            answer.setDimension(question.dimension());
            answer.setAnswerType(ContractCatalog.diagnosisQuestionTypeCode(question.type()));
            answer.setAnswerValueJson(toJson(normalizedValue));
            answer.setRawText(toRawText(question, normalizedValue));
            answers.add(answer);
        }
        return answers;
    }

    private List<String> extractAnswerCodes(DiagnosisAnswer answer) {
        try {
            JsonNode jsonNode = objectMapper.readTree(answer.getAnswerValueJson());
            if (jsonNode.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode item : jsonNode) {
                    values.add(normalizeOptionCode(answer.getDimension(), item.asText("")));
                }
                return values;
            }
            return List.of(normalizeOptionCode(answer.getDimension(), jsonNode.asText("")));
        } catch (Exception ex) {
            if (answer.getRawText() == null || answer.getRawText().isBlank()) {
                return List.of();
            }
            String[] parts = answer.getRawText().split("\\|");
            List<String> fallback = new ArrayList<>();
            for (String part : parts) {
                String code = normalizeOptionCode(answer.getDimension(), part.trim());
                if (!code.isBlank()) {
                    fallback.add(code);
                }
            }
            return fallback;
        }
    }

    private String normalizeOptionCode(DiagnosisDimension dimension, String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return ContractCatalog.diagnosisOptionCode(dimension, raw);
    }

    private List<DiagnosisQuestion> readQuestions(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            List<DiagnosisQuestion> questions = new ArrayList<>();
            for (JsonNode item : node) {
                DiagnosisDimension dimension = DiagnosisDimension.valueOf(item.path("dimension").asText("FOUNDATION"));
                DiagnosisQuestionCopy legacyCopy = readLegacyCopy(item.path("copy"), item);
                String title = textOrDefault(item.path("title").asText(""), legacyCopy.title());
                String description = textOrDefault(item.path("description").asText(""), legacyCopy.description());
                String placeholder = textOrDefault(item.path("placeholder").asText(""), legacyCopy.placeholder());
                String submitHint = textOrDefault(item.path("submitHint").asText(""), legacyCopy.submitHint());
                String sectionLabel = textOrDefault(item.path("sectionLabel").asText(""), legacyCopy.sectionLabel());
                questions.add(new DiagnosisQuestion(
                    item.path("questionId").asText(""),
                    dimension,
                    item.path("type").asText("single_choice"),
                    item.path("required").asBoolean(true),
                    readOptions(dimension, item.path("options")),
                    title,
                    description,
                    placeholder,
                    submitHint,
                    sectionLabel
                ));
            }
            return questions;
        } catch (Exception ex) {
            throw new InternalServerException("Failed to read generated diagnosis questions.");
        }
    }

    private DiagnosisQuestionCopy readLegacyCopy(JsonNode copyNode, JsonNode questionNode) {
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
            questionNode.path("placeholder").asText(""),
            questionNode.path("submitHint").asText("")
        );
    }

    private List<DiagnosisQuestionOption> readOptions(DiagnosisDimension dimension, JsonNode optionsNode) {
        List<DiagnosisQuestionOption> defaults = ContractCatalog.diagnosisQuestionOptions(dimension);
        if (!optionsNode.isArray() || optionsNode.isEmpty()) {
            return defaults;
        }
        List<DiagnosisQuestionOption> options = new ArrayList<>();
        for (int i = 0; i < optionsNode.size(); i++) {
            JsonNode optionNode = optionsNode.get(i);
            DiagnosisQuestionOption defaultOption = i < defaults.size() ? defaults.get(i) : null;
            String fallbackCode = defaultOption == null ? "" : defaultOption.code();
            String fallbackLabel = defaultOption == null ? "" : defaultOption.label();
            String code = optionNode.isObject()
                ? textOrDefault(optionNode.path("code").asText(""), fallbackCode)
                : normalizeOptionCode(dimension, optionNode.asText(""));
            String label = optionNode.isObject()
                ? textOrDefault(optionNode.path("label").asText(""), fallbackLabel)
                : textOrDefault(optionNode.asText(""), fallbackLabel);
            Integer order = optionNode.isObject() && optionNode.has("order")
                ? optionNode.path("order").asInt(i + 1)
                : i + 1;
            if (code.isBlank() && !label.isBlank()) {
                code = normalizeOptionCode(dimension, label);
            }
            if (!code.isBlank() && !label.isBlank()) {
                options.add(new DiagnosisQuestionOption(code, label, order));
            }
        }
        return options.isEmpty() ? defaults : options;
    }

    private List<Map<String, Object>> questionsToMap(List<DiagnosisQuestion> questions) {
        return questions.stream().map(question -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", question.questionId());
            item.put("dimension", question.dimension().name());
            item.put("type", ContractCatalog.diagnosisQuestionTypeCode(question.type()));
            item.put("required", question.required());
            item.put("title", question.title());
            item.put("description", question.description());
            item.put("placeholder", question.placeholder());
            item.put("submitHint", question.submitHint());
            item.put("sectionLabel", question.sectionLabel());
            item.put("options", question.options());
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
            ContractCatalog.goalOrientation(profile.getGoalOrientation())
        );
    }

    private List<DiagnosisQuestionDto> toQuestionDtos(List<DiagnosisQuestion> questions) {
        return questions.stream()
            .map(question -> new DiagnosisQuestionDto(
                question.questionId(),
                question.dimension().name(),
                ContractCatalog.diagnosisQuestionTypeCode(question.type()),
                question.required(),
                ContractCatalog.diagnosisQuestionOptions(question.options()),
                question.title(),
                question.description(),
                question.placeholder(),
                question.submitHint(),
                question.sectionLabel()
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

    private JsonNode normalizeAnswerValue(DiagnosisQuestion question, DiagnosisAnswerSubmissionDto request) {
        if (request.legacyValue() != null && !request.legacyValue().isNull()) {
            return normalizeLegacyValue(question, request.legacyValue());
        }
        String typeCode = ContractCatalog.diagnosisQuestionTypeCode(question.type());
        if ("MULTIPLE_CHOICE".equals(typeCode)) {
            List<String> answerCodes = request.selectedOptionCodes() == null ? List.of() : request.selectedOptionCodes().stream()
                .map(code -> normalizeOptionCode(question.dimension(), code))
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
            return objectMapper.valueToTree(answerCodes);
        }
        if ("TEXT".equals(typeCode)) {
            return objectMapper.valueToTree(request.text());
        }
        String singleCode = textOrDefault(request.selectedOptionCode(), "");
        if (singleCode.isBlank() && request.selectedOptionCodes() != null && !request.selectedOptionCodes().isEmpty()) {
            singleCode = request.selectedOptionCodes().get(0);
        }
        return objectMapper.valueToTree(normalizeOptionCode(question.dimension(), singleCode));
    }

    private JsonNode normalizeLegacyValue(DiagnosisQuestion question, JsonNode legacyValue) {
        String typeCode = ContractCatalog.diagnosisQuestionTypeCode(question.type());
        if ("MULTIPLE_CHOICE".equals(typeCode) && legacyValue.isArray()) {
            List<String> codes = new ArrayList<>();
            for (JsonNode item : legacyValue) {
                String code = normalizeOptionCode(question.dimension(), item.asText(""));
                if (!code.isBlank()) {
                    codes.add(code);
                }
            }
            return objectMapper.valueToTree(codes);
        }
        if ("TEXT".equals(typeCode)) {
            return objectMapper.valueToTree(legacyValue.asText(""));
        }
        return objectMapper.valueToTree(normalizeOptionCode(question.dimension(), legacyValue.asText("")));
    }

    private String toRawText(DiagnosisQuestion question, JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        String typeCode = ContractCatalog.diagnosisQuestionTypeCode(question.type());
        if ("TEXT".equals(typeCode)) {
            return value.asText("");
        }
        if (value.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : value) {
                items.add(resolveOptionLabel(question, item.asText("")));
            }
            return String.join(" | ", items);
        }
        return resolveOptionLabel(question, value.asText(""));
    }

    private String resolveOptionLabel(DiagnosisQuestion question, String code) {
        return question.options().stream()
            .filter(option -> option.code().equalsIgnoreCase(code))
            .map(DiagnosisQuestionOption::label)
            .findFirst()
            .orElseGet(() -> ContractCatalog.diagnosisOptionLabel(question.dimension(), code));
    }

    private String textOrDefault(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
    }
}
