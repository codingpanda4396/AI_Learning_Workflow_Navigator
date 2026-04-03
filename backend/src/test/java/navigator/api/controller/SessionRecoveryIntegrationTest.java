package navigator.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import navigator.infrastructure.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionRecoveryIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InMemoryStore store;

    @Test
    void currentTaskAndFinalReportStillWorkAfterInMemoryStateIsCleared() throws Exception {
        Cookie authCookie = TestAuthSupport.registerAndLogin(mvc, "sess_reco");

        String goalId = objectMapper.readTree(mvc.perform(post("/api/goals").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawGoalText":"DFS 和 BFS 快速入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data").get("goalId").asText();

        String diagnosisId = objectMapper.readTree(mvc.perform(post("/api/diagnosis/sessions").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data").get("diagnosisId").asText();

        mvc.perform(post("/api/diagnosis/submissions").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diagnosisId":"%s","answers":[{"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},{"questionId":"q_foundation_state","selectedOptions":["BASIC_BUT_FRAGILE"]},{"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},{"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},{"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},{"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}]}
                                """.formatted(diagnosisId)))
                .andExpect(status().isOk());

        String planId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/preview").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data").get("planId").asText();

        String sessionId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/commit").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data").get("sessionId").asText();

        clearRuntimeCaches();

        String currentTaskResp = mvc.perform(get("/api/sessions/" + sessionId + "/current-task").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").exists())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(currentTaskResp).get("data").get("taskId").asText();
        while (taskId != null && !taskId.isEmpty()) {
            String completeResp = mvc.perform(post("/api/tasks/" + taskId + "/complete").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sessionId":"%s","completionStatus":"COMPLETED","durationMinutes":6,"interactionCount":2,"userSummarySubmitted":true,"summaryText":"本任务已完成并形成一条稳定判断。","learnedFrameworkPoints":["先区分搜索顺序","再说明适用场景"],"nextPracticeIntent":"下一题先自己说判断。"}
                                    """.formatted(sessionId)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode data = objectMapper.readTree(completeResp).get("data");
            taskId = data.path("nextTaskAvailable").asBoolean(false)
                    ? data.path("nextTaskId").asText(null)
                    : null;
            if (taskId != null && taskId.isEmpty()) {
                taskId = null;
            }
        }

        clearRuntimeCaches();

        mvc.perform(get("/api/sessions/" + sessionId + "/report").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningReport.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.learningReport.finalSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.nextActionDecision.actionType").exists());
    }

    private void clearRuntimeCaches() {
        store.getSessions().clear();
        store.getPlanPreviews().clear();
        store.getPlanStatuses().clear();
        store.getDiagnosisToSession().clear();
        store.getTaskExecutionRuntimes().clear();
        store.getExecutableTaskSpecs().clear();
        store.getSessionMethodProfiles().clear();
    }
}
