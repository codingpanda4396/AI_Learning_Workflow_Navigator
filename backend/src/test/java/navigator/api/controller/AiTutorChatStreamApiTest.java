package navigator.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "navigator.llm.enabled=false",
})
class AiTutorChatStreamApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void streamReturnsSseErrorWhenLlmDisabled() throws Exception {
        String body = """
                {"messages":[{"role":"user","content":"我想知道 DFS 为什么会回退"}],"context":{"step":1,"knowledge":"dfs_bfs","phase":"UNDERSTANDING","knowledgeLabel":"DFS / BFS"}}
                """;
        MvcResult async = mvc.perform(post("/api/ai-tutor/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();

        String out = mvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(out).contains("event:error");
        assertThat(out).doesNotContain("event:delta");
    }
}
