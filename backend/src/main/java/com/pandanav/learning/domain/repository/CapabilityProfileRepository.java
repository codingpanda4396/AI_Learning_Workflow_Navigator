package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.CapabilityProfile;

import java.util.Optional;

public interface CapabilityProfileRepository {

    CapabilityProfile save(CapabilityProfile profile);

    Optional<CapabilityProfile> findLatestBySessionId(Long learningSessionId);
}
