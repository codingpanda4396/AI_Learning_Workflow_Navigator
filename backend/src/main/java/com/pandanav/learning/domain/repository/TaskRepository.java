package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    List<Task> saveAll(List<Task> tasks);

    Optional<Task> findById(Long id);

    List<Task> findBySessionIdWithStatus(Long sessionId);
}
