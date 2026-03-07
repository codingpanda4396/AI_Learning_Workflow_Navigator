package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Mastery;

import java.util.List;
import java.util.Optional;

public interface MasteryRepository {

    List<Mastery> findByUserIdAndChapterId(String userId, String chapterId);

    Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId);

    Mastery upsert(Mastery mastery);
}


