package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningTask;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    LearningTask save(LearningTask task);

    List<LearningTask> saveAll(List<LearningTask> tasks);

    Optional<LearningTask> findById(Long id);

    List<LearningTask> findBySessionId(Long sessionId);
}
