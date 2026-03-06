package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Evidence;

import java.util.List;
import java.util.Optional;

public interface EvidenceRepository {

    Evidence save(Evidence evidence);

    Optional<Evidence> findById(Long id);

    List<Evidence> findByTaskId(Long taskId);
}
