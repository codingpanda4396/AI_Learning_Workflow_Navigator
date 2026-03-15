package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.model.LearningStep;

import java.util.List;
import java.util.Optional;

public interface LearningStepRepository {

    LearningStep save(LearningStep step);

    List<LearningStep> saveAll(List<LearningStep> steps);

    List<LearningStep> findByTaskIdOrderByStepOrder(Long taskId);

    Optional<LearningStep> findByIdAndTaskId(Long stepId, Long taskId);

    void updateStatus(Long stepId, LearningStepStatus status);
}

