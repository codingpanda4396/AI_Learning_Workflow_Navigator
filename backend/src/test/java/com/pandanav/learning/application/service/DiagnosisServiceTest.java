package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionResponse;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisAnswerSubmissionDto;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionResponse;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisSession;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.domain.repository.DiagnosisAnswerRepository;
import com.pandanav.learning.domain.repository.DiagnosisSessionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.service.CapabilityProfileBuilder;
import com.pandanav.learning.domain.service.DiagnosisQuestionCopyFactory;
import com.pandanav.learning.domain.service.DiagnosisTemplateFactory;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private DiagnosisSessionRepository diagnosisSessionRepository;
    @Mock
    private DiagnosisAnswerRepository diagnosisAnswerRepository;
    @Mock
    private CapabilityProfileRepository capabilityProfileRepository;
    @Mock
    private LlmGateway llmGateway;

    private DiagnosisService diagnosisService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        LlmJsonParser llmJsonParser = new LlmJsonParser(objectMapper);
        diagnosisService = new DiagnosisService(
            sessionRepository,
            diagnosisSessionRepository,
            diagnosisAnswerRepository,
            capabilityProfileRepository,
            new DiagnosisTemplateFactory(new DiagnosisQuestionCopyFactory()),
            new DiagnosisQuestionAssembler(new DiagnosisQuestionCopyFactory()),
            new DiagnosisQuestionCopyLlmService(llmGateway, llmJsonParser, mock(LlmCallLogger.class), new QuestionStructureAssembler()),
            new DefaultDiagnosisQuestionPersonalizer(),
            new DiagnosisQuestionCopyNormalizer(),
            new DiagnosisExplanationBuilder(),
            new CapabilityProfileBuilder(),
            new DiagnosisExplanationAssembler(objectMapper),
            new CapabilityProfileSummaryLlmService(llmGateway, llmJsonParser, mock(LlmCallLogger.class)),
            objectMapper
        );
    }

    @Test
    void createDiagnosisSessionShouldReturnStructuredOptionsWhenLlmSucceeds() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession savedDiagnosis = new DiagnosisSession();
        savedDiagnosis.setId(501L);

        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenReturn(llmText("""
            {
              "questions": [
                {
                  "questionId": "q_foundation",
                  "title": "How solid is your foundation?",
                  "description": "Choose the closest option.",
                  "placeholder": "",
                  "submitHint": "This helps set the starting point.",
                  "sectionLabel": "FOUNDATION",
                  "options": [
                    {"code":"BEGINNER","label":"Starter"},
                    {"code":"BASIC","label":"Basic"},
                    {"code":"PROFICIENT","label":"Proficient"},
                    {"code":"ADVANCED","label":"Advanced"}
                  ]
                },
                {
                  "questionId": "q_experience",
                  "title": "Experience",
                  "description": "Past experience",
                  "placeholder": "",
                  "submitHint": "This helps sequence the plan.",
                  "sectionLabel": "EXPERIENCE",
                  "options": [
                    {"code":"COURSEWORK","label":"Coursework"},
                    {"code":"ASSIGNMENTS","label":"Assignments"},
                    {"code":"PROJECTS","label":"Projects"},
                    {"code":"EXAM_PREP","label":"Exam prep"},
                    {"code":"NO_EXPERIENCE","label":"No experience"}
                  ]
                },
                {
                  "questionId": "q_goal_style",
                  "title": "Goal",
                  "description": "Main goal",
                  "placeholder": "",
                  "submitHint": "This sets the plan focus.",
                  "sectionLabel": "GOAL",
                  "options": [
                    {"code":"COURSE","label":"Course"},
                    {"code":"EXAM","label":"Exam"},
                    {"code":"INTERVIEW","label":"Interview"},
                    {"code":"PROJECT","label":"Project"}
                  ]
                },
                {
                  "questionId": "q_time_budget",
                  "title": "Time",
                  "description": "Weekly time",
                  "placeholder": "",
                  "submitHint": "This affects the pace.",
                  "sectionLabel": "TIME",
                  "options": [
                    {"code":"LIGHT","label":"1-3h"},
                    {"code":"STANDARD","label":"4-6h"},
                    {"code":"INTENSIVE","label":"7-10h"},
                    {"code":"IMMERSIVE","label":"10h+"}
                  ]
                },
                {
                  "questionId": "q_difficulty_pain_point",
                  "title": "Pain point",
                  "description": "Main bottleneck",
                  "placeholder": "",
                  "submitHint": "This affects content delivery.",
                  "sectionLabel": "PAIN_POINT",
                  "options": [
                    {"code":"CONCEPT_UNDERSTANDING","label":"Concept understanding"},
                    {"code":"TRANSFER_APPLICATION","label":"Transfer application"},
                    {"code":"IMPLEMENTATION","label":"Implementation"},
                    {"code":"LONG_TERM_MEMORY","label":"Long-term memory"}
                  ]
                }
              ]
            }
            """));
        when(diagnosisSessionRepository.save(any(DiagnosisSession.class))).thenAnswer(invocation -> {
            DiagnosisSession diagnosis = invocation.getArgument(0);
            diagnosis.setId(savedDiagnosis.getId());
            return diagnosis;
        });

        CreateDiagnosisSessionResponse response = diagnosisService.createDiagnosisSession(101L, 10L);

        assertEquals(501L, response.diagnosisId());
        assertEquals("READY", response.status());
        assertEquals("LLM", response.generationMode());
        assertTrue(response.diagnosisExplanation().whyTheseQuestions().contains("系统需要了解"));
        assertEquals(3, response.decisionHints().planningFactors().size());
        assertEquals("BEGINNER", response.questions().get(0).options().get(0).code());
        assertEquals(4, response.questions().get(0).options().size());
        assertEquals("/plan", response.nextAction().target().route());
    }

    @Test
    void createDiagnosisSessionShouldFallbackWhenLlmFails() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession savedDiagnosis = new DiagnosisSession();
        savedDiagnosis.setId(501L);

        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenThrow(new RuntimeException("llm down"));
        when(diagnosisSessionRepository.save(any(DiagnosisSession.class))).thenAnswer(invocation -> {
            DiagnosisSession diagnosis = invocation.getArgument(0);
            diagnosis.setId(savedDiagnosis.getId());
            return diagnosis;
        });

        CreateDiagnosisSessionResponse response = diagnosisService.createDiagnosisSession(101L, 10L);
        assertEquals("READY", response.status());
        assertEquals("RULE_FALLBACK", response.generationMode());
        assertTrue(response.fallback().applied());
        assertEquals("RULE_FALLBACK", response.fallback().contentSource());
    }

    @Test
    void submitDiagnosisSessionShouldReturnStructuredProfileWhenLlmSucceeds() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setStatus(DiagnosisStatus.READY);
        diagnosisSession.setGeneratedQuestionsJson(generatedQuestionsJson());

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(capabilityProfileRepository.findLatestBySessionId(101L)).thenReturn(Optional.empty());
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenReturn(llmText("""
            {
              "summary": "Learner has a reasonable base and can keep moving with support.",
              "planExplanation": "Start by reinforcing foundation and then increase training volume."
            }
            """));
        when(capabilityProfileRepository.save(any(CapabilityProfile.class))).thenAnswer(invocation -> {
            CapabilityProfile profile = invocation.getArgument(0);
            profile.setId(701L);
            return profile;
        });

        SubmitDiagnosisSessionResponse response = diagnosisService.submitDiagnosisSession(501L, buildSubmitRequest(), 10L);
        assertEquals("EVALUATED", response.status());
        assertEquals("INTERMEDIATE", response.capabilityProfile().currentLevel().code());
        assertEquals("INTERVIEW", response.capabilityProfile().goalOrientation().code());
        assertEquals("PRACTICE_FIRST", response.capabilityProfile().learningPreference().code());
        assertEquals("STANDARD", response.capabilityProfile().timeBudget().code());
        assertEquals("LLM", response.fallback().contentSource());
        assertTrue(response.insights().planExplanation().contains("training"));
        assertEquals(5, response.reasoningSteps().size());
        assertTrue(response.strengthSources().size() >= 1);
        assertTrue(response.weaknessSources().size() >= 1);

        ArgumentCaptor<List<DiagnosisAnswer>> answersCaptor = ArgumentCaptor.forClass(List.class);
        verify(diagnosisAnswerRepository).saveAll(answersCaptor.capture());
        assertEquals(5, answersCaptor.getValue().size());
        assertEquals("BASIC", answersCaptor.getValue().get(0).getAnswerValueJson().replace("\"", ""));
    }

    @Test
    void submitDiagnosisSessionShouldFailWhenLlmFails() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setStatus(DiagnosisStatus.READY);
        diagnosisSession.setGeneratedQuestionsJson(generatedQuestionsJson());

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenThrow(new RuntimeException("llm down"));

        AiGenerationException ex = assertThrows(
            AiGenerationException.class,
            () -> diagnosisService.submitDiagnosisSession(501L, buildSubmitRequest(), 10L)
        );
        assertEquals("CAPABILITY_PROFILE_SUMMARY", ex.getStage());
    }

    @Test
    void submitDiagnosisSessionShouldRejectUnknownQuestion() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setGeneratedQuestionsJson(generatedQuestionsJson());

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));

        SubmitDiagnosisSessionRequest request = new SubmitDiagnosisSessionRequest(
            501L,
            List.of(new DiagnosisAnswerSubmissionDto("q_missing", "X", null, null, null))
        );

        assertThrows(BadRequestException.class, () -> diagnosisService.submitDiagnosisSession(501L, request, 10L));
    }

    private SubmitDiagnosisSessionRequest buildSubmitRequest() {
        return new SubmitDiagnosisSessionRequest(
            501L,
            List.of(
                new DiagnosisAnswerSubmissionDto("q_foundation", "BASIC", null, null, null),
                new DiagnosisAnswerSubmissionDto("q_experience", null, List.of("COURSEWORK", "EXAM_PREP"), null, null),
                new DiagnosisAnswerSubmissionDto("q_goal_style", "INTERVIEW", null, null, null),
                new DiagnosisAnswerSubmissionDto("q_time_budget", "STANDARD", null, null, null),
                new DiagnosisAnswerSubmissionDto("q_learning_preference", "PRACTICE_FIRST", null, null, null)
            )
        );
    }

    private String generatedQuestionsJson() {
        return """
            [
              {
                "questionId": "q_foundation",
                "dimension": "FOUNDATION",
                "type": "single_choice",
                "required": true,
                "title": "Foundation",
                "description": "How solid is your base?",
                "placeholder": "",
                "submitHint": "",
                "sectionLabel": "FOUNDATION",
                "options": [
                  {"code":"BEGINNER","label":"Beginner","order":1},
                  {"code":"BASIC","label":"Basic","order":2},
                  {"code":"PROFICIENT","label":"Proficient","order":3},
                  {"code":"ADVANCED","label":"Advanced","order":4}
                ]
              },
              {
                "questionId": "q_experience",
                "dimension": "EXPERIENCE",
                "type": "multiple_choice",
                "required": true,
                "title": "Experience",
                "description": "Past experience",
                "placeholder": "",
                "submitHint": "",
                "sectionLabel": "EXPERIENCE",
                "options": [
                  {"code":"COURSEWORK","label":"Coursework","order":1},
                  {"code":"ASSIGNMENTS","label":"Assignments","order":2},
                  {"code":"PROJECTS","label":"Projects","order":3},
                  {"code":"EXAM_PREP","label":"Exam prep","order":4},
                  {"code":"NO_EXPERIENCE","label":"No experience","order":5}
                ]
              },
              {
                "questionId": "q_goal_style",
                "dimension": "GOAL_STYLE",
                "type": "single_choice",
                "required": true,
                "title": "Goal",
                "description": "Main goal",
                "placeholder": "",
                "submitHint": "",
                "sectionLabel": "GOAL",
                "options": [
                  {"code":"COURSE","label":"Course","order":1},
                  {"code":"EXAM","label":"Exam","order":2},
                  {"code":"INTERVIEW","label":"Interview","order":3},
                  {"code":"PROJECT","label":"Project","order":4}
                ]
              },
              {
                "questionId": "q_time_budget",
                "dimension": "TIME_BUDGET",
                "type": "single_choice",
                "required": true,
                "title": "Time",
                "description": "Weekly time",
                "placeholder": "",
                "submitHint": "",
                "sectionLabel": "TIME",
                "options": [
                  {"code":"LIGHT","label":"1-3h","order":1},
                  {"code":"STANDARD","label":"4-6h","order":2},
                  {"code":"INTENSIVE","label":"7-10h","order":3},
                  {"code":"IMMERSIVE","label":"10h+","order":4}
                ]
              },
              {
                "questionId": "q_learning_preference",
                "dimension": "LEARNING_PREFERENCE",
                "type": "single_choice",
                "required": true,
                "title": "Preference",
                "description": "Best learning style",
                "placeholder": "",
                "submitHint": "",
                "sectionLabel": "PREFERENCE",
                "options": [
                  {"code":"CONCEPT_FIRST","label":"Concept first","order":1},
                  {"code":"EXAMPLE_FIRST","label":"Example first","order":2},
                  {"code":"PRACTICE_FIRST","label":"Practice first","order":3},
                  {"code":"PROJECT_DRIVEN","label":"Project driven","order":4}
                ]
              }
            ]
            """;
    }

    private LlmTextResult llmText(String text) {
        return new LlmTextResult(text, "mock", "mock-model", null, null, new ObjectNode(JsonNodeFactory.instance), new ObjectNode(JsonNodeFactory.instance));
    }

    private LearningSession learningSession(Long sessionId, Long userId) {
        LearningSession session = new LearningSession();
        session.setId(sessionId);
        session.setUserPk(userId);
        session.setGoalText("Prepare for interview");
        session.setChapterId("trees");
        session.setCourseId("data-structures");
        return session;
    }
}
