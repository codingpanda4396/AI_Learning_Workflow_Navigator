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

/**
 * Sprint 3：脚手架 + 消息流 + 自解释 + 微检查 + complete。
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint3TaskExecutionFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scaffoldThenMessagesSelfExplainCheckpointComplete() throws Exception {
        String goalBody = """
                {"rawGoalText":"链表入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
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
                .andExpect(jsonPath("$.data.currentExecutionState").value("ORIENT"))
                .andExpect(jsonPath("$.data.recommendedAskTemplates").isArray());

        String msgBody = "{\"sessionId\":\"" + sessionId + "\",\"content\":\"请解释一下链表是什么\"}";
        mvc.perform(post("/api/tasks/" + taskId + "/messages").contentType(MediaType.APPLICATION_JSON).content(msgBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskState").value("EXPLORE"))
                .andExpect(jsonPath("$.data.guidancePhase").exists());
        mvc.perform(get("/api/sessions/" + sessionId + "/current-task-guidance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyVersion").exists());
        mvc.perform(post("/api/tasks/" + taskId + "/messages").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"content\":\"能举个最小例子吗\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detectedAction").value("ASK_FOR_EXAMPLE"));
        mvc.perform(post("/api/tasks/" + taskId + "/messages").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"content\":\"和数组相比插入操作哪里更省\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks/" + taskId + "/execution-summary").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidenceSnapshot.totalTurns").exists());

        mvc.perform(post("/api/tasks/" + taskId + "/self-explanation").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"content\":\"我理解链表是通过指针把节点串起来的结构，例如两个节点用 next 连接；与数组相比插入时不同，链表只需改指针不用搬移整块内存。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskState").value("CHECK"))
                .andExpect(jsonPath("$.data.checkpointQuestion").exists());

        mvc.perform(post("/api/tasks/" + taskId + "/checkpoint").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"sessionId\":\"" + sessionId + "\",\"answer\":\"链表通过 next 指针连接节点，插入删除只需 O(1) 改指针\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PASS"));

        String completeBody = "{\"sessionId\":\"" + sessionId
                + "\",\"completionStatus\":\"COMPLETED\",\"durationMinutes\":5,\"interactionCount\":2"
                + ",\"summaryText\":\"本任务理解了链表节点与指针关系及插入思路。\""
                + ",\"learnedFrameworkPoints\":[\"节点与next指针\",\"插入只需改指针\"]"
                + ",\"unresolvedQuestions\":[]"
                + ",\"nextPracticeIntent\":\"下一题手写反转链表\"}";
        mvc.perform(post("/api/tasks/" + taskId + "/complete").contentType(MediaType.APPLICATION_JSON).content(completeBody))
                .andExpect(status().isOk());
    }
}
