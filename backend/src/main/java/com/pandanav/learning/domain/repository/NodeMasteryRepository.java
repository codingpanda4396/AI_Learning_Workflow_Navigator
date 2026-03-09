package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.NodeMastery;

import java.util.List;
import java.util.Optional;

public interface NodeMasteryRepository {

    NodeMastery upsert(NodeMastery mastery);

    Optional<NodeMastery> findByUserIdAndNodeId(Long userId, Long nodeId);

    List<NodeMastery> findByUserIdAndChapterId(Long userId, String chapterId);
}
