package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearnerProfileSnapshot;

import java.util.Optional;

public interface LearnerProfileSnapshotRepository {

    LearnerProfileSnapshot saveOrUpdate(LearnerProfileSnapshot snapshot);

    Optional<LearnerProfileSnapshot> findByDiagnosisSessionId(Long diagnosisSessionId);
}
