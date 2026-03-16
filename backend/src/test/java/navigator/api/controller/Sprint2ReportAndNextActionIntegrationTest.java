package navigator.api.controller;

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
 * Sprint 2 集成测试：基于 DB 的 report 与 next-action。
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint2ReportAndNextActionIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void fullLinkWithReportAndNextAction() throws Exception {
        String goalBody = """
                {"rawGoalText":"我想搞懂链表","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC","preferenceTags":["CONCEPT_FIRST","STEP_BY_STEP"]}
                """;
        mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structuredGoal.goalType").value("LEARN_NEW_CONCEPT"));

        mvc.perform(post("/api/diagnosis/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"goal_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        String submitBody = """
                {"diagnosisId":"diag_001","answers":[{"questionId":"q_foundation","selectedOptions":["BASIC"]},{"questionId":"q_gap","selectedOptions":["CONCEPT_GAP"]}]}
                """;
        mvc.perform(post("/api/diagnosis/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnerProfileSnapshot").exists());

        mvc.perform(post("/api/learning-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"goal_001\",\"diagnosisId\":\"diag_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREVIEW_READY"))
                .andExpect(jsonPath("$.data.tasks").isArray());

        mvc.perform(post("/api/learning-plans/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"plan_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("learn_session_001"));

        // 获取当前任务
        mvc.perform(get("/api/sessions/learn_session_001/current-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentTask.taskId").exists());

        // 完成当前任务（使用固定样例中的 task_001）
        String completeBody = """
                {"sessionId":"learn_session_001","completionStatus":"COMPLETED","durationMinutes":10,"interactionCount":3,"userSummarySubmitted":true,"detectedIssueTags":["CONCEPT_GAP"]}
                """;
        mvc.perform(post("/api/tasks/task_001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskExecutionRecord.taskId").value("task_001"));

        // 已完成至少一个任务后获取报告
        mvc.perform(get("/api/sessions/learn_session_001/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningReport.sessionId").value("learn_session_001"))
                .andExpect(jsonPath("$.data.learningReport.completedProgress[0]").exists())
                .andExpect(jsonPath("$.data.nextActionDecision.actionType").exists());

        // 确认 next-action
        mvc.perform(post("/api/sessions/learn_session_001/next-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REINFORCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("learn_session_001"))
                .andExpect(jsonPath("$.data.acceptedAction").value("REINFORCE"))
                .andExpect(jsonPath("$.data.nextHint").exists());
    }
}

