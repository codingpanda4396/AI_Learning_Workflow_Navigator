package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileDto;
import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionResponse;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisActionTargetDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisAnswerSubmissionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisDecisionHintsDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisExplanationDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisFallbackDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisInsightsDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisMetadataDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisNextActionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisStrategyDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerSnapshotDto;
import com.pandanav.learning.api.dto.diagnosis.PersonalizationMetaDto;
import com.pandanav.learning.api.dto.diagnosis.QuestionRationaleDto;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionResponse;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.enums.DiagnosisGenerationMode;
import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisLearnerProfileSnapshot;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCandidate;
import com.pandanav.learning.domain.model.DiagnosisQuestionDraft;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import com.pandanav.learning.domain.model.DiagnosisSignal;
import com.pandanav.learning.domain.model.DiagnosisSession;
import com.pandanav.learning.domain.model.DiagnosisStrategyDecision;
import com.pandanav.learning.domain.model.LearnerFeatureSignal;
import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanningContext;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.domain.repository.DiagnosisAnswerRepository;
import com.pandanav.learning.domain.repository.DiagnosisSessionRepository;
import com.pandanav.learning.domain.repository.LearnerFeatureSignalRepository;
import com.pandanav.learning.domain.repository.LearnerProfileSnapshotRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.service.CapabilityProfileBuilder;
import com.pandanav.learning.domain.service.DiagnosisTemplateFactory;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import com.pandanav.learning.infrastructure.observability.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private final SessionRepository sessionRepository;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;
    private final CapabilityProfileRepository capabilityProfileRepository;
    private final DiagnosisTemplateFactory diagnosisTemplateFactory;
    private final DiagnosisQuestionAssembler diagnosisQuestionAssembler;
    private final DiagnosisQuestionCopyLlmService diagnosisQuestionCopyLlmService;
    private final DiagnosisQuestionPersonalizer diagnosisQuestionPersonalizer;
    private final DiagnosisQuestionCopyNormalizer diagnosisQuestionCopyNormalizer;
    private final DiagnosisExplanationBuilder diagnosisExplanationBuilder;
    private final DiagnosisLearnerProfileBuilder diagnosisLearnerProfileBuilder;
    private final DiagnosisStrategyDecisionService diagnosisStrategyDecisionService;
    private final DiagnosisQuestionCandidateFactory diagnosisQuestionCandidateFactory;
    private final PersonalizedQuestionSelector personalizedQuestionSelector;
    private final DiagnosisQuestionCopyAdapter diagnosisQuestionCopyAdapter;
    private final QuestionRationaleBuilder questionRationaleBuilder;
    private final DiagnosisResponseAssembler diagnosisResponseAssembler;
    private final CapabilityProfileBuilder capabilityProfileBuilder;
    private final DiagnosisExplanationAssembler diagnosisExplanationAssembler;
    private final CapabilityProfileSummaryLlmService capabilityProfileSummaryLlmService;
    private final DiagnosisAnswerNormalizer diagnosisAnswerNormalizer;
    private final LearnerFeatureExtractor learnerFeatureExtractor;
    private final LearnerFeatureAggregator learnerFeatureAggregator;
    private final LearnerProfileSnapshotBuilder learnerProfileSnapshotBuilder;
    private final LearnerFeatureSignalRepository learnerFeatureSignalRepository;
    private final LearnerProfileSnapshotRepository learnerProfileSnapshotRepository;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public DiagnosisService(
        SessionRepository sessionRepository,
        DiagnosisSessionRepository diagnosisSessionRepository,
        DiagnosisAnswerRepository diagnosisAnswerRepository,
        CapabilityProfileRepository capabilityProfileRepository,
        DiagnosisTemplateFactory diagnosisTemplateFactory,
        DiagnosisQuestionAssembler diagnosisQuestionAssembler,
        DiagnosisQuestionCopyLlmService diagnosisQuestionCopyLlmService,
        DiagnosisQuestionPersonalizer diagnosisQuestionPersonalizer,
        DiagnosisQuestionCopyNormalizer diagnosisQuestionCopyNormalizer,
        DiagnosisExplanationBuilder diagnosisExplanationBuilder,
        DiagnosisLearnerProfileBuilder diagnosisLearnerProfileBuilder,
        DiagnosisStrategyDecisionService diagnosisStrategyDecisionService,
        DiagnosisQuestionCandidateFactory diagnosisQuestionCandidateFactory,
        PersonalizedQuestionSelector personalizedQuestionSelector,
        DiagnosisQuestionCopyAdapter diagnosisQuestionCopyAdapter,
        QuestionRationaleBuilder questionRationaleBuilder,
        DiagnosisResponseAssembler diagnosisResponseAssembler,
        CapabilityProfileBuilder capabilityProfileBuilder,
        DiagnosisExplanationAssembler diagnosisExplanationAssembler,
        CapabilityProfileSummaryLlmService capabilityProfileSummaryLlmService,
        DiagnosisAnswerNormalizer diagnosisAnswerNormalizer,
        LearnerFeatureExtractor learnerFeatureExtractor,
        LearnerFeatureAggregator learnerFeatureAggregator,
        LearnerProfileSnapshotBuilder learnerProfileSnapshotBuilder,
        LearnerFeatureSignalRepository learnerFeatureSignalRepository,
        LearnerProfileSnapshotRepository learnerProfileSnapshotRepository,
        LlmProperties llmProperties,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.diagnosisSessionRepository = diagnosisSessionRepository;
        this.diagnosisAnswerRepository = diagnosisAnswerRepository;
        this.capabilityProfileRepository = capabilityProfileRepository;
        this.diagnosisTemplateFactory = diagnosisTemplateFactory;
        this.diagnosisQuestionAssembler = diagnosisQuestionAssembler;
        this.diagnosisQuestionCopyLlmService = diagnosisQuestionCopyLlmService;
        this.diagnosisQuestionPersonalizer = diagnosisQuestionPersonalizer;
        this.diagnosisQuestionCopyNormalizer = diagnosisQuestionCopyNormalizer;
        this.diagnosisExplanationBuilder = diagnosisExplanationBuilder;
        this.diagnosisLearnerProfileBuilder = diagnosisLearnerProfileBuilder;
        this.diagnosisStrategyDecisionService = diagnosisStrategyDecisionService;
        this.diagnosisQuestionCandidateFactory = diagnosisQuestionCandidateFactory;
        this.personalizedQuestionSelector = personalizedQuestionSelector;
        this.diagnosisQuestionCopyAdapter = diagnosisQuestionCopyAdapter;
        this.questionRationaleBuilder = questionRationaleBuilder;
        this.diagnosisResponseAssembler = diagnosisResponseAssembler;
        this.capabilityProfileBuilder = capabilityProfileBuilder;
        this.diagnosisExplanationAssembler = diagnosisExplanationAssembler;
        this.capabilityProfileSummaryLlmService = capabilityProfileSummaryLlmService;
        this.diagnosisAnswerNormalizer = diagnosisAnswerNormalizer;
        this.learnerFeatureExtractor = learnerFeatureExtractor;
        this.learnerFeatureAggregator = learnerFeatureAggregator;
        this.learnerProfileSnapshotBuilder = learnerProfileSnapshotBuilder;
        this.learnerFeatureSignalRepository = learnerFeatureSignalRepository;
        this.learnerProfileSnapshotRepository = learnerProfileSnapshotRepository;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public CreateDiagnosisSessionResponse createDiagnosisSession(Long sessionId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));
        PlanningContext planningContext = buildPlanningContext(session);
        DiagnosisLearnerProfileSnapshot profileSnapshot = diagnosisLearnerProfileBuilder.build(session, planningContext);
        DiagnosisStrategyDecision strategyDecision = diagnosisStrategyDecisionService.decide(session, planningContext, profileSnapshot);
        String topic = resolveTopic(planningContext);
        log.info(
            "DIAGNOSIS_GENERATION_START traceId={} sessionId={} generationMode={} questionCount={} topic={}",
            TraceContext.traceId(),
            session.getId(),
            "PENDING",
            0,
            topic
        );

        List<DiagnosisQuestionCandidate> candidates =
            diagnosisQuestionCandidateFactory.buildCandidates(session, planningContext);
        List<DiagnosisQuestionDraft> selectedDrafts =
            personalizedQuestionSelector.select(candidates, strategyDecision, profileSnapshot);
        List<DiagnosisQuestion> adaptedQuestions =
            diagnosisQuestionCopyAdapter.adapt(selectedDrafts, planningContext, profileSnapshot, strategyDecision);
        List<DiagnosisQuestion> assembledQuestions = diagnosisQuestionAssembler.assemble(session, adaptedQuestions);
        DiagnosisGenerationMode generationMode = DiagnosisGenerationMode.LLM;
        boolean fallbackApplied = false;
        List<String> fallbackReasons = List.of();
        List<DiagnosisQuestion> questions;
        try {
            questions = diagnosisQuestionCopyLlmService.enhanceQuestions(session, assembledQuestions);
        } catch (Exception ex) {
            if (isDiagnosisCopyStrictMode()) {
                if (ex instanceof AiGenerationException aiEx) {
                    throw aiEx;
                }
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "API_ERROR");
            }
            generationMode = DiagnosisGenerationMode.RULE_FALLBACK;
            fallbackApplied = true;
            fallbackReasons = List.of(resolveFailureReason(ex, "LLM_DIAGNOSIS_COPY_UNAVAILABLE"));
            questions = assembledQuestions;
        }
        questions = diagnosisQuestionCopyNormalizer.normalize(questions);

        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setLearningSessionId(session.getId());
        diagnosisSession.setUserPk(userId);
        diagnosisSession.setStatus(DiagnosisStatus.READY);
        diagnosisSession.setGeneratedQuestionsJson(toJson(questionsToMap(questions)));
        diagnosisSession.setStartedAt(OffsetDateTime.now());

        DiagnosisSession saved = diagnosisSessionRepository.save(diagnosisSession);
        DiagnosisExplanationDto diagnosisExplanation = diagnosisExplanationBuilder.build(
            planningContext, profileSnapshot, strategyDecision, questions);
        DiagnosisDecisionHintsDto decisionHints = buildDecisionHints(strategyDecision, profileSnapshot);
        LearnerSnapshotDto learnerSnapshotDto = buildLearnerSnapshotDto(profileSnapshot, planningContext);
        DiagnosisStrategyDto diagnosisStrategyDto = buildDiagnosisStrategyDto(strategyDecision);
        List<QuestionRationaleDto> questionRationales =
            questionRationaleBuilder.build(selectedDrafts, profileSnapshot, strategyDecision);
        PersonalizationMetaDto personalizationMeta = buildPersonalizationMetaDto(profileSnapshot, strategyDecision);
        log.info(
            "DIAGNOSIS_GENERATION_RESULT traceId={} sessionId={} generationMode={} questionCount={} topic={}",
            TraceContext.traceId(),
            session.getId(),
            generationMode.name(),
            questions.size(),
            topic
        );
        return diagnosisResponseAssembler.assemble(
            saved.getId(),
            session.getId(),
            DiagnosisStatus.READY.name(),
            generationMode.name(),
            toQuestionDtos(questions),
            diagnosisExplanation,
            buildNextAction(session.getId(), saved.getId()),
            decisionHints,
            sourceMeta(fallbackApplied, fallbackReasons, generationMode.name()),
            new DiagnosisMetadataDto(questions.size(), null, null),
            learnerSnapshotDto,
            diagnosisStrategyDto,
            questionRationales,
            personalizationMeta
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

        List<DiagnosisAnswerNormalizer.NormalizedDiagnosisAnswer> normalizedAnswers =
            diagnosisAnswerNormalizer.normalize(questions, answers);
        Map<DiagnosisDimension, List<String>> answerCodesByDimension = normalizedAnswers.stream()
            .collect(Collectors.groupingBy(
                normalizedAnswer -> questionMap.get(normalizedAnswer.questionId()).dimension(),
                LinkedHashMap::new,
                Collectors.flatMapping(normalizedAnswer -> normalizedAnswer.selectedOptionCodes().stream(), Collectors.toList())
            ));
        List<LearnerFeatureSignal> featureSignals = learnerFeatureExtractor.extract(
            learningSession.getId(),
            userId,
            questions,
            normalizedAnswers
        );
        learnerFeatureSignalRepository.saveAll(featureSignals);
        LearnerFeatureAggregator.AggregationResult aggregationResult = learnerFeatureAggregator.aggregate(featureSignals);

        CapabilityProfileDraft draft = capabilityProfileBuilder.build(answerCodesByDimension);
        CapabilityProfileSummaryCopy summaryCopy;
        try {
            summaryCopy = capabilityProfileSummaryLlmService.generate(learningSession, draft, answerCodesByDimension);
        } catch (AiGenerationException ex) {
            if (isCapabilitySummaryStrictMode()) {
                throw ex;
            }
            log.warn(
                "Diagnosis capability summary fallback enabled. diagnosisId={} sessionId={} reason={}",
                diagnosisSession.getId(),
                learningSession.getId(),
                ex.getReason()
            );
            summaryCopy = buildRuleBasedSummaryCopy(draft, answerCodesByDimension);
        }

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
        LearnerProfileSnapshot snapshot = learnerProfileSnapshotBuilder.build(
            diagnosisSession.getId(),
            learningSession.getId(),
            userId,
            savedProfile.getVersion(),
            aggregationResult
        );
        LearnerProfileSnapshot savedSnapshot = learnerProfileSnapshotRepository.saveOrUpdate(snapshot);
        diagnosisSessionRepository.updateStatus(diagnosisSession.getId(), DiagnosisStatus.EVALUATED, OffsetDateTime.now());
        DiagnosisExplanationAssembler.DiagnosisExplanation explanation = diagnosisExplanationAssembler.assemble(
            questions,
            answers,
            draft,
            answerCodesByDimension
        );

        log.info(
            "DiagnosisService: submit completed. diagnosisId={}, sessionId={}, reasoningStepCount={}, strengthSourceCount={}, weaknessSourceCount={}, featureSignalCount={}, snapshotId={}",
            diagnosisSession.getId(),
            learningSession.getId(),
            explanation.reasoningSteps().size(),
            explanation.strengthSources().size(),
            explanation.weaknessSources().size(),
            featureSignals.size(),
            savedSnapshot.getId()
        );

        return new SubmitDiagnosisSessionResponse(
            diagnosisSession.getId(),
            learningSession.getId(),
            DiagnosisStatus.EVALUATED.name(),
            toProfileDto(savedProfile),
            new DiagnosisInsightsDto(
                savedProfile.getSummaryText(),
                savedProfile.getPlanExplanation(),
                savedSnapshot.getFeatureSummary(),
                savedSnapshot.getStrategyHints(),
                savedSnapshot.getConstraints()
            ),
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

    private DiagnosisDecisionHintsDto buildDecisionHints(
        DiagnosisStrategyDecision strategyDecision,
        DiagnosisLearnerProfileSnapshot profileSnapshot
    ) {
        List<String> factors = new ArrayList<>();
        if (strategyDecision != null && strategyDecision.priorityDimensions() != null) {
            for (String dim : strategyDecision.priorityDimensions()) {
                String hint = switch (dim) {
                    case "FOUNDATION" -> "FOUNDATION_LEVEL";
                    case "TIME_BUDGET" -> "TIME_BUDGET";
                    case "GOAL_STYLE" -> "GOAL_STYLE";
                    case "LEARNING_PREFERENCE" -> "LEARNING_PREFERENCE";
                    case "EXPERIENCE" -> "EXPERIENCE";
                    case "DIFFICULTY_PAIN_POINT" -> "DIFFICULTY_PAIN_POINT";
                    default -> dim;
                };
                if (!factors.contains(hint)) {
                    factors.add(hint);
                }
            }
        }
        if (factors.isEmpty()) {
            factors.add("FOUNDATION_LEVEL");
            factors.add("TIME_BUDGET");
            factors.add("GOAL_STYLE");
        }
        return new DiagnosisDecisionHintsDto(factors);
    }

    private LearnerSnapshotDto buildLearnerSnapshotDto(
        DiagnosisLearnerProfileSnapshot profile,
        PlanningContext planningContext
    ) {
        String topic = resolveTopic(planningContext);
        String summary = buildLearnerSnapshotSummary(profile, topic);
        List<String> signals = new ArrayList<>(profile.evidence() != null ? profile.evidence() : List.of());
        List<String> riskTags = new ArrayList<>();
        if (profile.hasContradictionRisk()) {
            riskTags.add("CONTRADICTION_RISK");
        }
        if (profile.hasRecentFailures()) {
            riskTags.add("RECENT_FAILURES");
        }
        if (profile.weaknessTags() != null && !profile.weaknessTags().isEmpty()) {
            riskTags.addAll(profile.weaknessTags());
        }
        return new LearnerSnapshotDto(summary, signals, riskTags);
    }

    private String buildLearnerSnapshotSummary(DiagnosisLearnerProfileSnapshot profile, String topic) {
        if (profile.goalClarity() != null && "HIGH".equals(profile.goalClarity())) {
            if (topic != null && !topic.isBlank()) {
                return "你当前目标比较明确，系统会重点确认你在「" + topic + "」上的前置基础与投入边界是否匹配。";
            }
            return "你当前目标比较明确，系统会重点确认前置基础与投入边界是否匹配。";
        }
        if (profile.evidence() == null || profile.evidence().isEmpty()) {
            return "当前学习数据较少，系统会通过以下问题先确认你的起点与目标。";
        }
        return "系统将根据你的目标与主题，确认学习起点并据此规划路径。";
    }

    private DiagnosisStrategyDto buildDiagnosisStrategyDto(DiagnosisStrategyDecision strategy) {
        if (strategy == null) {
            return new DiagnosisStrategyDto("FOUNDATION_FIRST", "先确认起点，再决定后续规划", List.of("前置基础", "时间投入", "学习目标"));
        }
        String label = resolveStrategyLabel(strategy.strategyCode());
        List<String> focuses = new ArrayList<>();
        if (strategy.priorityDimensions() != null) {
            for (String d : strategy.priorityDimensions()) {
                focuses.add(dimensionToFocusLabel(d));
            }
        }
        if (focuses.isEmpty()) {
            focuses.add("前置基础");
            focuses.add("时间投入");
            focuses.add("学习目标");
        }
        return new DiagnosisStrategyDto(strategy.strategyCode(), label, focuses);
    }

    private String resolveStrategyLabel(String code) {
        if (code == null) return "先确认起点，再决定后续规划";
        return switch (code) {
            case DiagnosisStrategyDecisionService.STRATEGY_FOUNDATION_FIRST -> "先确认起点，再决定是否直接进入目标内容";
            case DiagnosisStrategyDecisionService.STRATEGY_CONFLICT_CHECK -> "优先核实目标与基础是否匹配，避免冲突";
            case DiagnosisStrategyDecisionService.STRATEGY_FAST_TRIAGE -> "精简题量，快速定档后开始学习";
            default -> "先确认起点，再决定后续规划";
        };
    }

    private String dimensionToFocusLabel(String dimension) {
        if (dimension == null) return "";
        return switch (dimension) {
            case "FOUNDATION" -> "前置基础";
            case "TIME_BUDGET" -> "时间投入";
            case "GOAL_STYLE" -> "学习目标";
            case "LEARNING_PREFERENCE" -> "学习偏好";
            case "EXPERIENCE" -> "过往经验";
            case "DIFFICULTY_PAIN_POINT" -> "难点与支持";
            default -> dimension;
        };
    }

    private PersonalizationMetaDto buildPersonalizationMetaDto(
        DiagnosisLearnerProfileSnapshot profile,
        DiagnosisStrategyDecision strategy
    ) {
        List<String> usedSignals = new ArrayList<>();
        if (profile.evidence() != null && !profile.evidence().isEmpty()) {
            usedSignals.add("goalText");
            usedSignals.add("chapterId");
        }
        if (profile.hasHistory()) usedSignals.add("historyAvailable");
        if (profile.weaknessTags() != null && !profile.weaknessTags().isEmpty()) {
            usedSignals.add("recentWeaknessTags");
        }
        String level = usedSignals.size() >= 2 || (strategy != null && strategy.personalizationReasons() != null && !strategy.personalizationReasons().isEmpty())
            ? "HIGH" : "MEDIUM";
        return new PersonalizationMetaDto(level, usedSignals, "PROFILE_DRIVEN");
    }

    private PlanningContext buildPlanningContext(LearningSession session) {
        return new PlanningContext(
            safeText(session == null ? null : session.getGoalText()),
            safeText(session == null ? null : session.getCourseId()),
            safeText(session == null ? null : session.getChapterId())
        );
    }

    private String resolveTopic(PlanningContext context) {
        if (context == null) {
            return "";
        }
        if (!context.topicName().isBlank()) {
            return context.topicName();
        }
        if (!context.chapterName().isBlank()) {
            return context.chapterName();
        }
        return context.learningGoal();
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
                    sectionLabel,
                    readSignalTargets(item.path("signalTargets")),
                    readOptionSignalMapping(item.path("optionSignalMapping"))
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

    private List<String> readSignalTargets(JsonNode signalTargetsNode) {
        if (signalTargetsNode == null || !signalTargetsNode.isArray() || signalTargetsNode.isEmpty()) {
            return List.of();
        }
        List<String> targets = new ArrayList<>();
        for (JsonNode targetNode : signalTargetsNode) {
            String target = textOrDefault(targetNode.asText(""), "");
            if (!target.isBlank()) {
                targets.add(target);
            }
        }
        return targets;
    }

    private Map<String, List<DiagnosisSignal>> readOptionSignalMapping(JsonNode mappingNode) {
        if (mappingNode == null || !mappingNode.isObject()) {
            return Map.of();
        }
        Map<String, List<DiagnosisSignal>> mapping = new LinkedHashMap<>();
        mappingNode.fields().forEachRemaining(entry -> {
            String optionCode = normalizeOptionCodeForSignalMapping(entry.getKey());
            List<DiagnosisSignal> signals = readSignals(entry.getValue());
            if (!optionCode.isBlank() && !signals.isEmpty()) {
                mapping.put(optionCode, signals);
            }
        });
        return mapping;
    }

    private List<DiagnosisSignal> readSignals(JsonNode signalsNode) {
        if (signalsNode == null || !signalsNode.isArray() || signalsNode.isEmpty()) {
            return List.of();
        }
        List<DiagnosisSignal> signals = new ArrayList<>();
        for (JsonNode signalNode : signalsNode) {
            String featureKey = textOrDefault(signalNode.path("featureKey").asText(""), "");
            String featureValue = textOrDefault(signalNode.path("featureValue").asText(""), "");
            if (featureKey.isBlank() || featureValue.isBlank()) {
                continue;
            }
            double scoreDelta = signalNode.path("scoreDelta").asDouble(0.0);
            double confidence = signalNode.path("confidence").asDouble(0.0);
            String evidence = textOrDefault(signalNode.path("evidence").asText(""), "");
            signals.add(new DiagnosisSignal(featureKey, featureValue, scoreDelta, confidence, evidence));
        }
        return signals;
    }

    private String normalizeOptionCodeForSignalMapping(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
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
            item.put("signalTargets", question.signalTargets());
            item.put("optionSignalMapping", question.optionSignalMapping());
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isDiagnosisCopyStrictMode() {
        return llmProperties.getFailurePolicy() != null
            && llmProperties.getFailurePolicy().isDiagnosisCopyStrict();
    }

    private boolean isCapabilitySummaryStrictMode() {
        return llmProperties.getFailurePolicy() != null
            && llmProperties.getFailurePolicy().isCapabilitySummaryStrict();
    }

    private String resolveFailureReason(Exception ex, String fallbackReason) {
        if (ex instanceof AiGenerationException aiEx) {
            return aiEx.getReason();
        }
        return fallbackReason;
    }

    private CapabilityProfileSummaryCopy buildRuleBasedSummaryCopy(
        CapabilityProfileDraft draft,
        Map<DiagnosisDimension, List<String>> answersByDimension
    ) {
        String level = draft.currentLevel() == null ? "当前阶段" : draft.currentLevel().name();
        String primaryStrength = firstOrDefault(draft.strengths(), "有可持续推进的基础能力");
        String primaryWeakness = firstOrDefault(draft.weaknesses(), "仍有关键环节需要补强");
        int answerCount = answersByDimension.values().stream().mapToInt(List::size).sum();
        String summary = "当前能力水平为 " + level + "，主要优势是" + primaryStrength + "，主要待提升点是" + primaryWeakness + "。";
        String planExplanation = "建议先围绕待提升点做一轮短闭环训练，再用 1 次复盘确认是否稳定，当前共采集 " + answerCount + " 条诊断信号。";
        return new CapabilityProfileSummaryCopy(summary, planExplanation);
    }

    private String firstOrDefault(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return fallback;
    }
}
