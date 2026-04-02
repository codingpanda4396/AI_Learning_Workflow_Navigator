package navigator.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import navigator.application.scaffold.DfsBfsStructureScaffoldDefinition;
import navigator.infrastructure.memory.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:scaffold_action_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "navigator.llm.enabled=false"
})
class LearningScaffoldActionIntegrationTest {

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
        authCookie = TestAuthSupport.registerAndLogin(mvc, "sa");
    }

    @Test
    void submitActionReturnsUpdatedStageAlignedWithGetStage() throws Exception {
        String goalId = postGoalAndGetId("""
                {"rawGoalText":"搞懂 DFS 与 BFS 的区别","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BEGINNER"}
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

        String stageBefore = mvc.perform(get("/api/tasks/" + taskId + "/learning-scaffold/stage/current")
                        .cookie(authCookie)
                        .param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageKey").value("STRUCTURE"))
                .andExpect(jsonPath("$.data.currentActionId").value(DfsBfsStructureScaffoldDefinition.ACTION_POSITION))
                .andReturn().getResponse().getContentAsString();

        String actionBody = """
                {"sessionId":"%s","stageKey":"STRUCTURE","actionId":"%s","userInput":"STRUCTURE:sq1:A"}
                """.formatted(sessionId, DfsBfsStructureScaffoldDefinition.ACTION_POSITION);

        String submitResp = mvc.perform(post("/api/tasks/" + taskId + "/learning-scaffold/action")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedStage").exists())
                .andExpect(jsonPath("$.data.updatedStage.currentActionId").value(DfsBfsStructureScaffoldDefinition.ACTION_PREREQ))
                .andExpect(jsonPath("$.data.updatedStage.stageKey").value("STRUCTURE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode submitData = objectMapper.readTree(submitResp).get("data");
        String fromSubmit = submitData.get("updatedStage").get("currentActionId").asText();

        String stageAfter = mvc.perform(get("/api/tasks/" + taskId + "/learning-scaffold/stage/current")
                        .cookie(authCookie)
                        .param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String fromGet = objectMapper.readTree(stageAfter).get("data").get("currentActionId").asText();
        org.junit.jupiter.api.Assertions.assertEquals(fromSubmit, fromGet);

        // 避免误用：提交前与提交后阶段快照应不同（动作卡已推进）
        org.junit.jupiter.api.Assertions.assertNotEquals(
                objectMapper.readTree(stageBefore).get("data").get("currentActionId").asText(),
                fromGet);
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
                .get("data").get("taskId").asText();
    }
}
