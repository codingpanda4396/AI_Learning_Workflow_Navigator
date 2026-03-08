package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmJsonParserTest {

    private final LlmJsonParser parser = new LlmJsonParser(new ObjectMapper());

    @Test
    void shouldParseJsonInsideCodeFence() {
        String raw = "```json\n{\"score\":88,\"feedback\":\"ok\"}\n```";
        assertEquals(88, parser.parse(raw).path("score").asInt());
    }

    @Test
    void shouldExtractJsonFromWrappedText() {
        String raw = "前置说明 {\"questions\":[{\"id\":\"q1\"}]} 后置说明";
        assertEquals("q1", parser.parse(raw).path("questions").path(0).path("id").asText());
    }
}

