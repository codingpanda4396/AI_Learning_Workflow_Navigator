package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Stage;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TaskStatus;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
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
    public void markAttemptSucceeded(Long attemptId, String outputJson) {
        jdbcTemplate.update(
            """
                UPDATE task_attempt
                SET status = 'SUCCEEDED'::run_status,
                    output_json = CAST(? AS jsonb),
                    finished_at = now()
                WHERE id = ?
                """,
            outputJson,
            attemptId
        );
    }

    private Task mapTask(java.sql.ResultSet rs) throws java.sql.SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setSessionId(rs.getLong("session_id"));
        task.setStage(Stage.valueOf(rs.getString("stage")));
        task.setNodeId(rs.getLong("node_id"));
        task.setObjective(rs.getString("objective"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setOutputJson(rs.getString("output_json"));
        task.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        task.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return task;
    }
}
