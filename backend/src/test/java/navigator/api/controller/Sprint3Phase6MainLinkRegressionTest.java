package navigator.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Cookie authCookie;

    @BeforeEach
    void setUp() throws Exception {
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
        authCookie = TestAuthSupport.registerAndLogin(mvc, "phase6_user");
    }

    @Nested
    class ScenarioA_BeginnerWithin15MinCognitiveOverloadConceptFirst {
        @Test
        void fullLink_previewCommitExecutionAndReport() throws Exception {
            String goalId = postGoalAndGetId("""
                    {"rawGoalText":"链表入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BEGINNER","preferenceTags":["CONCEPT_FIRST"]}
                    """);
            String diagnosisId = createDiagnosisSession(goalId);
            mvc.perform(post("/api/diagnosis/submissions").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content("""
                    {"diagnosisId":"%s","answers":[
                    {"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},
                    {"questionId":"q_foundation_state","selectedOptions":["BEGINNER"]},
                    {"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},
                    {"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},
                    {"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},
                    {"questionId":"q_execution_risk","selectedOptions":["COGNITIVE_OVERLOAD_RISK"]}
                    ]}
                    """.formatted(diagnosisId))).andExpect(status().isOk());

            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            String taskId = getCurrentTaskId(sessionId);
            org.hamcrest.MatcherAssert.assertThat(store.getExecutableTaskSpecs().get(InMemoryStore.taskRuntimeKey(sessionId, taskId)), notNullValue());

            mvc.perform(get("/api/sessions/" + sessionId + "/current-task").cookie(authCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentTask.evaluationRubricSummary").exists())
                    .andExpect(jsonPath("$.data.currentTask.scaffoldPolicySummary").exists());

            mvc.perform(get("/api/tasks/" + taskId + "/scaffold").cookie(authCookie).param("sessionId", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendedAskTemplates").isArray());

            String msgResp = mvc.perform(post("/api/tasks/" + taskId + "/messages").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"请解释一下链表是什么\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assistantReply").isNotEmpty())
                    .andReturn().getResponse().getContentAsString();
            org.hamcrest.MatcherAssert.assertThat(objectMapper.readTree(msgResp).get("data").get("assistantReply").asText().length(), greaterThanOrEqualTo(5));

            mvc.perform(post("/api/tasks/" + taskId + "/messages").cookie(authCookie).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"能举个最小例子吗\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/tasks/" + taskId + "/messages").cookie(authCookie).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"和数组有什么不同\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/tasks/" + taskId + "/self-explanation").cookie(authCookie).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"链表由节点和 next 指针组成，例如 1->2->3。插入头结点时，新节点只要指向原头结点即可，不像数组那样需要整体搬移元素。\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskState").value("CHECK"));
            mvc.perform(post("/api/tasks/" + taskId + "/checkpoint").cookie(authCookie).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"answer\":\"链表靠 next 连接节点，例如 1->2->3；在头部插入新节点时，只要让新节点指向原头结点即可。\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.result").value("PASS"));

            String completeBody = "{\"sessionId\":\"" + sessionId
                    + "\",\"completionStatus\":\"COMPLETED\",\"durationMinutes\":5,\"interactionCount\":2"
                    + ",\"userSummarySubmitted\":true,\"summaryText\":\"本任务理解了链表结构与插入逻辑。\""
                    + ",\"learnedFrameworkPoints\":[\"节点与指针\",\"插入改指针\"]"
                    + ",\"nextPracticeIntent\":\"继续练习链表基础题\"}";
            String completeResp = mvc.perform(post("/api/tasks/" + taskId + "/complete").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content(completeBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            completeRemainingTasks(completeResp, completeBody);

            mvc.perform(get("/api/sessions/" + sessionId + "/report").cookie(authCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.learningReport.evidenceSummary").isArray())
                    .andExpect(jsonPath("$.data.nextActionDecision.actionType").exists());
        }
    }

    @Nested
    class ScenarioB_BasicSinglePointLocalRepair {
        @Test
        void fullLink_taskMixDiffersAndAllTasksCanComplete() throws Exception {
            String goalId = postGoalAndGetId("""
                    {"rawGoalText":"补一下二叉树遍历卡点","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC","goalTypeHint":"FIX_SPECIFIC_BLOCKER","topicHints":["二叉树遍历"]}
                    """);
            String diagnosisId = createDiagnosisSession(goalId);
            mvc.perform(post("/api/diagnosis/submissions").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content("""
                    {"diagnosisId":"%s","answers":[
                    {"questionId":"q_goal_outcome","selectedOptions":["FILL_A_SPECIFIC_GAP"]},
                    {"questionId":"q_foundation_state","selectedOptions":["SOLID_WITH_LOCAL_GAPS"]},
                    {"questionId":"q_primary_gap","selectedOptions":["PROCEDURE_GAP"]},
                    {"questionId":"q_scope_of_problem","selectedOptions":["SINGLE_POINT"]},
                    {"questionId":"q_preferred_entry_mode","selectedOptions":["PRACTICE_FIRST"]},
                    {"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}
                    ]}
                    """.formatted(diagnosisId))).andExpect(status().isOk());

            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            mvc.perform(get("/api/sessions/" + sessionId + "/current-task").cookie(authCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentTask.taskId").exists())
                    .andExpect(jsonPath("$.data.currentTask.completionCriteria").isArray());

            completeAllTasksInSession(sessionId, "{\"sessionId\":\"" + sessionId + "\",\"completionStatus\":\"COMPLETED\",\"durationMinutes\":8,\"interactionCount\":3,\"summaryText\":\"完成当前任务。\",\"learnedFrameworkPoints\":[\"步骤识别\",\"局部修复\"],\"nextPracticeIntent\":\"继续下一题\"}");
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
        @Test
        void messagesFallbackToMockWhenLlmUnavailable() throws Exception {
            String goalId = postGoalAndGetId("{\"rawGoalText\":\"哈希表\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\"}");
            String diagnosisId = createDiagnosisSession(goalId);
            submitDefaultDiagnosis(diagnosisId);
            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            String taskId = getCurrentTaskId(sessionId);

            mvc.perform(post("/api/tasks/" + taskId + "/messages").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"请用最小例子解释哈希冲突\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fallbackMode").value("MOCK"))
                    .andExpect(jsonPath("$.data.assistantReply").isNotEmpty());
        }

        @Test
        void messagesReturnBoundaryConstrainedReplyWhenOffTopic() throws Exception {
            String goalId = postGoalAndGetId("{\"rawGoalText\":\"栈和队列\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\"}");
            String diagnosisId = createDiagnosisSession(goalId);
            submitDefaultDiagnosis(diagnosisId);
            String sessionId = commitAndGetSessionId(goalId, diagnosisId);
            String taskId = getCurrentTaskId(sessionId);

            mvc.perform(get("/api/tasks/" + taskId + "/scaffold").cookie(authCookie).param("sessionId", sessionId))
                    .andExpect(status().isOk());

            mvc.perform(post("/api/tasks/" + taskId + "/messages").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sessionId + "\",\"content\":\"今天天气真好，我们闲聊一下吧\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.detectedAction").value("OFF_TOPIC"))
                    .andExpect(jsonPath("$.data.assistantReply", containsString("回到当前任务")));
        }
    }

    private String postGoalAndGetId(String goalBody) throws Exception {
        return objectMapper.readTree(mvc.perform(post("/api/goals").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content(goalBody))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("goalId").asText();
    }

    private String createDiagnosisSession(String goalId) throws Exception {
        String diagResp = mvc.perform(post("/api/diagnosis/sessions").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(diagResp).get("data").get("diagnosisId").asText();
    }

    private void submitDefaultDiagnosis(String diagnosisId) throws Exception {
        mvc.perform(post("/api/diagnosis/submissions").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content("""
                {"diagnosisId":"%s","answers":[
                {"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},
                {"questionId":"q_foundation_state","selectedOptions":["BASIC_BUT_FRAGILE"]},
                {"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},
                {"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},
                {"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},
                {"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}
                ]}
                """.formatted(diagnosisId))).andExpect(status().isOk());
    }

    private String commitAndGetSessionId(String goalId, String diagnosisId) throws Exception {
        String planId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/preview").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data").get("planId").asText();
        return objectMapper.readTree(mvc.perform(post("/api/learning-plans/commit").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data").get("sessionId").asText();
    }

    private String getCurrentTaskId(String sessionId) throws Exception {
        return objectMapper.readTree(mvc.perform(get("/api/sessions/" + sessionId + "/current-task").cookie(authCookie))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("currentTask").get("taskId").asText();
    }

    private void completeRemainingTasks(String firstCompleteResp, String completeBody) throws Exception {
        JsonNode dataNode = objectMapper.readTree(firstCompleteResp).get("data");
        String taskToComplete = dataNode.path("nextTaskAvailable").asBoolean(false) ? dataNode.path("nextTaskId").asText(null) : null;
        while (taskToComplete != null && !taskToComplete.isEmpty()) {
            String completeResp = mvc.perform(post("/api/tasks/" + taskToComplete + "/complete").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            dataNode = objectMapper.readTree(completeResp).get("data");
            taskToComplete = dataNode.path("nextTaskAvailable").asBoolean(false) ? dataNode.path("nextTaskId").asText(null) : null;
        }
    }

    private void completeAllTasksInSession(String sessionId, String completeBody) throws Exception {
        String firstTaskId = getCurrentTaskId(sessionId);
        String firstResp = mvc.perform(post("/api/tasks/" + firstTaskId + "/complete").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        completeRemainingTasks(firstResp, completeBody);
    }
}
