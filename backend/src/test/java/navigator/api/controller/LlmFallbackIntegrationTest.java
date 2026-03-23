package navigator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "navigator.llm.enabled=true",
        "navigator.llm.baseUrl=http://127.0.0.1:1",
        "navigator.llm.apiKey=dummy",
        "navigator.llm.model=gpt-4.1-mini",
        "navigator.llm.timeoutMs=200"
})
class LlmFallbackIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void messagesShouldFallbackToMockWhenProviderFails() throws Exception {
        String goalBody = """
                {"rawGoalText":"哈希表入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
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

        mvc.perform(post("/api/tasks/" + taskId + "/messages").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"content\":\"请用最小例子解释哈希冲突\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallbackMode").value("MOCK"))
                .andExpect(jsonPath("$.data.assistantReply").isNotEmpty());
    }

    @Test
    void aiTutorChatShouldReturnFallbackWhenProviderFails() throws Exception {
        String body = """
                {"message":"我觉得二叉树像分叉","context":{"step":1,"knowledge":"binary_tree","phase":"STRUCTURE","knowledgeLabel":"二叉树"}}
                """;
        mvc.perform(post("/api/ai-tutor/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("FALLBACK"))
                .andExpect(jsonPath("$.data.reply").isNotEmpty());
    }
}

