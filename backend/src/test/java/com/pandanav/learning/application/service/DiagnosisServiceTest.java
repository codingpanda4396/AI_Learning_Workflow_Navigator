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
    void generateDiagnosisShouldReturnPersonalizedCopyWhenLlmSucceeds() {
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
                    "sectionLabel": "KNOWLEDGE_FOUNDATION",
                    "title": "你现在对链表掌握到什么程度？",
                    "description": "按真实感觉来选就可以，不用担心答错。",
                    "placeholder": "",
                    "submitHint": "系统会据此判断起点。"
                  },
                  "options": ["刚开始接触", "学过但还不太稳", "基础比较稳", "已经能独立应用"]
                },
                {
                  "questionId": "q_experience",
                  "copy": {
                    "sectionLabel": "PAST_EXPERIENCE",
                    "title": "你之前和链表相关的经历有哪些？",
                    "description": "可多选，按真实经历勾选就好。",
                    "placeholder": "",
                    "submitHint": "这些经历会帮助系统安排切入方式。"
                  },
                  "options": ["上过相关课程", "做过作业或实验", "做过项目或作品", "准备过考试或面试", "几乎没有相关经验"]
                },
                {
                  "questionId": "q_goal_style",
                  "copy": {
                    "sectionLabel": "GOAL_ORIENTATION",
                    "title": "这次你最想先解决什么目标？",
                    "description": "选最主要的一项，方便安排后续重点。",
                    "placeholder": "",
                    "submitHint": "系统会根据目标调整路径。"
                  },
                  "options": ["应对课程学习与作业", "准备考试或测验", "准备实习或求职面试", "完成项目或作品"]
                },
                {
                  "questionId": "q_time_budget",
                  "copy": {
                    "sectionLabel": "TIME_BUDGET",
                    "title": "你每周大概能留出多少时间？",
                    "description": "按现实中能稳定坚持的节奏来选。",
                    "placeholder": "",
                    "submitHint": "系统会尽量控制任务节奏。"
                  },
                  "options": ["每周 1-3 小时", "每周 4-6 小时", "每周 7-10 小时", "每周 10 小时以上"]
                },
                {
                  "questionId": "q_learning_preference",
                  "copy": {
                    "sectionLabel": "LEARNING_PREFERENCE",
                    "title": "你更适合哪种学习方式？",
                    "description": "想想你平时最容易进入状态的方式。",
                    "placeholder": "比如先看总结再做题。",
                    "submitHint": "这会影响讲解和练习的组织方式。"
                  },
                  "options": ["先讲清概念，再做练习", "先看例子，再总结方法", "先做题，在反馈中查漏补缺", "边学边做项目，穿插补基础"]
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
        assertEquals(5, response.questions().size());
        assertEquals("你现在对链表掌握到什么程度？", response.questions().get(0).copy().title());
        assertEquals(4, response.questions().get(0).options().size());
        assertEquals(Boolean.FALSE, response.fallbackApplied());
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
        assertTrue(response.questions().get(0).copy().title().contains("掌握程度"));
        assertEquals(4, response.questions().get(0).options().size());
        assertEquals(Boolean.TRUE, response.fallbackApplied());
    }

    @Test
    void submitDiagnosisShouldReturnSummaryAndPlanExplanationWhenLlmSucceeds() {
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
              "summary": "从这轮诊断来看，你已经有一定基础，更适合边巩固边推进，目标也比较明确。",
              "planExplanation": "系统接下来会先帮你梳理关键概念，再逐步增加训练量，并尽量贴合你每周可投入的时间。"
            }
            """));
        when(capabilityProfileRepository.save(any(CapabilityProfile.class))).thenAnswer(invocation -> {
            CapabilityProfile profile = invocation.getArgument(0);
            profile.setId(701L);
            return profile;
        });

        SubmitDiagnosisResponse response = diagnosisService.submitDiagnosis(buildSubmitRequest(), 10L);
        assertEquals(Boolean.FALSE, response.fallbackApplied());
        assertEquals("LLM", response.contentSource());

        ArgumentCaptor<List<DiagnosisAnswer>> answersCaptor = ArgumentCaptor.forClass(List.class);
        verify(diagnosisAnswerRepository).saveAll(answersCaptor.capture());
        assertEquals(5, answersCaptor.getValue().size());
        assertEquals("INTERMEDIATE", response.capabilityProfile().currentLevel());
        assertEquals("INTERVIEW", response.capabilityProfile().goalOrientation());
        assertEquals("PRACTICE_FIRST", response.capabilityProfile().learningPreference());
        assertEquals(Boolean.FALSE, response.fallbackApplied());
        assertEquals("LLM", response.contentSource());
        assertTrue(response.capabilityProfile().summary().contains("已经有一定基础"));
        assertTrue(response.capabilityProfile().planExplanation().contains("逐步增加训练量"));
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

        assertTrue(response.capabilityProfile().summary().contains("尽量贴合你的当前基础和节奏"));
        assertTrue(response.capabilityProfile().planExplanation().contains("逐步调整训练难度"));
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
            List.of(new SubmitDiagnosisAnswerRequest("q_missing", JsonNodeFactory.instance.textNode("x")))
        );

        assertThrows(BadRequestException.class, () -> diagnosisService.submitDiagnosis(request, 10L));
    }

    private SubmitDiagnosisRequest buildSubmitRequest() {
        return new SubmitDiagnosisRequest(
            501L,
            List.of(
                new SubmitDiagnosisAnswerRequest("q_foundation", JsonNodeFactory.instance.textNode("学过但还不太稳")),
                new SubmitDiagnosisAnswerRequest("q_experience", objectMapper.valueToTree(List.of("上过相关课程", "准备过考试或面试"))),
                new SubmitDiagnosisAnswerRequest("q_goal_style", JsonNodeFactory.instance.textNode("准备实习或求职面试")),
                new SubmitDiagnosisAnswerRequest("q_time_budget", JsonNodeFactory.instance.textNode("每周 4-6 小时")),
                new SubmitDiagnosisAnswerRequest("q_learning_preference", JsonNodeFactory.instance.textNode("先做题，在反馈中查漏补缺"))
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
                "title": "你觉得自己目前对这部分内容的掌握程度如何？",
                "description": "按你现在的真实情况作答即可，这不是考试。",
                "options": ["刚开始接触", "学过但还不太稳", "基础比较稳", "已经能独立应用"],
                "required": true,
                "copy": {
                  "sectionLabel": "KNOWLEDGE_FOUNDATION",
                  "title": "你觉得自己目前对这部分内容的掌握程度如何？",
                  "description": "按你现在的真实情况作答即可，这不是考试。",
                  "placeholder": "",
                  "submitHint": "你的回答会帮助系统判断起点和后续安排。"
                }
              },
              {
                "questionId": "q_experience",
                "dimension": "EXPERIENCE",
                "type": "multiple_choice",
                "title": "你之前有过哪些相关学习或实践经历？",
                "description": "可多选，我们会据此判断更适合从讲解入手还是从训练入手。",
                "options": ["上过相关课程", "做过作业或实验", "做过项目或作品", "准备过考试或面试", "几乎没有相关经验"],
                "required": true,
                "copy": {
                  "sectionLabel": "PAST_EXPERIENCE",
                  "title": "你之前有过哪些相关学习或实践经历？",
                  "description": "可多选，我们会据此判断更适合从讲解入手还是从训练入手。",
                  "placeholder": "",
                  "submitHint": "这些经历会帮助系统判断更适合的切入方式。"
                }
              },
              {
                "questionId": "q_goal_style",
                "dimension": "GOAL_STYLE",
                "type": "single_choice",
                "title": "这次学习你最想优先解决哪类目标？",
                "description": "选最主要的一项，系统会据此调整后续路径的侧重点。",
                "options": ["应对课程学习与作业", "准备考试或测验", "准备实习或求职面试", "完成项目或作品"],
                "required": true,
                "copy": {
                  "sectionLabel": "GOAL_ORIENTATION",
                  "title": "这次学习你最想优先解决哪类目标？",
                  "description": "选最主要的一项，系统会据此调整后续路径的侧重点。",
                  "placeholder": "",
                  "submitHint": "系统会根据你的目标调整后续路径侧重点。"
                }
              },
              {
                "questionId": "q_time_budget",
                "dimension": "TIME_BUDGET",
                "type": "single_choice",
                "title": "你每周大概能为这个目标投入多少时间？",
                "description": "不用特别精确，按你现实中能稳定执行的节奏选择即可。",
                "options": ["每周 1-3 小时", "每周 4-6 小时", "每周 7-10 小时", "每周 10 小时以上"],
                "required": true,
                "copy": {
                  "sectionLabel": "TIME_BUDGET",
                  "title": "你每周大概能为这个目标投入多少时间？",
                  "description": "不用特别精确，按你现实中能稳定执行的节奏选择即可。",
                  "placeholder": "",
                  "submitHint": "系统会尽量把安排控制在你能坚持的节奏里。"
                }
              },
              {
                "questionId": "q_learning_preference",
                "dimension": "LEARNING_PREFERENCE",
                "type": "single_choice",
                "title": "你平时更适合哪种学习方式？",
                "description": "选你最容易坚持、也最容易进入状态的一种。",
                "options": ["先讲清概念，再做练习", "先看例子，再总结方法", "先做题，在反馈中查漏补缺", "边学边做项目，穿插补基础"],
                "required": true,
                "copy": {
                  "sectionLabel": "LEARNING_PREFERENCE",
                  "title": "你平时更适合哪种学习方式？",
                  "description": "选你最容易坚持、也最容易进入状态的一种。",
                  "placeholder": "你可以简单描述自己更容易进入状态的方式。",
                  "submitHint": "这会影响系统讲解和练习的组织方式。"
                }
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
        session.setGoalText("准备算法面试");
        session.setChapterId("链表");
        session.setCourseId("数据结构");
        return session;
    }
}
