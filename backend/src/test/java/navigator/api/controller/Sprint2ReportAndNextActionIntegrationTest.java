package navigator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
class Sprint2ReportAndNextActionIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLinkWithReportAndNextAction() throws Exception {
        Cookie authCookie = TestAuthSupport.registerAndLogin(mvc, "sprint2_user");
        String goalBody = """
                {"rawGoalText":"我想搞懂链表","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC","preferenceTags":["CONCEPT_FIRST","STEP_BY_STEP"]}
                """;
        String goalResp = mvc.perform(post("/api/goals").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content(goalBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structuredGoal.goalType").value("LEARN_NEW_CONCEPT"))
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(goalResp).get("data").get("goalId").asText();

        String diagResp = mvc.perform(post("/api/diagnosis/sessions").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andReturn().getResponse().getContentAsString();
        String diagnosisId = objectMapper.readTree(diagResp).get("data").get("diagnosisId").asText();

        String submitBody = """
                {"diagnosisId":"%s","answers":[{"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},{"questionId":"q_foundation_state","selectedOptions":["BASIC_BUT_FRAGILE"]},{"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},{"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},{"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},{"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}]}
                """.formatted(diagnosisId);
        mvc.perform(post("/api/diagnosis/submissions").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnerProfileSnapshot").exists());

        String previewResp = mvc.perform(post("/api/learning-plans/preview").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREVIEW_READY"))
                .andExpect(jsonPath("$.data.tasks").isArray())
                .andReturn().getResponse().getContentAsString();
        String planId = objectMapper.readTree(previewResp).get("data").get("planId").asText();

        String commitResp = mvc.perform(post("/api/learning-plans/commit").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(commitResp).get("data").get("sessionId").asText();
        String currentTaskId = objectMapper.readTree(commitResp).get("data").get("currentTaskId").asText();

        mvc.perform(get("/api/sessions/" + sessionId + "/current-task").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").exists());

        String completeBody = """
                {"sessionId":"%s","completionStatus":"COMPLETED","durationMinutes":10,"interactionCount":3,"userSummarySubmitted":true,"detectedIssueTags":["CONCEPT_GAP"]}
                """.formatted(sessionId);
        String taskToComplete = currentTaskId;
        while (taskToComplete != null && !taskToComplete.isEmpty()) {
            String completeResp = mvc.perform(post("/api/tasks/" + taskToComplete + "/complete").cookie(authCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var dataNode = objectMapper.readTree(completeResp).get("data");
            boolean nextAvailable = dataNode.path("nextTaskAvailable").asBoolean(false);
            taskToComplete = nextAvailable ? dataNode.path("nextTaskId").asText(null) : null;
            if (taskToComplete != null && taskToComplete.isEmpty()) {
                taskToComplete = null;
            }
        }

        mvc.perform(get("/api/sessions/" + sessionId + "/report").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningReport.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.learningReport.completedProgress[0]").exists())
                .andExpect(jsonPath("$.data.nextActionDecision.actionType").exists());

        mvc.perform(post("/api/sessions/" + sessionId + "/next-action").cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REINFORCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.acceptedAction").value("REINFORCE"))
                .andExpect(jsonPath("$.data.nextHint").exists());
    }
}
