package com.pandanav.learning.application.service.pathplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.enums.PlanMode;
import com.pandanav.learning.domain.enums.PlanSource;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalizedPathPlannerServiceTest {

    @Test
    void shouldApplyTrainingFeedbackInRuleMode() {
        ConceptNodeRepository conceptNodeRepository = mock(ConceptNodeRepository.class);
        MasteryRepository masteryRepository = mock(MasteryRepository.class);
        NodeMasteryRepository nodeMasteryRepository = mock(NodeMasteryRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        LearningEventRepository learningEventRepository = mock(LearningEventRepository.class);
        LlmGateway llmGateway = mock(LlmGateway.class);
        PromptTemplateProvider promptTemplateProvider = mock(PromptTemplateProvider.class);

        LearningSession session = buildSession();
        List<ConceptNode> chapterNodes = List.of(node(101L, 1), node(102L, 2));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch-1")).thenReturn(chapterNodes);
        when(masteryRepository.findByUserIdAndChapterId("u-1", "ch-1")).thenReturn(List.of(mastery(101L, "0.90"), mastery(102L, "0.30")));
        when(taskRepository.findRecentTrainingAttempts(1L, 6)).thenReturn(List.of(
            new TrainingAttemptSummary(11L, 102L, 42, List.of("MISSING_STEPS"))
        ));

        PersonalizedPathPlannerService service = new PersonalizedPathPlannerService(
            conceptNodeRepository,
            masteryRepository,
            nodeMasteryRepository,
            taskRepository,
            learningEventRepository,
            llmGateway,
            promptTemplateProvider,
            new PromptOutputValidator(),
            new LlmJsonParser(new ObjectMapper()),
            readyLlmProperties(),
            new ObjectMapper()
        );

        PersonalizedPlanResult result = service.plan(session, PlanMode.RULE, false);

        assertEquals(PlanSource.RULE, result.source());
        assertEquals(List.of(102L), result.advancedNodeIds());
        assertFalse(result.insertedTasks().isEmpty());
        assertEquals("UNDERSTANDING", result.insertedTasks().get(0).stage());
        assertEquals("MISSING_STEPS", result.insertedTasks().get(0).trigger());
        verify(llmGateway, never()).generate(any());
    }

    @Test
    void shouldFallbackToRulePlanWhenLlmBusinessValidationFails() {
        ConceptNodeRepository conceptNodeRepository = mock(ConceptNodeRepository.class);
        MasteryRepository masteryRepository = mock(MasteryRepository.class);
        NodeMasteryRepository nodeMasteryRepository = mock(NodeMasteryRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        LearningEventRepository learningEventRepository = mock(LearningEventRepository.class);
        LlmGateway llmGateway = mock(LlmGateway.class);
        PromptTemplateProvider promptTemplateProvider = mock(PromptTemplateProvider.class);

        LearningSession session = buildSession();
        List<ConceptNode> chapterNodes = List.of(node(101L, 1), node(102L, 2));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch-1")).thenReturn(chapterNodes);
        when(masteryRepository.findByUserIdAndChapterId("u-1", "ch-1")).thenReturn(List.of(mastery(101L, "0.90"), mastery(102L, "0.30")));
        when(taskRepository.findRecentTrainingAttempts(1L, 6)).thenReturn(List.of(
            new TrainingAttemptSummary(11L, 102L, 42, List.of("MISSING_STEPS"))
        ));
        when(promptTemplateProvider.buildPersonalizedPathPlanPrompt(any())).thenReturn(
            new LlmPrompt(PromptTemplateKey.PATH_PLAN_V1, "PATH_PLAN", "v1", LlmInvocationProfile.HEAVY_REASONING_TASK, "sys", "user", "{}", "", null, null)
        );
        when(llmGateway.generate(any())).thenReturn(new LlmTextResult(
            """
            {
              "ordered_nodes": [
                {"node_id": 101, "priority": 1, "reason": "Keep original order."},
                {"node_id": 102, "priority": 2, "reason": "Then review weak node."}
              ],
              "inserted_tasks": [
                {
                  "node_id": 102,
                  "stage": "REFLECTION",
                  "objective": "Provide extra reflective notes for this weak concept.",
                  "trigger": "CONCEPT_CONFUSION"
                }
              ],
              "plan_reasoning_summary": "Use a simple order and add one extra inserted task for confusion handling.",
              "risk_flags": ["LOW_CONFIDENCE_DIAGNOSIS"]
            }
            """,
            "provider",
            "model",
            LlmInvocationProfile.HEAVY_REASONING_TASK,
            null,
            null,
            null
        ));

        PersonalizedPathPlannerService service = new PersonalizedPathPlannerService(
            conceptNodeRepository,
            masteryRepository,
            nodeMasteryRepository,
            taskRepository,
            learningEventRepository,
            llmGateway,
            promptTemplateProvider,
            new PromptOutputValidator(),
            new LlmJsonParser(new ObjectMapper()),
            readyLlmProperties(),
            new ObjectMapper()
        );

        PersonalizedPlanResult result = service.plan(session, PlanMode.LLM, false);

        assertEquals(PlanSource.RULE, result.source());
        assertTrue(result.fallbackApplied());
        assertTrue(result.riskFlags().contains("LLM_PLAN_FALLBACK"));
        assertTrue(result.validationErrors().stream().anyMatch(v -> v.contains("inserted_tasks stage must be UNDERSTANDING or TRAINING")));
        assertEquals(List.of(102L), result.advancedNodeIds());
    }

    private static LearningSession buildSession() {
        LearningSession session = new LearningSession();
        session.setId(1L);
        session.setUserId("u-1");
        session.setUserPk(null);
        session.setChapterId("ch-1");
        session.setGoalText("Improve concept mastery in one week");
        return session;
    }

    private static ConceptNode node(Long id, int orderNo) {
        ConceptNode node = new ConceptNode();
        node.setId(id);
        node.setOrderNo(orderNo);
        node.setName("Node-" + id);
        return node;
    }

    private static Mastery mastery(Long nodeId, String value) {
        Mastery mastery = new Mastery();
        mastery.setNodeId(nodeId);
        mastery.setMasteryValue(new BigDecimal(value));
        return mastery;
    }

    private static LlmProperties readyLlmProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("k");
        properties.setModel("m");
        return properties;
    }
}
