package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningSession;

import java.util.Optional;

public interface SessionRepository {

    LearningSession save(LearningSession session);

    Optional<LearningSession> findById(Long id);
}
