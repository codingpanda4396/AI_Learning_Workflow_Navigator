package navigator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.infrastructure.memory.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sprint 1 集成测试：主链路与非法状态。
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint1IntegrationTest {

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
    }

    @Test
    void fullLinkUnderstandingList() throws Exception {
        String goalBody = "{\"rawGoalText\":\"我想搞懂链表\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\",\"preferenceTags\":[\"CONCEPT_FIRST\",\"STEP_BY_STEP\"]}";
        mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structuredGoal.goalType").value("LEARN_NEW_CONCEPT"));

        mvc.perform(post("/api/diagnosis/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        String submitBody = "{\"diagnosisId\":\"diag_001\",\"answers\":[{\"questionId\":\"q_foundation\",\"selectedOptions\":[\"BASIC\"]},{\"questionId\":\"q_gap\",\"selectedOptions\":[\"CONCEPT_GAP\"]}]}";
        mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnerProfileSnapshot").exists());

        mvc.perform(post("/api/learning-plans/preview").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\",\"diagnosisId\":\"diag_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREVIEW_READY"))
                .andExpect(jsonPath("$.data.tasks").isArray());

        mvc.perform(post("/api/learning-plans/commit").contentType(MediaType.APPLICATION_JSON).content("{\"planId\":\"plan_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists());

        mvc.perform(get("/api/sessions/learn_session_001/current-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentTask").exists());
    }

    @Test
    void previewWithoutDiagnosisCompletedReturns409() throws Exception {
        String goalBody = "{\"rawGoalText\":\"我想搞懂链表\",\"timeBudget\":\"MULTI_DAY\",\"selfReportedLevel\":\"BEGINNER\"}";
        mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody)).andExpect(status().isOk());
        mvc.perform(post("/api/diagnosis/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/learning-plans/preview").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\",\"diagnosisId\":\"diag_001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_NOT_COMPLETED"));
    }

    @Test
    void currentTaskWithoutCommitReturns404BecauseNoSessionYet() throws Exception {
        String goalBody = "{\"rawGoalText\":\"我想搞懂链表\",\"timeBudget\":\"WITHIN_30_MIN\",\"selfReportedLevel\":\"BASIC\"}";
        mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(goalBody)).andExpect(status().isOk());
        mvc.perform(post("/api/diagnosis/sessions").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\"}")).andExpect(status().isOk());
        String submitBody = "{\"diagnosisId\":\"diag_001\",\"answers\":[{\"questionId\":\"q_foundation\",\"selectedOptions\":[\"BASIC\"]},{\"questionId\":\"q_gap\",\"selectedOptions\":[\"CONCEPT_GAP\"]}]}";
        mvc.perform(post("/api/diagnosis/submissions").contentType(MediaType.APPLICATION_JSON).content(submitBody)).andExpect(status().isOk());
        mvc.perform(post("/api/learning-plans/preview").contentType(MediaType.APPLICATION_JSON).content("{\"goalId\":\"goal_001\",\"diagnosisId\":\"diag_001\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/sessions/learn_session_001/current-task"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
