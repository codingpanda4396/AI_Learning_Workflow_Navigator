package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LlmCallLog;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLlmCallLogRepository implements LlmCallLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLlmCallLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(LlmCallLog log) {
        jdbcTemplate.update(
            """
                INSERT INTO llm_call_log (
                    task_attempt_id, biz_type, provider, model, prompt_template_key, prompt_version,
                    request_payload, response_payload, parsed_json, status, latency_ms, token_input, token_output
                )
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?)
                """,
            log.taskAttemptId(),
            log.bizType(),
            log.provider(),
            log.model(),
            log.promptTemplateKey(),
            log.promptVersion(),
            jsonOrNull(log.requestPayload()),
            jsonOrNull(log.responsePayload()),
            jsonOrNull(log.parsedJson()),
            log.status(),
            log.latencyMs(),
            log.tokenInput(),
            log.tokenOutput()
        );
    }

    private String jsonOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}

