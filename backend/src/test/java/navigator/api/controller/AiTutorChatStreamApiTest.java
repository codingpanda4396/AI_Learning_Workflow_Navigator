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
    void streamReturnsSseFallbackWhenLlmDisabled() throws Exception {
        String body = """
                {"message":"我觉得二叉树像分叉","context":{"step":1,"knowledge":"binary_tree","phase":"STRUCTURE","knowledgeLabel":"二叉树"}}
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

        assertThat(out).contains("event:meta");
        assertThat(out).contains("FALLBACK");
        assertThat(out).contains("event:delta");
        assertThat(out).contains("event:done");
    }
}
