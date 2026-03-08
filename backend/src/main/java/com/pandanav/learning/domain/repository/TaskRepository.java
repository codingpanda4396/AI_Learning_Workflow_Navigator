package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    List<Task> saveAll(List<Task> tasks);

    Optional<Task> findById(Long id);

    List<Task> findBySessionIdWithStatus(Long sessionId);

    Optional<Task> findFirstBySessionIdAndNodeIdAndStage(Long sessionId, Long nodeId, Stage stage);

    Long createRunningAttempt(Long taskId);

    void markAttemptSucceeded(Long attemptId, String outputJson);

    void createSubmissionAttempt(Long taskId, String userAnswer, Integer score, String errorTagsJson, String feedbackJson);
}


