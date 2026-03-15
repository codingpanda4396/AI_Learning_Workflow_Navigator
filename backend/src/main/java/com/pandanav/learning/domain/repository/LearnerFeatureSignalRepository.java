package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearnerFeatureSignal;

import java.util.List;

public interface LearnerFeatureSignalRepository {

    void saveAll(List<LearnerFeatureSignal> signals);
}
