package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.TutorMessage;

import java.util.List;

public interface TutorMessageRepository {

    List<TutorMessage> findBySessionIdAndTaskIdAndUserId(Long sessionId, Long taskId, Long userId);

    List<TutorMessage> findRecentBySessionIdAndTaskIdAndUserId(Long sessionId, Long taskId, Long userId, int limit);

    TutorMessage save(TutorMessage message);
}
