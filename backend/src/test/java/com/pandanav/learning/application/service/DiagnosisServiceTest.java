package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.api.dto.diagnosis.GenerateDiagnosisResponse;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisAnswerRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisResponse;
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
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        LlmJsonParser llmJsonParser = new LlmJsonParser(objectMapper);
        diagnosisService = new DiagnosisService(
            sessionRepository,
            diagnosisSessionRepository,
            diagnosisAnswerRepository,
            capabilityProfileRepository,
            new DiagnosisTemplateFactory(new DiagnosisQuestionCopyFactory()),
            new DiagnosisQuestionCopyLlmService(llmGateway, llmJsonParser, mock(LlmCallLogger.class), new LlmFailureClassifier()),
            new CapabilityProfileBuilder(),
            new CapabilityProfileSummaryGenerator(),
            new CapabilityProfileSummaryLlmService(llmGateway, llmJsonParser, mock(LlmCallLogger.class), new LlmFailureClassifier()),
            objectMapper
        );
    }

    @Test
    void generateDiagnosisShouldReturnCodeLabelOptionsWhenLlmSucceeds() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession savedDiagnosis = new DiagnosisSession();
        savedDiagnosis.setId(501L);

        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenReturn(llmText("""
            {
              "questions": [
                {
                  "questionId": "q_foundation",
                  "copy": {
                    "sectionLabel": "FOUNDATION",
                    "title": "How solid is your foundation?",
                    "description": "Choose the closest option.",
                    "placeholder": "",
                    "submitHint": "This helps set the starting point."
                  },
                  "options": ["刚开始接触", "学过但还不太熟", "基础比较熟", "已经能独立应用"]
                }
              ]
            }
            """));
        when(diagnosisSessionRepository.save(any(DiagnosisSession.class))).thenAnswer(invocation -> {
            DiagnosisSession diagnosis = invocation.getArgument(0);
            diagnosis.setId(savedDiagnosis.getId());
            return diagnosis;
        });

        GenerateDiagnosisResponse response = diagnosisService.generateDiagnosis(101L, 10L);

        assertEquals(501L, response.diagnosisId());
        assertTrue(response.contentSource() != null && !response.contentSource().isBlank());
        assertEquals("BEGINNER", response.questions().get(0).options().get(0).code());
        assertEquals(4, response.questions().get(0).options().size());
    }

    @Test
    void generateDiagnosisShouldFallbackWhenLlmFails() {
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

        GenerateDiagnosisResponse response = diagnosisService.generateDiagnosis(101L, 10L);

        assertEquals(5, response.questions().size());
        assertEquals(4, response.questions().get(0).options().size());
        assertEquals(Boolean.TRUE, response.fallbackApplied());
        assertEquals("RULE_FALLBACK", response.contentSource());
    }

    @Test
    void submitDiagnosisShouldReturnCodeLabelProfileWhenLlmSucceeds() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setStatus(DiagnosisStatus.GENERATED);
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

        SubmitDiagnosisResponse response = diagnosisService.submitDiagnosis(buildSubmitRequest(), 10L);
        assertEquals("INTERMEDIATE", response.capabilityProfile().currentLevel().code());
        assertEquals("INTERVIEW", response.capabilityProfile().goalOrientation().code());
        assertEquals("PRACTICE_FIRST", response.capabilityProfile().learningPreference().code());
        assertEquals("STANDARD", response.capabilityProfile().timeBudget().code());
        assertEquals("LLM", response.contentSource());

        ArgumentCaptor<List<DiagnosisAnswer>> answersCaptor = ArgumentCaptor.forClass(List.class);
        verify(diagnosisAnswerRepository).saveAll(answersCaptor.capture());
        assertEquals(5, answersCaptor.getValue().size());
        assertTrue(response.capabilityProfile().planExplanation().contains("training"));
    }

    @Test
    void submitDiagnosisShouldFallbackWhenLlmFails() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setStatus(DiagnosisStatus.GENERATED);
        diagnosisSession.setGeneratedQuestionsJson(generatedQuestionsJson());

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(capabilityProfileRepository.findLatestBySessionId(101L)).thenReturn(Optional.empty());
        when(llmGateway.generate(any(LlmStage.class), any(LlmPrompt.class))).thenThrow(new RuntimeException("llm down"));
        when(capabilityProfileRepository.save(any(CapabilityProfile.class))).thenAnswer(invocation -> {
            CapabilityProfile profile = invocation.getArgument(0);
            profile.setId(701L);
            return profile;
        });

        SubmitDiagnosisResponse response = diagnosisService.submitDiagnosis(buildSubmitRequest(), 10L);
        assertEquals(Boolean.TRUE, response.fallbackApplied());
        assertEquals("RULE_FALLBACK", response.contentSource());
        assertTrue(response.capabilityProfile().summary().length() > 0);
    }

    @Test
    void submitDiagnosisShouldRejectUnknownQuestion() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setGeneratedQuestionsJson(generatedQuestionsJson());

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));

        SubmitDiagnosisRequest request = new SubmitDiagnosisRequest(
            501L,
            List.of(new SubmitDiagnosisAnswerRequest("q_missing", List.of("X"), null, null))
        );

        assertThrows(BadRequestException.class, () -> diagnosisService.submitDiagnosis(request, 10L));
    }

    private SubmitDiagnosisRequest buildSubmitRequest() {
        return new SubmitDiagnosisRequest(
            501L,
            List.of(
                new SubmitDiagnosisAnswerRequest("q_foundation", List.of("BASIC"), null, null),
                new SubmitDiagnosisAnswerRequest("q_experience", List.of("COURSEWORK", "EXAM_PREP"), null, null),
                new SubmitDiagnosisAnswerRequest("q_goal_style", List.of("INTERVIEW"), null, null),
                new SubmitDiagnosisAnswerRequest("q_time_budget", List.of("STANDARD"), null, null),
                new SubmitDiagnosisAnswerRequest("q_learning_preference", List.of("PRACTICE_FIRST"), null, null)
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
                "title": "Foundation",
                "description": "How solid is your base?",
                "options": [
                  {"code":"BEGINNER","label":"刚开始接触"},
                  {"code":"BASIC","label":"学过但还不太熟"},
                  {"code":"PROFICIENT","label":"基础比较熟"},
                  {"code":"ADVANCED","label":"已经能独立应用"}
                ],
                "required": true
              },
              {
                "questionId": "q_experience",
                "dimension": "EXPERIENCE",
                "type": "multiple_choice",
                "title": "Experience",
                "description": "Past experience",
                "options": [
                  {"code":"COURSEWORK","label":"上过相关课程"},
                  {"code":"ASSIGNMENTS","label":"做过作业或实验"},
                  {"code":"PROJECTS","label":"做过项目或作品"},
                  {"code":"EXAM_PREP","label":"准备过考试或面试"},
                  {"code":"NO_EXPERIENCE","label":"几乎没有相关经验"}
                ],
                "required": true
              },
              {
                "questionId": "q_goal_style",
                "dimension": "GOAL_STYLE",
                "type": "single_choice",
                "title": "Goal",
                "description": "Main goal",
                "options": [
                  {"code":"COURSE","label":"应对课程学习与作业"},
                  {"code":"EXAM","label":"准备考试或测验"},
                  {"code":"INTERVIEW","label":"准备实习或求职面试"},
                  {"code":"PROJECT","label":"完成项目或作品"}
                ],
                "required": true
              },
              {
                "questionId": "q_time_budget",
                "dimension": "TIME_BUDGET",
                "type": "single_choice",
                "title": "Time",
                "description": "Weekly time",
                "options": [
                  {"code":"LIGHT","label":"每周 1-3 小时"},
                  {"code":"STANDARD","label":"每周 4-6 小时"},
                  {"code":"INTENSIVE","label":"每周 7-10 小时"},
                  {"code":"IMMERSIVE","label":"每周 10 小时以上"}
                ],
                "required": true
              },
              {
                "questionId": "q_learning_preference",
                "dimension": "LEARNING_PREFERENCE",
                "type": "single_choice",
                "title": "Preference",
                "description": "Best learning style",
                "options": [
                  {"code":"CONCEPT_FIRST","label":"先讲清概念，再做练习"},
                  {"code":"EXAMPLE_FIRST","label":"先看例子，再总结方法"},
                  {"code":"PRACTICE_FIRST","label":"先做题，在反馈中查漏补缺"},
                  {"code":"PROJECT_DRIVEN","label":"边学边做项目，穿插补基础"}
                ],
                "required": true
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
