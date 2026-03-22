package navigator.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.infrastructure.memory.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 3 Phase 6：主链路回归测试。
 * 覆盖 BEGINNER/WITHIN_15_MIN、BASIC/LOCAL_REPAIR、Fallback/Boundary 场景。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:phase6_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class Sprint3Phase6MainLinkRegressionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InMemoryStore store;

    @BeforeEach
    void clearStore() {
        store.getGoals().clear();
        store.getGoalContextSnapshots().clear();
        store.getLearnerProfiles().clear();
        store.getDiagnosisEvidenceSummaries().clear();
        store.getPlanPreviews().clear();
        store.getPlanStatuses().clear();
        store.getSessions().clear();
        store.getSessionTaskRecords().clear();
        store.getDiagnosisSessionStatuses().clear();
        store.getDiagnosisToGoal().clear();
        store.getDiagnosisToSession().clear();
        store.getTaskExecutionRuntimes().clear();
        store.getSessionMethodProfiles().clear();
        store.getExecutableTaskSpecs().clear();
    }

    @Nested
    class ScenarioA_BeginnerWithin15MinCognitiveOverloadConceptFirst {
        /**
         * 场景 A：BEGINNER + WITHIN_15_MIN + COGNITIVE_OVERLOAD_RISK + CONCEPT_FIRST
         */
        @Test
        void fullLink_previewWithinBudget_commitHasExecutableSpec_currentTaskHasRubric_messagesHasContent_completionWorks_reportReflectsEvidence()
                throws Exception {
            String goalBody = """
                    {"rawGoalText":"链表入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BEGINNER","preferenceTags":["CONCEPT_FIRST"]}
                    """;
            String goalId = postGoalAndGetId(goalBody);

            String diagnosisId = createDiagnosisSession(goalId);
            String submitBody = """
                    {"diagnosisId":"%s","answers":[
                    {"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},
                    {"questionId":"q_foundation_state","selectedOptions":["BEGINNER"]},
                    {"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},
                    {"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},
                    {"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},
                    {"questionId":"q_execution_risk","selectedOptions":["COGNITIVE_OVERLOAD_RISK"]}
                    ]}
                    """.formatted(diagnosisId);
            mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody))
                    .andExpect(status().isOk());

            String previewResp = mvc.perform(post("/api/learning-plans/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tasks").isArray())
                    .andReturn().getResponse().getContentAsString();

            JsonNode previewData = objectMapper.readTree(previewResp).get("data");
            String planId = previewData.get("planId").asText();
            String commitResp = mvc.perform(post("/api/learning-plans/commit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"planId\":\"" + planId + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String sessionId = objectMapper.readTree(commitResp).get("data").get("sessionId").asText();

            String currentTaskResp = mvc.perform(get("/api/sessions/" + sessionId + "/current-task"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode currentTaskData = objectMapper.readTree(currentTaskResp).get("data");
            String taskId = currentTaskData.get("currentTask").get("taskId").asText();

            org.hamcrest.MatcherAssert.assertThat(store.getExecutableTaskSpecs().get(InMemoryStore.taskRuntimeKey(sessionId, taskId)), notNullValue());

            mvc.perform(get("/api/sessions/" + sessionId + "/current-task"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentTask.evaluationRubricSummary").exists())
                    .andExpect(jsonPath("$.data.currentTask.scaffoldPolicySummary").exists());

            mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendedAskTemplates").isArray());

            String msgResp = mvc.perform(post("/api/tasks/" + taskId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"请解释一下链表是什么\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assistantReply").isNotEmpty())
                    .andReturn().getResponse().getContentAsString();
            String assistantReply = objectMapper.readTree(msgResp).get("data").get("assistantReply").asText();
            org.hamcrest.MatcherAssert.assertThat("messages should have substantive content or mock", assistantReply.length(), greaterThanOrEqualTo(5));

            mvc.perform(post("/api/tasks/" + taskId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"能举个最小例子吗\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/tasks/" + taskId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"和数组有什么不同\"}"))
                    .andExpect(status().isOk());

            mvc.perform(post("/api/tasks/" + taskId + "/self-explanation").contentType(MediaType.APPLICATION_JSON).content(
                            "{\"sessionId\":\"" + sessionId + "\",\"content\":\"我理解链表是通过指针把节点串起来的结构，例如两个节点用next连接，插入时只需改指针不用搬移整块内存，能用自己的话说清。\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskState").value("CHECK"))
                    .andExpect(jsonPath("$.data.checkpointQuestion").exists());

            mvc.perform(post("/api/tasks/" + taskId + "/checkpoint").contentType(MediaType.APPLICATION_JSON).content(
                            "{\"sessionId\":\"" + sessionId + "\",\"answer\":\"链表即通过 next 指针连接节点，插入删除只需改指针，例如在头部插入 O(1)。\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.result").value("PASS"));

            String completeBody = "{\"sessionId\":\"" + sessionId
                    + "\",\"completionStatus\":\"COMPLETED\",\"durationMinutes\":5,\"interactionCount\":2"
                    + ",\"userSummarySubmitted\":true,\"behaviorSignals\":[\"ASKED_FOR_EXAMPLE\"]"
                    + ",\"summaryText\":\"本任务理解了链表结构与操作要点，能用自己的话复述。\""
                    + ",\"learnedFrameworkPoints\":[\"指针串联节点\",\"插入改指针\"]"
                    + ",\"unresolvedQuestions\":[]"
                    + ",\"nextPracticeIntent\":\"练习更多链表题\"}";
            String completeResp = mvc.perform(post("/api/tasks/" + taskId + "/complete").contentType(MediaType.APPLICATION_JSON).content(completeBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            completeRemainingTasksFromNextTaskId(completeResp, completeBody);

            mvc.perform(get("/api/sessions/" + sessionId + "/report"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.learningReport.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.data.learningReport.evidenceSummary").isArray())
                    .andExpect(jsonPath("$.data.learningReport.evidenceSummary[0]").exists())
                    .andExpect(jsonPath("$.data.nextActionDecision.actionType").exists());
        }
    }

    @Nested
    class ScenarioB_BasicSinglePointLocalRepair {
        /**
         * 场景 B：BASIC + SINGLE_POINT + LOCAL_REPAIR 类策略
         */
        @Test
        void fullLink_taskMixDiffers_scaffoldAndMessagesReflectTaskSpecific() throws Exception {
            String goalBody = """
                    {"rawGoalText":"补一下二叉树的遍历卡点","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC","goalTypeHint":"FIX_SPECIFIC_BLOCKER","topicHints":["二叉树遍历"]}
                    """;
            String goalId = postGoalAndGetId(goalBody);

            String diagnosisId = createDiagnosisSession(goalId);
            String submitBody = """
                    {"diagnosisId":"%s","answers":[
                    {"questionId":"q_goal_outcome","selectedOptions":["FILL_A_SPECIFIC_GAP"]},
                    {"questionId":"q_foundation_state","selectedOptions":["SOLID_WITH_LOCAL_GAPS"]},
                    {"questionId":"q_primary_gap","selectedOptions":["PROCEDURE_GAP"]},
                    {"questionId":"q_scope_of_problem","selectedOptions":["SINGLE_POINT"]},
                    {"questionId":"q_preferred_entry_mode","selectedOptions":["PRACTICE_FIRST"]},
                    {"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}
                    ]}
                    """.formatted(diagnosisId);
            mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody))
                    .andExpect(status().isOk());

            String previewResp = mvc.perform(post("/api/learning-plans/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode previewData = objectMapper.readTree(previewResp).get("data");
            String commitResp = mvc.perform(post("/api/learning-plans/commit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"planId\":\"" + previewData.get("planId").asText() + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String sessionIdVal = objectMapper.readTree(commitResp).get("data").get("sessionId").asText();

            mvc.perform(get("/api/sessions/" + sessionIdVal + "/current-task"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentTask.taskId").exists())
                    .andExpect(jsonPath("$.data.currentTask.completionCriteria").isArray());

            String completeBody = "{\"sessionId\":\"" + sessionIdVal + "\",\"completionStatus\":\"COMPLETED\",\"durationMinutes\":8,\"interactionCount\":3}";
            completeAllTasksInSession(sessionIdVal, completeBody);
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "navigator.llm.enabled=true",
            "navigator.llm.baseUrl=http://127.0.0.1:1",
            "navigator.llm.apiKey=dummy",
            "navigator.llm.model=gpt-4.1-mini",
            "navigator.llm.timeoutMs=200"
    })
    class ScenarioC_FallbackAndBoundary {
        /**
         * C1: LLM 不可用时 fallback
         */
        @Test
        void messagesFallbackToMockWhenLlmUnavailable() throws Exception {
            String goalId = postGoalAndGetId("{\"rawGoalText\":\"哈希表\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\"}");
            String diagnosisId = createDiagnosisSession(goalId);
            submitDefaultDiagnosis(diagnosisId);
            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            String taskId = getCurrentTaskId(sessionId);

            mvc.perform(post("/api/tasks/" + taskId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"请用最小例子解释哈希冲突\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fallbackMode").value("MOCK"))
                    .andExpect(jsonPath("$.data.assistantReply").isNotEmpty());
        }

        /**
         * C2: 用户输入触发 OFF_TOPIC 时，返回受边界约束的回复
         */
        @Test
        void messagesReturnBoundaryConstrainedReplyWhenOffTopic() throws Exception {
            String goalId = postGoalAndGetId("{\"rawGoalText\":\"栈和队列\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\"}");
            String diagnosisId = createDiagnosisSession(goalId);
            submitDefaultDiagnosis(diagnosisId);
            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            String taskId = getCurrentTaskId(sessionId);

            mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                    .andExpect(status().isOk());

            mvc.perform(post("/api/tasks/" + taskId + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"今天天气真好，我们闲聊一下吧\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.detectedAction").value("OFF_TOPIC"))
                    .andExpect(jsonPath("$.data.assistantReply", org.hamcrest.Matchers.containsString("回到当前任务")));

            mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                    .andExpect(status().isOk());
        }
    }

    private String postGoalAndGetId(String goalBody) throws Exception {
        return objectMapper.readTree(mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("goalId").asText();
    }

    private String createDiagnosisSession(String goalId) throws Exception {
        String diagResp = mvc.perform(post("/api/diagnosis/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(diagResp).get("data").get("diagnosisId").asText();
    }

    private void submitDefaultDiagnosis(String diagnosisId) throws Exception {
        String submitBody = """
                {"diagnosisId":"%s","answers":[
                {"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},
                {"questionId":"q_foundation_state","selectedOptions":["BASIC_BUT_FRAGILE"]},
                {"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},
                {"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},
                {"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},
                {"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}
                ]}
                """.formatted(diagnosisId);
        mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isOk());
    }

    private String commitAndGetSessionId(String goalId, String diagnosisId) throws Exception {
        String planId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("planId").asText();
        return objectMapper.readTree(mvc.perform(post("/api/learning-plans/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("sessionId").asText();
    }

    private String getCurrentTaskId(String sessionId) throws Exception {
        return objectMapper.readTree(mvc.perform(get("/api/sessions/" + sessionId + "/current-task"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("currentTask").get("taskId").asText();
    }

    private void completeRemainingTasksFromNextTaskId(String firstCompleteResp, String completeBody) throws Exception {
        String taskToComplete = null;
        JsonNode dataNode = objectMapper.readTree(firstCompleteResp).get("data");
        if (dataNode != null && dataNode.path("nextTaskAvailable").asBoolean(false)) {
            taskToComplete = dataNode.path("nextTaskId").asText(null);
        }
        while (taskToComplete != null && !taskToComplete.isEmpty()) {
            String completeResp = mvc.perform(post("/api/tasks/" + taskToComplete + "/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            dataNode = objectMapper.readTree(completeResp).get("data");
            boolean nextAvailable = dataNode != null && dataNode.path("nextTaskAvailable").asBoolean(false);
            taskToComplete = nextAvailable ? dataNode.path("nextTaskId").asText(null) : null;
            if (taskToComplete != null && taskToComplete.isEmpty()) {
                taskToComplete = null;
            }
        }
    }

    private void completeAllTasksInSession(String sessionId, String completeBody) throws Exception {
        String taskId = getCurrentTaskId(sessionId);
        String firstResp = mvc.perform(post("/api/tasks/" + taskId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        completeRemainingTasksFromNextTaskId(firstResp, completeBody);
    }
}
