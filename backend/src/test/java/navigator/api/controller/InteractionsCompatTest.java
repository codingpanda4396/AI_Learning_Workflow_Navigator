package navigator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class InteractionsCompatTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void interactionsShouldRemainAvailableAndWriteUnifiedMessageEvidence() throws Exception {
        String goalBody = """
                {"rawGoalText":"栈入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
                """;
        String goalId = objectMapper.readTree(mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("goalId").asText();

        String diagResp = mvc.perform(post("/api/diagnosis/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String diagnosisId = objectMapper.readTree(diagResp).get("data").get("diagnosisId").asText();

        String submitBody = """
                {"diagnosisId":"%s","answers":[{"questionId":"q_goal_outcome","selectedOptions":["BUILD_FRAMEWORK"]},{"questionId":"q_foundation_state","selectedOptions":["BASIC_BUT_FRAGILE"]},{"questionId":"q_primary_gap","selectedOptions":["CONCEPT_GAP"]},{"questionId":"q_scope_of_problem","selectedOptions":["MULTI_POINT"]},{"questionId":"q_preferred_entry_mode","selectedOptions":["CONCEPT_FIRST"]},{"questionId":"q_execution_risk","selectedOptions":["LOW_RISK"]}]}
                """.formatted(diagnosisId);
        mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isOk());

        String planId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\",\"diagnosisId\":\"" + diagnosisId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("planId").asText();

        String sessionId = objectMapper.readTree(mvc.perform(post("/api/learning-plans/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("sessionId").asText();

        String taskId = objectMapper.readTree(mvc.perform(get("/api/sessions/" + sessionId + "/current-task"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("data").get("currentTask").get("taskId").asText();

        String legacyBody = "{\"sessionId\":\"" + sessionId + "\",\"interactionType\":\"GENERIC\",\"contentSummary\":\"我尝试用数组模拟栈，但是 pop 的边界不确定\",\"behaviorSignals\":[\"CONFUSION_SIGNAL\"]}";
        mvc.perform(post("/api/tasks/" + taskId + "/interactions").contentType(MediaType.APPLICATION_JSON).content(legacyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentMessages").isArray())
                .andExpect(jsonPath("$.data.recentMessages[0].role").value("USER"));
    }
}

