package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.AttemptLlmMetadata;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTaskRepository implements TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Task save(Task task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO task (session_id, stage, node_id, objective)
                    VALUES (?, ?::task_stage, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, task.getSessionId());
            ps.setString(2, task.getStage().name());
            ps.setLong(3, task.getNodeId());
            ps.setString(4, task.getObjective());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            task.setId(key.longValue());
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        return task;
    }

    @Override
    public List<Task> saveAll(List<Task> tasks) {
        return tasks.stream().map(this::save).toList();
    }

    @Override
    public Optional<Task> findById(Long id) {
        try {
            Task task = jdbcTemplate.queryForObject(
                """
                    SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                           COALESCE((
                               SELECT ta.status
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ), 'PENDING') AS status,
                           (
                               SELECT ta.output_json
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                                 AND ta.output_json IS NOT NULL
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ) AS output_json
                    FROM task t
                    WHERE t.id = ?
                    """,
                (rs, rowNum) -> mapTask(rs),
                id
            );
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Task> findByIdAndUserPk(Long id, Long userPk) {
        try {
            Task task = jdbcTemplate.queryForObject(
                """
                    SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                           COALESCE((
                               SELECT ta.status
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ), 'PENDING') AS status,
                           (
                               SELECT ta.output_json
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                                 AND ta.output_json IS NOT NULL
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ) AS output_json
                    FROM task t
                    JOIN learning_session ls ON ls.id = t.session_id
                    WHERE t.id = ?
                      AND ls.user_pk = ?
                    """,
                (rs, rowNum) -> mapTask(rs),
                id,
                userPk
            );
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Task> findBySessionIdWithStatus(Long sessionId) {
        return jdbcTemplate.query(
            """
                SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                       COALESCE((
                           SELECT ta.status
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ), 'PENDING') AS status,
                       (
                           SELECT ta.output_json
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                             AND ta.output_json IS NOT NULL
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ) AS output_json
                FROM task t
                WHERE t.session_id = ?
                ORDER BY t.created_at ASC, t.id ASC
                """,
            (rs, rowNum) -> mapTask(rs),
            sessionId
        );
    }

    @Override
    public Optional<Task> findFirstBySessionIdAndNodeIdAndStage(Long sessionId, Long nodeId, Stage stage) {
        List<Task> tasks = jdbcTemplate.query(
            """
                SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                       COALESCE((
                           SELECT ta.status
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ), 'PENDING') AS status,
                       (
                           SELECT ta.output_json
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                             AND ta.output_json IS NOT NULL
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ) AS output_json
                FROM task t
                WHERE t.session_id = ?
                  AND t.node_id = ?
                  AND t.stage = ?::task_stage
                ORDER BY t.created_at ASC, t.id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> mapTask(rs),
            sessionId,
            nodeId,
            stage.name()
        );
        return tasks.stream().findFirst();
    }

    @Override
    public List<TrainingAttemptSummary> findRecentTrainingAttempts(Long sessionId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.query(
            """
                SELECT t.id AS task_id,
                       t.node_id,
                       ta.score,
                       ARRAY(
                           SELECT jsonb_array_elements_text(COALESCE(ta.error_tags, '[]'::jsonb))
                       ) AS error_tags
                FROM task_attempt ta
                JOIN task t ON t.id = ta.task_id
                WHERE t.session_id = ?
                  AND t.stage = 'TRAINING'::task_stage
                  AND ta.score IS NOT NULL
                ORDER BY ta.created_at DESC
                LIMIT ?
                """,
            (rs, rowNum) -> new TrainingAttemptSummary(
                rs.getLong("task_id"),
                rs.getLong("node_id"),
                rs.getInt("score"),
                readErrorTags(rs)
            ),
            sessionId,
            boundedLimit
        );
    }

    @Override
    public Optional<Integer> findLatestScoreByTaskId(Long taskId) {
        Integer score = jdbcTemplate.query(
            """
                SELECT ta.score
                FROM task_attempt ta
                WHERE ta.task_id = ?
                  AND ta.score IS NOT NULL
                ORDER BY ta.created_at DESC, ta.id DESC
                LIMIT 1
                """,
            (rs, rowNum) -> rs.getInt("score"),
            taskId
        ).stream().findFirst().orElse(null);
        return Optional.ofNullable(score);
    }

    @Override
    public Long createRunningAttempt(Long taskId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO task_attempt (task_id, status, started_at)
                    VALUES (?, 'RUNNING'::run_status, now())
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, taskId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create task attempt.");
        }
        return key.longValue();
    }

    @Override
    public void markAttemptSucceeded(Long attemptId, String outputJson, AttemptLlmMetadata metadata) {
        jdbcTemplate.update(
            """
                UPDATE task_attempt
                SET status = 'SUCCEEDED'::run_status,
                    output_json = CAST(? AS jsonb),
                    llm_provider = ?,
                    llm_model = ?,
                    prompt_version = ?,
                    invocation_profile = ?,
                    token_input = ?,
                    token_output = ?,
                    reasoning_tokens = ?,
                    latency_ms = ?,
                    finish_reason = ?,
                    timeout_flag = ?,
                    truncated_flag = ?,
                    generation_mode = ?,
                    finished_at = now()
                WHERE id = ?
                """,
            outputJson,
            metadata.llmProvider(),
            metadata.llmModel(),
            metadata.promptVersion(),
            metadata.invocationProfile(),
            metadata.tokenInput(),
            metadata.tokenOutput(),
            metadata.reasoningTokens(),
            metadata.latencyMs(),
            metadata.finishReason(),
            metadata.timeout(),
            metadata.truncated(),
            metadata.generationMode(),
            attemptId
        );
    }

    @Override
    public void markAttemptFailed(Long attemptId, String reason, AttemptLlmMetadata metadata) {
        jdbcTemplate.update(
            """
                UPDATE task_attempt
                SET status = 'FAILED'::run_status,
                    feedback_json = CAST(? AS jsonb),
                    llm_provider = ?,
                    llm_model = ?,
                    prompt_version = ?,
                    invocation_profile = ?,
                    token_input = ?,
                    token_output = ?,
                    reasoning_tokens = ?,
                    latency_ms = ?,
                    finish_reason = ?,
                    timeout_flag = ?,
                    truncated_flag = ?,
                    generation_mode = ?,
                    finished_at = now()
                WHERE id = ?
                """,
            "{\"reason\":\"" + (reason == null ? "unknown" : reason.replace("\"", "'")) + "\"}",
            metadata.llmProvider(),
            metadata.llmModel(),
            metadata.promptVersion(),
            metadata.invocationProfile(),
            metadata.tokenInput(),
            metadata.tokenOutput(),
            metadata.reasoningTokens(),
            metadata.latencyMs(),
            metadata.finishReason(),
            metadata.timeout(),
            metadata.truncated(),
            metadata.generationMode(),
            attemptId
        );
    }

    @Override
    public void createSubmissionAttempt(
        Long taskId,
        String userAnswer,
        Integer score,
        String errorTagsJson,
        String feedbackJson,
        AttemptLlmMetadata metadata
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO task_attempt (
                    task_id, status, user_answer, score, error_tags, feedback_json,
                    llm_provider, llm_model, prompt_version, invocation_profile,
                    token_input, token_output, reasoning_tokens, latency_ms, finish_reason, timeout_flag, truncated_flag,
                    generation_mode, created_at
                )
                VALUES (?, 'SUCCEEDED'::run_status, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
            taskId,
            userAnswer,
            score,
            errorTagsJson,
            feedbackJson,
            metadata.llmProvider(),
            metadata.llmModel(),
            metadata.promptVersion(),
            metadata.invocationProfile(),
            metadata.tokenInput(),
            metadata.tokenOutput(),
            metadata.reasoningTokens(),
            metadata.latencyMs(),
            metadata.finishReason(),
            metadata.timeout(),
            metadata.truncated(),
            metadata.generationMode()
        );
    }

    private Task mapTask(java.sql.ResultSet rs) throws java.sql.SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setSessionId(rs.getLong("session_id"));
        task.setStage(Stage.valueOf(rs.getString("stage")));
        task.setNodeId(rs.getLong("node_id"));
        task.setObjective(rs.getString("objective"));
        task.setStatus(TaskStatus.fromDb(rs.getString("status")));
        task.setOutputJson(rs.getString("output_json"));
        task.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        task.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return task;
    }

    private List<String> readErrorTags(java.sql.ResultSet rs) throws SQLException {
        java.sql.Array sqlArray = rs.getArray("error_tags");
        if (sqlArray == null || sqlArray.getArray() == null) {
            return List.of();
        }
        Object raw = sqlArray.getArray();
        if (!(raw instanceof Object[] values) || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values)
            .filter(v -> v != null && !v.toString().isBlank())
            .map(Object::toString)
            .toList();
    }
}



