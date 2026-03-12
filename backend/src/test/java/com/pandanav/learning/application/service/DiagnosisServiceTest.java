package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.api.dto.diagnosis.GenerateDiagnosisResponse;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisAnswerRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisResponse;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.enums.DiagnosisStatus;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
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
import com.pandanav.learning.domain.service.DiagnosisTemplateFactory;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
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
        diagnosisService = new DiagnosisService(
            sessionRepository,
            diagnosisSessionRepository,
            diagnosisAnswerRepository,
            capabilityProfileRepository,
            new DiagnosisTemplateFactory(),
            new CapabilityProfileBuilder(),
            llmGateway,
            new LlmJsonParser(objectMapper),
            objectMapper
        );
    }

    @Test
    void generateDiagnosisShouldPersistQuestions() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession savedDiagnosis = new DiagnosisSession();
        savedDiagnosis.setId(501L);

        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmPrompt.class))).thenReturn(llmText("""
            {"questions":[
              {"question_id":"q_foundation","title":"你目前和目标的距离大概在哪一档？","description":"结合这次目标来判断。","options":["刚开始接触，需要从基础概念学起","了解过核心概念，但做题或应用还不稳定","已经比较熟悉，希望更快进入综合应用"]},
              {"question_id":"q_experience","title":"你以前接触过哪些相关经历？","description":"可多选。","options":["上过相关课程","做过课程作业或实验","做过项目或作品","准备过考试或面试","几乎没有相关经验"]},
              {"question_id":"q_goal_style","title":"你这次最想优先解决什么问题？","description":"选最主要的一项。","options":["应对课程学习与作业","准备考试或测验","准备实习或求职面试","完成项目或作品"]},
              {"question_id":"q_time_budget","title":"你一周通常能拿出多少时间？","description":"按常见情况选择即可。","options":["每周 1-3 小时","每周 4-6 小时","每周 7-10 小时","每周 10 小时以上"]},
              {"question_id":"q_learning_preference","title":"你更容易进入状态的学习方式是什么？","description":"选最适合自己的方式。","options":["先讲清概念，再做练习","先看例子，再总结方法","先做题，在反馈中查漏补缺","边学边做项目，穿插补基础"]}
            ]}
            """));
        when(diagnosisSessionRepository.save(any(DiagnosisSession.class))).thenAnswer(invocation -> {
            DiagnosisSession diagnosis = invocation.getArgument(0);
            diagnosis.setId(savedDiagnosis.getId());
            return diagnosis;
        });

        GenerateDiagnosisResponse response = diagnosisService.generateDiagnosis(101L, 10L);

        assertEquals(501L, response.diagnosisId());
        assertEquals(5, response.questions().size());
        assertEquals("FOUNDATION", response.questions().get(0).dimension());
        verify(diagnosisSessionRepository).save(any(DiagnosisSession.class));
    }

    @Test
    void submitDiagnosisShouldPersistAnswersAndProfile() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setStatus(DiagnosisStatus.GENERATED);
        diagnosisSession.setGeneratedQuestionsJson("""
            [{"questionId":"q_foundation","dimension":"FOUNDATION","type":"single_choice","title":"t1","description":"d1","options":["刚开始接触，需要从基础概念学起","了解过核心概念，但做题或应用还不稳定","已经比较熟悉，希望更快进入综合应用"],"required":true},
             {"questionId":"q_experience","dimension":"EXPERIENCE","type":"multiple_choice","title":"t2","description":"d2","options":["上过相关课程","做过课程作业或实验","做过项目或作品","准备过考试或面试","几乎没有相关经验"],"required":true},
             {"questionId":"q_goal_style","dimension":"GOAL_STYLE","type":"single_choice","title":"t3","description":"d3","options":["应对课程学习与作业","准备考试或测验","准备实习或求职面试","完成项目或作品"],"required":true},
             {"questionId":"q_time_budget","dimension":"TIME_BUDGET","type":"single_choice","title":"t4","description":"d4","options":["每周 1-3 小时","每周 4-6 小时","每周 7-10 小时","每周 10 小时以上"],"required":true},
             {"questionId":"q_learning_preference","dimension":"LEARNING_PREFERENCE","type":"single_choice","title":"t5","description":"d5","options":["先讲清概念，再做练习","先看例子，再总结方法","先做题，在反馈中查漏补缺","边学边做项目，穿插补基础"],"required":true}]
            """);

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(capabilityProfileRepository.findLatestBySessionId(101L)).thenReturn(Optional.empty());
        when(llmGateway.generate(any(LlmPrompt.class))).thenReturn(llmText("{\"summary\":\"你有一定基础，目标偏面试导向，适合训练优先的学习安排。\"}"));
        when(capabilityProfileRepository.save(any(CapabilityProfile.class))).thenAnswer(invocation -> {
            CapabilityProfile profile = invocation.getArgument(0);
            profile.setId(701L);
            return profile;
        });

        SubmitDiagnosisRequest request = new SubmitDiagnosisRequest(
            501L,
            List.of(
                new SubmitDiagnosisAnswerRequest("q_foundation", JsonNodeFactory.instance.textNode("了解过核心概念，但做题或应用还不稳定")),
                new SubmitDiagnosisAnswerRequest("q_experience", objectMapper.valueToTree(List.of("上过相关课程", "准备过考试或面试"))),
                new SubmitDiagnosisAnswerRequest("q_goal_style", JsonNodeFactory.instance.textNode("准备实习或求职面试")),
                new SubmitDiagnosisAnswerRequest("q_time_budget", JsonNodeFactory.instance.textNode("每周 4-6 小时")),
                new SubmitDiagnosisAnswerRequest("q_learning_preference", JsonNodeFactory.instance.textNode("先做题，在反馈中查漏补缺"))
            )
        );

        SubmitDiagnosisResponse response = diagnosisService.submitDiagnosis(request, 10L);

        ArgumentCaptor<List<DiagnosisAnswer>> answersCaptor = ArgumentCaptor.forClass(List.class);
        verify(diagnosisAnswerRepository).saveAll(answersCaptor.capture());
        verify(capabilityProfileRepository).save(any(CapabilityProfile.class));
        assertEquals(5, answersCaptor.getValue().size());
        assertEquals("INTERMEDIATE", response.capabilityProfile().currentLevel());
        assertEquals("INTERVIEW", response.capabilityProfile().goalOrientation());
        assertEquals("PRACTICE_FIRST", response.capabilityProfile().learningPreference());
        assertEquals("PATH_PLAN", response.nextAction().type());
    }

    @Test
    void getSummaryShouldFallbackWhenLlmFails() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession savedDiagnosis = new DiagnosisSession();
        savedDiagnosis.setId(501L);

        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));
        when(llmGateway.generate(any(LlmPrompt.class))).thenThrow(new InternalServerException("llm down"));
        when(diagnosisSessionRepository.save(any(DiagnosisSession.class))).thenAnswer(invocation -> {
            DiagnosisSession diagnosis = invocation.getArgument(0);
            diagnosis.setId(savedDiagnosis.getId());
            return diagnosis;
        });

        GenerateDiagnosisResponse response = diagnosisService.generateDiagnosis(101L, 10L);

        assertEquals(5, response.questions().size());
        assertTrue(response.questions().get(0).title().contains("基础"));
    }

    @Test
    void submitDiagnosisShouldRejectUnknownQuestion() {
        LearningSession session = learningSession(101L, 10L);
        DiagnosisSession diagnosisSession = new DiagnosisSession();
        diagnosisSession.setId(501L);
        diagnosisSession.setLearningSessionId(101L);
        diagnosisSession.setUserPk(10L);
        diagnosisSession.setGeneratedQuestionsJson("""
            [{"questionId":"q_foundation","dimension":"FOUNDATION","type":"single_choice","title":"t1","description":"d1","options":["刚开始接触，需要从基础概念学起","了解过核心概念，但做题或应用还不稳定","已经比较熟悉，希望更快进入综合应用"],"required":true}]
            """);

        when(diagnosisSessionRepository.findByIdAndUserPk(501L, 10L)).thenReturn(Optional.of(diagnosisSession));
        when(sessionRepository.findByIdAndUserPk(101L, 10L)).thenReturn(Optional.of(session));

        SubmitDiagnosisRequest request = new SubmitDiagnosisRequest(
            501L,
            List.of(new SubmitDiagnosisAnswerRequest("q_missing", JsonNodeFactory.instance.textNode("x")))
        );

        assertThrows(BadRequestException.class, () -> diagnosisService.submitDiagnosis(request, 10L));
    }

    private LlmTextResult llmText(String text) {
        return new LlmTextResult(text, "mock", "mock-model", null, null, new ObjectNode(JsonNodeFactory.instance), new ObjectNode(JsonNodeFactory.instance));
    }

    private LearningSession learningSession(Long sessionId, Long userId) {
        LearningSession session = new LearningSession();
        session.setId(sessionId);
        session.setUserPk(userId);
        session.setGoalText("准备算法面试");
        session.setChapterId("algo");
        session.setCourseId("cs101");
        return session;
    }
}
