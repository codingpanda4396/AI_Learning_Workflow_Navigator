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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginMeAndLogoutShouldWork() throws Exception {
        String registerResp = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_case","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("auth_case"))
                .andReturn().getResponse().getContentAsString();
        Cookie cookie = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_case","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("auth_case"))
                .andReturn().getResponse().getCookie("lumina_session");

        mvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.user.username").value("auth_case"));

        Cookie clearedCookie = mvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("lumina_session");

        mvc.perform(get("/api/auth/me").cookie(clearedCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    void goalsShouldRequireAuthentication() throws Exception {
        mvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawGoalText":"图论入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void goalShouldBeScopedToOwner() throws Exception {
        Cookie userACookie = TestAuthSupport.registerAndLogin(mvc, "owner_a");
        Cookie userBCookie = TestAuthSupport.registerAndLogin(mvc, "owner_b");
        String goalId = objectMapper.readTree(mvc.perform(post("/api/goals").cookie(userACookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawGoalText":"链表入门","timeBudget":"WITHIN_30_MIN","selfReportedLevel":"BASIC"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data").get("goalId").asText();

        mvc.perform(post("/api/diagnosis/sessions").cookie(userBCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalId\":\"" + goalId + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
