package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.SessionStatus;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    LearningSession save(LearningSession session);

    Optional<LearningSession> findById(Long id);

    Optional<LearningSession> findLatestByUserId(String userId);

    Optional<LearningSession> findByIdAndUserPk(Long id, Long userPk);

    Optional<LearningSession> findLatestActiveByUserPk(Long userPk);

    List<LearningSession> findHistoryByUserPk(Long userPk, SessionStatus status, int limit, int offset);

    long countHistoryByUserPk(Long userPk, SessionStatus status);

    void updateCurrentPosition(Long sessionId, Long currentNodeId, Stage currentStage);

    void updateStatus(Long sessionId, SessionStatus status);

    void updateCurrentPlanInstance(Long sessionId, Long planInstanceId);

    void touchLastActive(Long sessionId);
}


