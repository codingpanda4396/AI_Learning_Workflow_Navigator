package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Mastery;

import java.util.List;
import java.util.Optional;

public interface MasteryRepository {

    Mastery save(Mastery mastery);

    Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId);

    List<Mastery> findByUserId(String userId);
}
