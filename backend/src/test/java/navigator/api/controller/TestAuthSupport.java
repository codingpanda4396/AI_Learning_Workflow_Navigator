package navigator.api.controller;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class TestAuthSupport {

    private TestAuthSupport() {
    }

    static Cookie registerAndLogin(MockMvc mvc, String username) throws Exception {
        String uniqueUsername = username + "_" + System.nanoTime();
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(uniqueUsername)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("lumina_session");
    }
}
