package navigator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class Sprint3ExecutionPersistenceRecoveryTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InMemoryStore store;

    @Test
    void scaffoldAndMessagesAreRecoverableAfterInMemoryCleared() throws Exception {
        String goalBody = """
                {"rawGoalText":"数组入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
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

        mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionSnapshot.currentState").value("ORIENT"));

        mvc.perform(post("/api/tasks/" + taskId + "/messages").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"content\":\"数组和链表最大的差异是什么？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskState").value("EXPLORE"));

        // Simulate server restart / runtime eviction.
        store.getTaskExecutionRuntimes().clear();

        mvc.perform(get("/api/tasks/" + taskId + "/scaffold").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionSnapshot.currentState").value("EXPLORE"))
                .andExpect(jsonPath("$.data.executionSnapshot.exploreTurnCount").value(1))
                .andExpect(jsonPath("$.data.recentMessages").isArray())
                .andExpect(jsonPath("$.data.recentMessages.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // Should still block complete because persisted state != PASS.
        mvc.perform(post("/api/tasks/" + taskId + "/complete").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"completionStatus\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TASK_EXECUTION_NOT_READY_FOR_COMPLETE"));
    }
}

