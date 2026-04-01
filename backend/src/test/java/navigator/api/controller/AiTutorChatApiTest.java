package navigator.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "navigator.llm.enabled=false",
})
class AiTutorChatApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void chatReturnsExplicitErrorWhenLlmDisabled() throws Exception {
        String body = """
                {"messages":[{"role":"user","content":"我想知道 DFS 为什么会回退"}],"context":{"step":1,"knowledge":"dfs_bfs","phase":"UNDERSTANDING","knowledgeLabel":"DFS / BFS"}}
                """;
        mvc.perform(post("/api/ai-tutor/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
