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
    void chatReturnsFallbackWhenLlmDisabled() throws Exception {
        String body = """
                {"message":"我觉得二叉树像分叉","context":{"step":1,"knowledge":"binary_tree","phase":"STRUCTURE","knowledgeLabel":"二叉树"}}
                """;
        mvc.perform(post("/api/ai-tutor/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("FALLBACK"))
                .andExpect(jsonPath("$.data.reply").isNotEmpty());
    }
}
