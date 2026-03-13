package com.pandanav.learning.application.service.llm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmStage;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmJsonParserTest {

    private final LlmJsonParser parser = new LlmJsonParser(new ObjectMapper());

    @Test
    void shouldParsePureJson() {
        assertEquals(88, parser.parse("{\"score\":88}").path("score").asInt());
    }

    @Test
    void shouldParseJsonInsideCodeFence() {
        String raw = "```json\n{\"score\":88,\"feedback\":\"ok\"}\n```";
        assertEquals(88, parser.parse(raw).path("score").asInt());
    }

    @Test
    void shouldExtractJsonFromWrappedText() {
        String raw = "Here is the result: {\"questions\":[{\"id\":\"q1\"}]} Thanks.";
        assertEquals("q1", parser.parse(raw).path("questions").path(0).path("id").asText());
    }

    @Test
    void shouldThrowDiagnosableExceptionWhenJsonCannotBeParsed() {
        Logger logger = (Logger) LoggerFactory.getLogger(LlmJsonParser.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            LlmJsonParseException ex = assertThrows(LlmJsonParseException.class, () -> parser.parse(
                "```json\n{\"headline\":\"oops\",\n```",
                new LlmCallContext("trace-1", "req-1", LlmStage.LEARNING_PLAN, "test-model")
            ));
            assertTrue(ex.diagnosticSummary().contains("stage=LEARNING_PLAN"));
            assertTrue(ex.diagnosticSummary().contains("requestId=req-1"));
            assertTrue(ex.diagnosticSummary().contains("hasCodeFence=true"));

            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
            assertTrue(logs.contains("LLM_JSON_PARSE_FAILURE"));
            assertTrue(logs.contains("traceId=trace-1"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
