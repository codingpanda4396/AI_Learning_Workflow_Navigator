package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningPlan;

import java.util.Optional;

public interface LearningPlanRepository {

    LearningPlan save(LearningPlan plan);

    LearningPlan update(LearningPlan plan);

    Optional<LearningPlan> findByIdAndUserId(Long id, Long userId);
}
